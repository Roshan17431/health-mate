#!/usr/bin/env python3
"""Local-only Phase 3 rehearsal; never reads or connects to the normal product DB."""
from pathlib import Path
import base64, hashlib, json, os, secrets, shutil, subprocess, sys, tempfile, time
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/playwright/phase3'
MARKER = Path(tempfile.gettempdir()) / 'healthmate-phase3-current'
PROJECT = 'healthmate-phase3-staging'
MANUAL = ROOT / 'backend/src/main/resources/db/manual'

class Stage:
    def __init__(self):
        if not MARKER.exists(): raise SystemExit('Run init first.')
        self.path = Path(MARKER.read_text().strip())
        self.values = json.loads((self.path/'private.json').read_text())
    def run(self, args, data=None, check=True):
        result = subprocess.run(args,input=data,capture_output=True)
        if check and result.returncode:
            message = result.stderr.decode(errors='replace')[-2500:]
            for value in self.values.values(): message = message.replace(str(value), '[redacted]')
            raise RuntimeError(message or 'Staging command failed.')
        return result
    def compose(self, *args, check=True):
        return self.run(['docker','compose','-p',PROJECT,'--env-file',str(self.path/'compose.env'),
            '-f',str(ROOT/'compose.yaml'),'-f',str(ROOT/'compose.staging.yaml'),*args],check=check)
    def container(self, name):
        return self.compose('ps','-a','-q',name).stdout.decode().strip()
    def sql(self, query, db='healthmate_staging', check=True):
        return self.run(['docker','exec','-i',self.container('postgres'),'psql','-X','-U','healthmate_owner','-d',db,'-v','ON_ERROR_STOP=1','-At'],query.encode(),check)
    def createdb(self,name):
        self.run(['docker','exec',self.container('postgres'),'createdb','-U','healthmate_owner',name])
    def snapshot(self,db='healthmate_staging'):
        query="""SELECT jsonb_build_object('tables',(SELECT jsonb_agg(row(t.table_name,t.column_name,t.data_type,t.is_nullable,t.column_default)::text ORDER BY t.table_name,t.ordinal_position) FROM information_schema.columns t WHERE t.table_schema='public'), 'indexes',(SELECT jsonb_agg(indexdef ORDER BY indexname) FROM pg_indexes WHERE schemaname='public'));"""
        return hashlib.sha256(self.sql(query,db).stdout).hexdigest()
    def rows(self,db='healthmate_staging'):
        names=['users','doctors','patients','doctor_schedules','time_slots','appointments','notifications','otp_tokens','audit_logs']
        return {name:hashlib.sha256(self.sql(f'SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text),\'[]\'::jsonb)::text FROM {name} t;',db).stdout).hexdigest() for name in names}
    def prepare(self):
        self.compose('up','-d','postgres','mailpit')
        for _ in range(90):
            ready=self.run(['docker','exec',self.container('postgres'),'pg_isready','-h','127.0.0.1','-U','healthmate_owner'],check=False)
            if ready.returncode==0: break
            time.sleep(1)
        else: raise RuntimeError('Isolated PostgreSQL unavailable.')
        self.createdb('healthmate_legacy')
        self.sql((ROOT/'scripts/tests/legacy_schema.sql').read_text(),'healthmate_legacy')
        # Synthetic legacy administrator is deliberately not a usable privileged identity.
        rows=(ROOT/'scripts/tests/legacy_rows.sql').read_text().replace("'ROLE_ADMIN'", "'ROLE_PATIENT'")
        self.sql(rows,'healthmate_legacy')
        before=self.rows('healthmate_legacy')
        backup=self.run(['docker','exec',self.container('postgres'),'pg_dump','-U','healthmate_owner','-d','healthmate_legacy','-Fc','--no-owner']).stdout
        dump=self.path/'legacy.dump'; dump.write_bytes(backup); dump.chmod(0o600)
        self.run(['docker','exec','-i',self.container('postgres'),'pg_restore','-U','healthmate_owner','-d','healthmate_staging','--no-owner','--exit-on-error'],backup)
        assert before==self.rows(), 'Restored legacy rows differ'
        self.sql((MANUAL/'booking_integrity.sql').read_text())
        self.sql((MANUAL/'audit_actor_snapshot.sql').read_text())
        after=self.rows()
        assert all(before[t]==after[t] for t in before if t!='audit_logs'), 'Clinical rows changed during migration'
        self.createdb('healthmate_fresh')
        self.sql((MANUAL/'initial_schema.sql').read_text(),'healthmate_fresh')
        password=self.values['runtime']
        self.sql(f"CREATE ROLE healthmate_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION PASSWORD '{password}';")
        for db in ['healthmate_staging','healthmate_fresh']:
            self.sql(f"REVOKE ALL ON DATABASE {db} FROM PUBLIC; GRANT CONNECT ON DATABASE {db} TO healthmate_app; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO healthmate_app; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO healthmate_app; GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO healthmate_app;",db)
        self.verify_prepared()
    def verify_prepared(self):
        before=self.rows('healthmate_legacy')
        after=self.rows()
        assert all(before[t] == after[t] for t in before if t != 'audit_logs'), 'Prepared clinical records differ from the restored legacy baseline'
        audit_query="SELECT coalesce(jsonb_agg(to_jsonb(t)-'actor_user_id' ORDER BY id),'[]'::jsonb)::text FROM audit_logs t;"
        assert self.sql(audit_query, 'healthmate_legacy').stdout == self.sql(audit_query).stdout, 'Migration changed legacy audit fields'
        backup=(self.path/'legacy.dump').read_bytes()
        denied={}
        for statement in ['CREATE TABLE public.forbidden_probe(id int);','ALTER TABLE public.users ADD COLUMN forbidden_probe boolean;']:
            result=self.sql('SET ROLE healthmate_app; '+statement,check=False)
            assert result.returncode != 0 and (b'permission denied' in result.stderr or b'must be owner' in result.stderr)
            denied[statement.split()[0]]=True
        (OUT/'staging-prepare.json').write_text(json.dumps({'legacyBackupSha256':hashlib.sha256(backup).hexdigest(),'restoreAllNineTablesExact':True,'clinicalRowsPreservedAcrossMigrations':True,'rowDigestsBefore':before,'rowDigestsAfter':after,'runtimeDdlDenied':denied,'schemaBeforeStartup':self.snapshot(),'rowsBeforeStartup':self.rows()},indent=2))
        print('Isolated backup restored; migrations, fresh schema and runtime DDL denial verified.')
    def startup_check(self):
        before=json.loads((OUT/'staging-prepare.json').read_text())
        assert before['schemaBeforeStartup']==self.snapshot(), 'Startup changed database schema'
        assert before['rowsBeforeStartup']==self.rows(), 'Startup seeded or changed data'
        state={}
        for name in ['backend','frontend']:
            inspect=json.loads(self.run(['docker','inspect',self.container(name)]).stdout)[0]
            assert inspect['State']['Health']['Status']=='healthy'
            assert inspect['Config']['User'] not in ['','root','0','0:0']
            assert inspect['HostConfig']['ReadonlyRootfs'] is True
            state[name]={'health':inspect['State']['Health']['Status'],'user':inspect['Config']['User'],'readOnlyRootfs':True,'imageId':inspect['Image']}
        (OUT/'startup.json').write_text(json.dumps({'schemaUnchanged':True,'dataUnchanged':True,'containers':state},indent=2))
        print('Production-profile startup changed no schema or data; both nonroot containers healthy.')
    def sql_file(self,path,email=None):
        args=['docker','exec','-i',self.container('postgres'),'psql','-X','-U','healthmate_owner','-d','healthmate_staging','-v','ON_ERROR_STOP=1']
        if email:args+=['-v','admin_email='+email]
        return self.run(args,Path(path).read_bytes())
    def down(self):
        self.compose('down','--volumes','--remove-orphans')
        shutil.rmtree(self.path); MARKER.unlink()
        print('Only isolated Phase 3 services, volumes and private rehearsal inputs removed.')

