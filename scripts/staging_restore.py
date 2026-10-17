"""Final current-state recovery drill plus real API reads on the restored database."""
import hashlib,json,time,sys,re
from staging import Stage,OUT
label=sys.argv[1]if len(sys.argv)>1 else ''
if label and not re.fullmatch('[a-z0-9_]{1,32}',label):raise SystemExit('Use a short lowercase recovery label.')
s=Stage();db='healthmate_final_recovery'+('_'+label if label else '');name='healthmate-phase3-restored-app';checks=[]
def check(name,condition):
 assert condition,name;checks.append({'test':name,'passed':True})
rows=s.rows();schema=s.snapshot()
seq_query="SELECT jsonb_agg(to_jsonb(t) ORDER BY sequencename) FROM pg_sequences t WHERE schemaname='public';"
seq=s.sql(seq_query).stdout
backup=s.run(['docker','exec',s.container('postgres'),'pg_dump','-U','healthmate_owner','-d','healthmate_staging','-Fc','--no-owner']).stdout
p=s.path/'final.dump';p.write_bytes(backup);p.chmod(0o600)
s.createdb(db);s.run(['docker','exec','-i',s.container('postgres'),'pg_restore','-U','healthmate_owner','-d',db,'--no-owner','--exit-on-error','--single-transaction'],backup)
check('final backup/restore preserves all nine tables, schema and sequences',rows==s.rows(db) and schema==s.snapshot(db) and seq==s.sql(seq_query,db).stdout)
s.sql(f'REVOKE ALL ON DATABASE {db} FROM PUBLIC; GRANT CONNECT ON DATABASE {db} TO healthmate_app;',db)
s.run(['docker','run','-d','--name',name,'--network','healthmate-phase3-staging_app','--env-file',str(s.path/'backend.env'),'-e','SPRING_PROFILES_ACTIVE=prod','-e','DB_URL=jdbc:postgresql://postgres:5432/'+db,'--read-only','--tmpfs','/tmp:rw,noexec,nosuid,size=128m','healthmate-backend:phase3'])
try:
 for _ in range(90):
  i=json.loads(s.run(['docker','inspect',name]).stdout)[0]
  if not i['State']['Running']or i['State'].get('Health',{}).get('Status')=='healthy':break
  time.sleep(1)
 check('restored database validates and starts under non-owner runtime account',i['State']['Health']['Status']=='healthy'and schema==s.snapshot(db)and rows==s.rows(db))
 for key,path,expected in [('admin','/api/users',list),('doctor','/api/appointments/doctor/6',list),('patient','/api/appointments/patient/8',list)]:
  credentials=json.dumps({'email':'phase3-'+key+'@example.test','password':'Integration-pass-123!'})
  auth=s.run(['docker','exec','-i',name,'curl','--fail','--silent','-H','Content-Type: application/json','--data-binary','@-','http://127.0.0.1:8080/api/auth/login'],credentials.encode())
  session=json.loads(auth.stdout);check('restored '+key+' login',bool(session.get('token')))
  config='url = "http://127.0.0.1:8080'+path+'"\nheader = "Authorization: Bearer '+session['token']+'"\n'
  response=s.run(['docker','exec','-i',name,'curl','--fail','--silent','--config','-'],config.encode());body=json.loads(response.stdout)
  check('restored '+key+' authorized data retrieval',isinstance(body,expected)and len(body)>0)
finally:s.run(['docker','rm','-f',name])
(OUT/'final-restore.json').write_text(json.dumps({'backupSha256':hashlib.sha256(backup).hexdigest(),'checks':checks,'prodDatabaseAccessed':False},indent=2));print('Final recovery checks:',len(checks),'passed')
