import sys,time,json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from staging import Stage,OUT
s=Stage(); result={}
# This executes no application writes: snapshot BEFORE any API registration.
# Initial no-write startup evidence is captured separately.
image='healthmate-backend:phase3'
for db,name,expected in [('healthmate_fresh','healthmate-phase3-fresh-check',True),('healthmate_empty','healthmate-phase3-empty-check',False)]:
    if not expected:
        if s.sql("SELECT count(*) FROM pg_database WHERE datname='"+db+"';",'postgres').stdout.strip()==b'0': s.createdb(db)
        assert s.sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'",db).stdout.strip()==b'0'
        s.sql(f'REVOKE ALL ON DATABASE {db} FROM PUBLIC; GRANT CONNECT ON DATABASE {db} TO healthmate_app; REVOKE ALL ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO healthmate_app;',db)
    before=s.snapshot(db)
    args=['docker','run','-d','--name',name,'--network','healthmate-phase3-staging_app','--env-file',str(s.path/'backend.env'),'-e','DB_URL=jdbc:postgresql://postgres:5432/'+db,'-e','PORT=10001','--read-only','--tmpfs','/tmp:rw,noexec,nosuid,size=128m',image]
    s.run(args)
    try:
        for _ in range(120):
            info=json.loads(s.run(['docker','inspect',name]).stdout)[0]
            if not info['State']['Running'] or info['State'].get('Health',{}).get('Status')=='healthy':break
            time.sleep(1)
        log_result=s.run(['docker','logs',name]); logs=(log_result.stdout+log_result.stderr).decode(errors='replace')
        for value in s.values.values():logs=logs.replace(value,'[redacted]')
        (OUT/(db+'-startup.log')).write_text(logs)
        assert s.snapshot(db)==before, db+' schema changed'
        if expected:
            assert info['State']['Health']['Status']=='healthy', 'fresh initial schema failed JPA validation'
            assert 'Started HealthMateApplication' in logs
            # Probe the PORT override via the image's own HEALTHCHECK, not an external guessed port.
            assert all(s.sql(f'SELECT count(*) FROM {table}',db).stdout.strip()==b'0' for table in s.rows(db))
        else:
            assert not info['State']['Running'] and 'missing table' in logs.lower()
        result[db]={'expectedStartup':expected,'schemaUnchanged':True,'validated':True,'PORTOverrideHealthCheck':expected}
    finally:s.run(['docker','rm','-f',name])
(OUT/'fresh-schema.json').write_text(json.dumps(result,indent=2)); print(json.dumps(result))
