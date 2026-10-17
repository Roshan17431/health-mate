"""Real HTTP + disposable PostgreSQL assertions; no network response mocking."""
import base64, concurrent.futures, datetime as dt, hashlib, hmac, json, subprocess, time
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from pathlib import Path
BASE='http://localhost:3300'; PASSWORD='Integration-pass-123!'
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from staging import Stage
stage=Stage()
OUT=Path(__file__).resolve().parents[1]/'output/playwright/phase3'; fixtures=json.loads((OUT/'fixtures.json').read_text()); results=[]
def check(name,ok,details=None):
    results.append(dict(name=name,passed=bool(ok),details=details))
    (OUT/'api-results.json').write_text(json.dumps(results,indent=2))
    if not ok:raise AssertionError(name+': '+str(details))
def api(method,path,body=None,token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    request=Request(BASE+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
    try:
        with urlopen(request) as r:return r.status,json.loads(r.read() or b'null')
    except HTTPError as e:return e.code,json.loads(e.read() or b'null')
def sql(query):
    return stage.sql(query).stdout.decode().strip()
def login(key,password=PASSWORD):
    status,body=api('POST','/api/auth/login',dict(email=fixtures[key]['email'],password=password))
    check(key+' real login',status==200,{'status':status});return body['token']
tokens={k:login(k) for k in fixtures}
a=tokens['admin'];p=tokens['patient'];p2=tokens['patient2'];d=tokens['doctor'];d2=tokens['doctor2'];pid=fixtures['patient']['id'];pid2=fixtures['patient2']['id'];did=fixtures['doctor']['id'];did2=fixtures['doctor2']['id']
check('PostgreSQL registration and BCrypt storage',sql("SELECT role||'|'||(password LIKE '$2%')::text FROM users WHERE email='phase3-patient@example.test'")=='ROLE_PATIENT|true')
for role in ['ROLE_ADMIN','ROLE_DOCTOR']:
    status,_=api('POST','/api/auth/register',dict(name='Denied',email='denied-'+role+'@example.test',password=PASSWORD,phoneNumber='denied-'+role,role=role,gender='OTHER',dateOfBirth='1990-01-01',address='Test'))
    check('public '+role+' rejected',status==403,status)
check('rejected staff signup produced no row',sql("SELECT count(*) FROM users WHERE email LIKE 'denied-%'")=='0')
for key,token in [('patient',p),('doctor',d)]:
    for path in ['/api/users','/api/audit-logs','/api/appointments']:
        status,_=api('GET',path,token=token);check(key+' blocked '+path,status==403,status)
    status,_=api('POST','/api/admin/users',dict(name='Denied',email='denied@example.test',password=PASSWORD,phoneNumber='denied',role='ROLE_ADMIN',gender='OTHER',dateOfBirth='1990-01-01',address='Test'),token)
    check(key+' cannot provision staff',status==403,status)
for path in [f'/api/users/{pid2}',f'/api/patients/{pid2}',f'/api/notifications/user/{pid2}',f'/api/appointments/patient/{pid2}','/api/otp?email=phase3-patient2@example.test']:
    status,_=api('GET',path,token=p);check('patient cross-user blocked '+path,status==403,status)
for path in [f'/api/doctor-schedules/doctor/{did2}',f'/api/appointments/doctor/{did2}']:
    status,_=api('GET',path,token=d);check('doctor cross-doctor blocked '+path,status==403,status)
for token in [None,'bad.jwt.value']:
    status,_=api('GET',f'/api/users/{pid}',token=token);check('unauthenticated or invalid JWT401 '+str(token),status==401,status)
def b64(v):return base64.urlsafe_b64encode(v).rstrip(b'=')
header=b64(json.dumps(dict(alg='HS384')).encode());payload=b64(json.dumps(dict(sub=fixtures['patient']['email'],exp=int(time.time())-60)).encode());signature=b64(hmac.new(base64.b64decode(stage.values['jwt']),header+b'.'+payload,hashlib.sha384).digest())
expired=(header+b'.'+payload+b'.'+signature).decode();status,_=api('GET',f'/api/users/{pid}',token=expired);check('signed expired JWT401',status==401,status)
status,_=api('POST','/api/auth/login',dict(email=fixtures['patient']['email'],password='incorrect'));check('incorrect password rejected',status==400,status)
for method,path,body in [('POST','/api/audit-logs',dict(action='Forged',details='No')),('DELETE','/api/audit-logs/1',None)]:
    status,_=api(method,path,body,a);check('audit immutable '+method,status==403,status)
# Controlled malformed input and UTF-8 byte-length validation.
status,body=api('POST','/api/auth/register',dict(name='Long',email='long@example.test',password='密'*30,phoneNumber='long',role='ROLE_PATIENT',gender='OTHER',dateOfBirth='1990-01-01',address='Test'))
check('72-byte BCrypt bound returns400 without implementation leak',status==400 and 'BCrypt' not in json.dumps(body),body)
# Real concurrency against PostgreSQL. Dates avoid the browser's next-day UI fixtures.
day=(dt.date.today()+dt.timedelta(days=4)).isoformat()
def booking(patient,when,slot=None):
    body=dict(doctorId=did,patientId=patient,appointmentDateTime=day+'T'+when,notes='Integration concurrency')
    if slot:body['timeSlotId']=slot
    return body
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    responses=list(pool.map(lambda _:api('POST','/api/appointments',booking(pid,'11:00:00'),p),range(4)))
statuses=sorted(s for s,_ in responses);check('slotless concurrent booking exactly1 success and3 controlled409',statuses==[201,409,409,409],statuses)
check('slotless concurrency persisted exactly1',sql(f"SELECT count(*) FROM appointments WHERE doctor_id={did} AND appointment_date_time='{day} 11:00:00'")=='1')
status,slot=api('POST','/api/time-slots',dict(doctorId=did,slotDate=day,startTime='12:00',endTime='12:30'),d);check('doctor creates own slot',status==201,status);sid=slot['id']
status,_=api('POST','/api/appointments',booking(pid,'12:15:00',sid),p);check('mismatched slot time rejected400',status==400,status)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    responses=list(pool.map(lambda _:api('POST','/api/appointments',booking(pid,'12:00:00',sid),p),range(4)))
statuses=sorted(s for s,_ in responses);check('slotted concurrent booking exactly1 success and3 controlled409',statuses==[201,409,409,409],statuses)
first=next(body for status,body in responses if status==201)
status,_=api('PATCH',f"/api/appointments/{first['id']}/cancel",token=p);check('patient cancels own booking',status==200,status)
status,second=api('POST','/api/appointments',booking(pid2,'12:00:00',sid),p2);check('canceled slot rebooked by second patient',status==201,status)
check('both canceled and active booking history retained',sql(f'SELECT count(*) FROM appointments WHERE time_slot_id={sid}')=='2')
status,_=api('GET',f'/api/time-slots/{sid}',token=p);check('old patient cannot inspect new slot booking',status==403,status)
status,_=api('GET',f"/api/appointments/{second['id']}",token=p);check('old patient cannot inspect new appointment',status==403,status)
status,_=api('PATCH',f"/api/appointments/{first['id']}/cancel",token=p);check('repeated cancellation controlled',status==200,status)
check('old cancellation does not release newer booking',sql(f'SELECT status FROM time_slots WHERE id={sid}')=='BOOKED')
status,assigned=api('GET','/api/patients',token=d);check('doctor sees assigned patients only',status==200 and {v['id'] for v in assigned}=={pid,pid2},assigned)
status,assigned=api('GET','/api/patients',token=d2);check('other doctor has no patient directory disclosure',status==200 and assigned==[],assigned)
for path in [f'/api/users/{pid}',f'/api/patients/{pid}',f'/api/doctors/{did}']:
    status,body=api('DELETE',path,token=a);check('clinical history protects '+path,status==409 and 'SQL' not in json.dumps(body),body)
check('blocked deletions preserve accounts and appointments',sql(f'SELECT count(*) FROM users WHERE id IN ({pid},{did})')=='2' and sql(f'SELECT count(*) FROM appointments WHERE time_slot_id={sid}')=='2')
# Non-destructive profile unlink on doctor2 (no clinical history), then relink.
status,_=api('DELETE',f'/api/doctors/{did2}',token=a);check('no-history doctor profile unlink',status==204,status)
check('unlink preserves doctor account and role',sql(f'SELECT role FROM users WHERE id={did2}')=='ROLE_DOCTOR')
status,_=api('POST','/api/doctors',dict(userId=did2,experience=5,specialization='Cardiology',availableHours='09:00-17:00'),a);check('doctor can be relinked',status==201,status)
status,_=api('POST','/api/doctors',dict(userId=pid,experience=5,specialization='Cardiology',availableHours='09:00-17:00'),a);check('profile linking cannot change patient role',status==400,status)
# SMTP failure is a real refused connection after stopping only our dedicated mail sink.
before=sql("SELECT count(*) FROM otp_tokens WHERE email='phase3-patient2@example.test'")
subprocess.run(['docker','stop',stage.container('mailpit')],check=True,capture_output=True)
try:
    status,body=api('POST','/api/otp/send',dict(email=fixtures['patient2']['email'],purpose='PASSWORD_RESET'))
    check('SMTP outage returns503',status==503,body)
    check('SMTP outage rolls OTP row back',sql("SELECT count(*) FROM otp_tokens WHERE email='phase3-patient2@example.test'")==before)
finally:subprocess.run(['docker','start',stage.container('mailpit')],check=True,capture_output=True)
# Keep patient UI account password unchanged for browser reset; API reset uses patient2.
status,body=api('POST','/api/otp/send',dict(email=fixtures['patient2']['email'],purpose='PASSWORD_RESET'));check('SMTP OTP request successful without metadata/code disclosure',status==201 and set(body)=={'message'},body)
messages=json.load(urlopen('http://localhost:18026/api/v1/messages'))['messages'];message=next(m for m in messages if any(t['Address']==fixtures['patient2']['email'] for t in m['To']))
mail=json.load(urlopen('http://localhost:18026/api/v1/message/'+message['ID']))
import re
code=re.search(r'code is: (\d{6})',mail['Text']).group(1)
check('OTP received through actual SMTP inbox',True,{'recipient':fixtures['patient2']['email'],'messageId':message['ID']})
status,grant=api('POST','/api/otp/verify',dict(email=fixtures['patient2']['email'],purpose='PASSWORD_RESET',otpCode=code));check('OTP server verification issues reset grant',status==200 and bool(grant.get('resetToken')),status)
status,_=api('POST','/api/otp/verify',dict(email=fixtures['patient2']['email'],purpose='PASSWORD_RESET',otpCode=code));check('consumed OTP reuse rejected',status==400,status)
reset=dict(resetToken=grant['resetToken'],newPassword='Updated-pass-456!',confirmPassword='Updated-pass-456!')
status,_=api('POST','/api/auth/password-reset',reset);check('real SMTP password reset',status==200,status)
status,_=api('POST','/api/auth/password-reset',reset);check('reset grant reuse rejected',status==400,status)
status,_=api('POST','/api/auth/login',dict(email=fixtures['patient2']['email'],password=PASSWORD));check('old password rejected after reset',status==400,status)
status,_=api('POST','/api/auth/login',dict(email=fixtures['patient2']['email'],password='Updated-pass-456!'));check('new password works',status==200,status)
status,_=api('GET',f'/api/users/{pid2}',token=p2);check('pre-reset JWT revoked401',status==401,status)
# PostgreSQL consumed state and server audit actor persistence.
check('OTP consumption persisted in PostgreSQL',sql("SELECT is_used::text||'|'||(used_at IS NOT NULL)::text FROM otp_tokens WHERE email='phase3-patient2@example.test' ORDER BY id DESC LIMIT 1")=='true|true')
status,logs=api('GET','/api/audit-logs',token=a);check('password reset audit actor is verified target',status==200 and any(v['action']=='PASSWORD_RESET' and v['userId']==pid2 for v in logs))
check('no forged audit records',all(v['action']!='Forged' for v in logs))
(OUT/'api-summary.json').write_text(json.dumps(dict(passed=sum(v['passed'] for v in results),failed=sum(not v['passed'] for v in results)),indent=2))
print(json.dumps(dict(passed=len(results),failed=0)))
