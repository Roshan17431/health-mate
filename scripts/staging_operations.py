"""Backup/recovery and outage drills confined to the owned synthetic staging stack."""
import hashlib,json,time
from urllib.request import urlopen
from urllib.error import HTTPError,URLError
from staging import Stage,OUT,MANUAL
s=Stage();checks=[]
def check(name,condition,details=None):
 checks.append(dict(name=name,passed=bool(condition),details=details));(OUT/'operations.json').write_text(json.dumps(checks,indent=2))
 assert condition,(name,details)
def status(path):
 try:
  with urlopen('http://localhost:3300'+path,timeout=8)as r:return r.status
 except HTTPError as e:return e.code
 except URLError:return 0
def wait_ready():
 for _ in range(60):
  if status('/readyz')==200:return
  time.sleep(1)
 raise AssertionError('Staging readiness did not recover')
def sequences(db):
 return s.sql("SELECT jsonb_agg(to_jsonb(t) ORDER BY sequencename) FROM pg_sequences t WHERE schemaname='public';",db).stdout
rows=s.rows();schema=s.snapshot();seq=sequences('healthmate_staging')
backup=s.run(['docker','exec',s.container('postgres'),'pg_dump','-U','healthmate_owner','-d','healthmate_staging','-Fc','--no-owner']).stdout
p=s.path/'latest.dump';p.write_bytes(backup);p.chmod(0o600)
s.createdb('healthmate_recovery')
s.run(['docker','exec','-i',s.container('postgres'),'pg_restore','-U','healthmate_owner','-d','healthmate_recovery','--no-owner','--exit-on-error','--single-transaction'],backup)
check('latest migrated full-stack database custom backup/restore',s.rows('healthmate_recovery')==rows and s.snapshot('healthmate_recovery')==schema and sequences('healthmate_recovery')==seq,{'sha256':hashlib.sha256(backup).hexdigest(),'tables':9,'sequencesPreserved':True})
check('canceled and completed browser appointment history restored',s.sql("SELECT string_agg(status,',' ORDER BY id) FROM appointments WHERE patient_id=10;",'healthmate_recovery').stdout.strip()==b'CANCELED,CANCELED,COMPLETED')
# Repeat upgrades without deleting history, including already shared canceled/rebooked slots.
s.sql((MANUAL/'booking_integrity.sql').read_text());s.sql((MANUAL/'audit_actor_snapshot.sql').read_text())
check('both migrations idempotent after real browser writes',s.rows()==rows and s.snapshot()==schema)
postgres=s.container('postgres');s.run(['docker','stop','-t','5',postgres])
try:
 ready=status('/readyz');check('readiness fails when database stops',ready in [503,504],{'status':ready})
 check('static health remains available during database outage',status('/healthz')==200)
 live=s.run(['docker','exec',s.container('backend'),'curl','--fail','--silent','--max-time','5','http://127.0.0.1:8080/actuator/health/liveness'],check=False)
 check('application liveness excludes database outage',live.returncode==0 and json.loads(live.stdout)['status']=='UP')
finally:s.run(['docker','start',postgres])
wait_ready();check('database readiness recovers',status('/readyz')==200)
check('database outage loses no records or schema',s.rows()==rows and s.snapshot()==schema)
backend=s.container('backend');s.compose('up','-d','--force-recreate','--no-deps','--wait','--wait-timeout','180','backend');wait_ready()
check('backend recreation recovered through dynamic proxy DNS',s.container('backend')!=backend and status('/readyz')==200)
check('backend recreation preserves all clinical data and schema',s.rows()==rows and s.snapshot()==schema)
log=s.run(['docker','logs',backend],check=False) # replaced container may have been removed; no false graceful-shutdown claim
# Verify graceful SIGTERM on the current backend without deleting the container first.
s.compose('stop','-t','35','backend');logs=s.run(['docker','logs',s.container('backend')]);text=(logs.stdout+logs.stderr).decode(errors='replace')
check('SIGTERM performs graceful web-server shutdown','Graceful shutdown complete' in text)
s.compose('up','-d','--wait','--wait-timeout','180','backend');wait_ready()
check('graceful restart recovers and preserves records',s.rows()==rows and s.snapshot()==schema)
# Per-process limits: real login requests, no credentials/tokens written to evidence.
from urllib.request import Request
statuses=[];retry=None
for _ in range(11):
 request=Request('http://localhost:3300/api/auth/login',data=json.dumps({'email':'staging-limit@example.test','password':'wrong-password'}).encode(),headers={'Content-Type':'application/json','X-Forwarded-For':'203.0.113.99'})
 try:
  with urlopen(request)as response:statuses.append(response.status)
 except HTTPError as e:statuses.append(e.code);retry=e.headers.get('Retry-After')
check('real production account login quota returns 429 and Retry-After',statuses==[400]*10+[429] and int(retry)>0,{'statuses':statuses,'retryAfterPresent':retry is not None})
print(json.dumps({'passed':sum(c['passed']for c in checks),'failed':sum(not c['passed']for c in checks)}))