def init():
    if MARKER.exists(): raise SystemExit('A staging context already exists; inspect/reuse or run down, never overwrite it.')
    existing=subprocess.run(['docker','ps','-aq','--filter','label=com.docker.compose.project='+PROJECT],capture_output=True,check=True)
    if existing.stdout.strip(): raise SystemExit('Unowned staging containers exist; refusing to reuse them.')
    volumes=subprocess.run(['docker','volume','ls','-q','--filter','label=com.docker.compose.project='+PROJECT],capture_output=True,check=True)
    named_volume=subprocess.run(['docker','volume','inspect',PROJECT+'_staging-db'],capture_output=True)
    if volumes.stdout.strip() or named_volume.returncode == 0:
        raise SystemExit('Unowned staging volumes exist; refusing to reuse or delete them.')
    path=Path(tempfile.mkdtemp(prefix='healthmate-phase3-'));path.chmod(0o700)
    values={'owner':secrets.token_urlsafe(32),'runtime':secrets.token_urlsafe(32),'jwt':base64.b64encode(secrets.token_bytes(48)).decode()}
    def private(name,text):
        p=path/name;p.write_text(text);p.chmod(0o600)
    private('private.json',json.dumps(values))
    private('backend.env',f"DB_URL=jdbc:postgresql://postgres:5432/healthmate_staging\nDB_USERNAME=healthmate_app\nDB_PASSWORD={values['runtime']}\nJWT_SECRET={values['jwt']}\nJWT_EXPIRATION_MS=3600000\nCORS_ALLOWED_ORIGINS=http://localhost:3300\n")
    private('compose.env',f"BACKEND_ENV_FILE={path/'backend.env'}\nSTAGING_OWNER_PASSWORD={values['owner']}\nHTTP_PORT=3300\nSTAGING_DB_PORT=55433\nSTAGING_MAIL_PORT=18026\n")
    MARKER.write_text(str(path));MARKER.chmod(0o600)
    OUT.mkdir(parents=True,exist_ok=True)
    print('Private local staging context created. Frontend will bind only localhost:3300.')

if __name__=='__main__':
    command=sys.argv[1] if len(sys.argv)>1 else 'help'
    if command=='init': init()
    elif command=='prepare': Stage().prepare()
    elif command=='verify-prepared': Stage().verify_prepared()
    elif command=='build':
        result=Stage().compose('build');print(result.stdout.decode());print(result.stderr.decode())
    elif command=='up': Stage().compose('up','-d','--wait','--wait-timeout','240');print('Local staging started.')
    elif command=='startup-check': Stage().startup_check()
    elif command=='down': Stage().down()
    elif command=='ps': print(Stage().compose('ps').stdout.decode())
    elif command=='sql': print(Stage().sql_file(sys.argv[2],sys.argv[3] if len(sys.argv)>3 else None).stdout.decode())
    else: print('Usage: staging.py init|prepare|build|up|startup-check|ps|sql FILE [ADMIN_EMAIL]|down')
