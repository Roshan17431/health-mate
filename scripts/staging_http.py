"""Real HTTP checks of the production proxy and image; isolated rehearsal only."""
import json,zipfile
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from staging import Stage,OUT
s=Stage();cases=[]
def req(path,headers=None,method='GET'):
 try:
  with urlopen(Request('http://localhost:3300'+path,headers=headers or {},method=method)) as r:return r.status,dict(r.headers),r.read()
 except HTTPError as e:return e.code,dict(e.headers),e.read()
for origin,expected in [('http://localhost:3300',200),('https://untrusted.example',403)]:
 status,h,b=req('/api/auth/login',{'Origin':origin,'Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'content-type'},'OPTIONS');assert status==expected,(origin,status)
 cases.append({'test':'CORS '+origin,'status':status,'passed':True})
for path,expected in [('/healthz',200),('/readyz',200),('/favicon.svg',200),('/actuator/env',404),('/actuator/health',404),('/assets/missing-asset.js',404)]:
 status,h,b=req(path);assert status==expected,(path,status)
 assert h.get('X-Content-Type-Options')=='nosniff' and h.get('X-Frame-Options')=='DENY' and 'Content-Security-Policy'in h
 if path=='/favicon.svg': assert '<svg' in b.decode() and 'svg' in h.get('Content-Type','')
 cases.append({'test':path,'status':status,'securityHeaders':True,'passed':True})
status,h,b=req('/api/users');assert status==401 and 'no-store' in h.get('Cache-Control','');cases.append({'test':'unauthorized API no-store','status':status,'passed':True})
c=s.run(['docker','create','healthmate-backend:phase3']).stdout.decode().strip()
try:
 jar=s.path/'image.jar';s.run(['docker','cp',c+':/app/app.jar',str(jar)])
 with zipfile.ZipFile(jar)as z:assert 'BOOT-INF/classes/data.sql' not in z.namelist()
 cases.append({'test':'production image excludes seed SQL','passed':True})
finally:s.run(['docker','rm',c])
s.compose('config','--quiet');cases.append({'test':'Compose configuration valid','passed':True})
(OUT/'http-configuration.json').write_text(json.dumps(cases,indent=2));print('Configuration HTTP/image checks:',len(cases),'passed')
