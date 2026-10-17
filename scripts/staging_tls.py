"""Disposable PostgreSQL TLS and trusted-edge drills; never uses provider/prod credentials."""
import json,os,time
from pathlib import Path
from staging import Stage,OUT,ROOT,PROJECT
s=Stage();checks=[];owned=[]
def check(name,condition,details=None):
 checks.append({'test':name,'passed':bool(condition),'details':details});(OUT/'tls-proxy.json').write_text(json.dumps(checks,indent=2));assert condition,(name,details)
def launch(args):
 cid=s.run(['docker','run','-d',*args]).stdout.decode().strip();owned.append(cid);return cid
cert=s.path/'staging-ca.crt';key=s.path/'staging-server.key'
s.run(['openssl','req','-x509','-newkey','rsa:2048','-sha256','-days','1','-nodes','-subj','/CN=healthmate-phase3-edge','-addext','subjectAltName=DNS:healthmate-phase3-edge,DNS:postgres','-keyout',str(key),'-out',str(cert)])
key.chmod(0o600);cert.chmod(0o644) # Public CA; private parent remains mode 700.
pg=s.container('postgres')
for path,target,mode in [(cert,'phase3-server.crt','644'),(key,'phase3-server.key','600')]:
 command=f'cat > /var/lib/postgresql/data/{target} && chown postgres:postgres /var/lib/postgresql/data/{target} && chmod {mode} /var/lib/postgresql/data/{target}'
 s.run(['docker','exec','-i',pg,'sh','-c',command],path.read_bytes())
s.sql("ALTER SYSTEM SET ssl='on';\nALTER SYSTEM SET ssl_cert_file='/var/lib/postgresql/data/phase3-server.crt';\nALTER SYSTEM SET ssl_key_file='/var/lib/postgresql/data/phase3-server.key';\nSELECT pg_reload_conf();")
edgeconf=s.path/'edge-server.conf'
edgeconf.write_text('''resolver 127.0.0.11 valid=2s ipv6=off;
upstream staging_frontend { zone staging_frontend 64k; server frontend:8080 resolve; }
server { listen 8443 ssl; ssl_certificate /run/test.crt; ssl_certificate_key /run/test.key;
 location / { proxy_pass http://staging_frontend; proxy_set_header Host $host; proxy_set_header X-Forwarded-For $remote_addr; proxy_set_header X-Forwarded-Proto $scheme; }
}
''');edgeconf.chmod(0o644)
try:
 edge=launch(['--name','healthmate-phase3-edge','--network',PROJECT+'_app','--user',str(os.getuid())+':'+str(os.getgid()),'--read-only','--tmpfs','/tmp:rw,nosuid,size=32m','--cap-drop','ALL','--mount','type=bind,source='+str(edgeconf)+',target=/etc/nginx/conf.d/default.conf,readonly','--mount','type=bind,source='+str(cert)+',target=/run/test.crt,readonly','--mount','type=bind,source='+str(key)+',target=/run/test.key,readonly','--entrypoint','nginx','nginxinc/nginx-unprivileged:stable-alpine@sha256:782bb89768cc87b7f95c18b95baa1af250e5d451f26794f59363540161147c01','-g','daemon off;'])
 info=json.loads(s.run(['docker','inspect',edge]).stdout)[0];edgeip=info['NetworkSettings']['Networks'][PROJECT+'_app']['IPAddress'];assert edgeip
 trust=s.path/'edge-trust.conf';trust.write_text((ROOT/'deploy/nginx-edge.conf.example').read_text().replace('192.0.2.10/32',edgeip+'/32')+"\nlog_format phase3_trust '$remote_addr $realip_remote_addr $healthmate_forwarded_proto';\naccess_log /tmp/phase3-proxy.log phase3_trust;\n");trust.chmod(0o644)
 with (s.path/'compose.env').open('a')as f:f.write('DB_CA_FILE='+str(cert)+'\nEDGE_TRUST_CONFIG_FILE='+str(trust)+'\n')
 override=s.path/'database-tls.yaml';override.write_text('services:\n  backend:\n    environment:\n      DB_URL: jdbc:postgresql://postgres:5432/healthmate_staging?sslmode=verify-full&sslrootcert=/run/secrets/db-ca.crt\n')
 cmd=['docker','compose','-p',PROJECT,'--env-file',str(s.path/'compose.env')]
 for file in [ROOT/'compose.yaml',ROOT/'compose.staging.yaml',ROOT/'compose.database-tls.yaml',ROOT/'compose.edge.yaml',override]:cmd+=['-f',str(file)]
 s.run(cmd+['config','--quiet']);s.run(cmd+['up','-d','--wait','--wait-timeout','180','backend','frontend'])
 ssl=s.sql("SELECT count(*) FROM pg_stat_ssl s JOIN pg_stat_activity a USING(pid) WHERE a.usename='healthmate_app' AND a.datname='healthmate_staging' AND s.ssl;").stdout.strip()
 check('real Spring PostgreSQL verify-full connection with mounted CA',int(ssl)>0,{'tlsConnections':int(ssl)})
 readable=s.run(['docker','exec',s.container('backend'),'test','-r','/run/secrets/db-ca.crt'],check=False);check('database CA readable by nonroot backend',readable.returncode==0)
 clients=[]
 for i in range(3):clients.append(launch(['--name','healthmate-phase3-client-'+str(i),'--network',PROJECT+'_app','--read-only','--cap-drop','ALL','--mount','type=bind,source='+str(cert)+',target=/run/test-ca.crt,readonly','--entrypoint','sh','healthmate-backend:phase3','-c','sleep 600']))
 def curl(client,path,body=None,spoof=None,direct=False):
  args=['docker','exec','-i',client,'curl','--silent','--show-error','--max-time','15','--cacert','/run/test-ca.crt','-o','/dev/null','-w','%{http_code}','-H','Content-Type: application/json']
  if spoof:args+=['-H','X-Forwarded-For: '+spoof,'-H','X-Forwarded-Proto: https']
  if body is not None:args+=['--data-binary','@-']
  args+=[('http://frontend:8080'if direct else'https://healthmate-phase3-edge:8443')+path]
  result=s.run(args,None if body is None else json.dumps(body).encode(),check=False)
  return int(result.stdout or 0)
 for _ in range(20):
  if curl(clients[0],'/readyz')==200:break
  time.sleep(1)
 check('HTTPS edge verifies disposable certificate and reaches real readiness',curl(clients[0],'/readyz')==200)
 statuses=[curl(clients[0],'/api/auth/login',{'email':f'edge-limit-{i}@example.test','password':'incorrect'})for i in range(31)]
 check('trusted edge preserves first client IP quota',statuses==[400]*30+[429],{'statuses':statuses})
 status=curl(clients[1],'/api/auth/login',{'email':'phase3-doctor@example.test','password':'Integration-pass-123!'})
 check('second actual client retains login access after first is throttled',status==200,{'status':status})
 statuses=[curl(clients[2],'/api/auth/login',{'email':f'direct-limit-{i}@example.test','password':'incorrect'},spoof='203.0.113.'+str(i+1),direct=True)for i in range(31)]
 check('untrusted direct client cannot evade IP quota with forged forwarding headers',statuses==[400]*30+[429],{'statuses':statuses})
 # The default access log uses the remapped remote address. Neither passwords nor tokens are logged.
 logs=s.run(['docker','logs',s.container('frontend')]);text=(logs.stdout+logs.stderr).decode()
 addresses=[json.loads(s.run(['docker','inspect',c]).stdout)[0]['NetworkSettings']['Networks'][PROJECT+'_app']['IPAddress']for c in clients]
 check('frontend log attributes separate clients rather than only edge address',all(ip+' - 'in text for ip in addresses[:2]))
 proof=s.run(['docker','exec',s.container('frontend'),'cat','/tmp/phase3-proxy.log']).stdout.decode()
 check('trusted HTTPS protocol preserved; direct forged HTTPS ignored',addresses[0]+' '+edgeip+' https' in proof and addresses[2]+' '+addresses[2]+' http' in proof)
 print('TLS/proxy checks:',len(checks),'passed')
finally:
 for cid in reversed(owned):s.run(['docker','rm','-f',cid],check=False)
 # Restore base rehearsal topology. Optional overlays remain explicit production choices.
 s.compose('up','-d','--wait','--wait-timeout','180','backend','frontend')
