"""Create synthetic accounts only in the explicitly owned Phase 3 rehearsal."""
from pathlib import Path
from urllib.request import Request,urlopen
import json
from staging import Stage,MANUAL,OUT
s=Stage();BASE='http://localhost:3300';PASSWORD='Integration-pass-123!'
def post(path,body,token=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    with urlopen(Request(BASE+path,data=json.dumps(body).encode(),headers=headers)) as response:return json.load(response)
def user(key,role='ROLE_PATIENT'):
    return dict(email='phase3-'+key+'@example.test',name='Staging '+key.title(),password=PASSWORD,phoneNumber='phase3-'+key,role=role,gender='OTHER',dateOfBirth='1990-01-01',address='Synthetic isolated staging')
def provision(email):
    return s.run(['docker','exec','-i',s.container('postgres'),'psql','-X','-U','healthmate_owner','-d','healthmate_staging','-v','ON_ERROR_STOP=1','-v','admin_email='+email],(MANUAL/'provision_initial_admin.sql').read_bytes(),check=False)
assert s.sql("SELECT count(*) FROM users WHERE email LIKE 'phase3-%'").stdout.strip()==b'0','Fixtures already exist; do not overwrite.'
# Clinical identities and an existing administrator must prevent provisioning atomically.
before=s.rows(); denied=provision('patient@example.invalid')
assert denied.returncode and s.rows()==before
admin=post('/api/auth/register',user('admin'))['user']; assert provision(admin['email']).returncode==0
admin['role']='ROLE_ADMIN'
assert s.sql("SELECT count(*) FROM audit_logs WHERE action='INITIAL_ADMIN_PROVISIONED'").stdout.strip()==b'1'
assert s.sql(f"SELECT count(*) FROM patients WHERE user_id={admin['id']}").stdout.strip()==b'1'
before=s.rows();denied=provision('admin@example.invalid');assert denied.returncode and s.rows()==before
token=post('/api/auth/login',dict(email=admin['email'],password=PASSWORD))['token']
fixtures={'admin':admin}
for key in ['doctor','doctor2']:
    fixtures[key]=post('/api/admin/users',user(key,'ROLE_DOCTOR'),token)
    post('/api/doctors',dict(userId=fixtures[key]['id'],experience=5,specialization='Cardiology',availableHours='09:00-17:00'),token)
for key in ['patient','patient2']:fixtures[key]=post('/api/auth/register',user(key))['user']
(OUT/'fixtures.json').write_text(json.dumps(fixtures,indent=2))
(OUT/'admin-provisioning.json').write_text(json.dumps(dict(clinicalIdentityRejectedAtomically=True,freshAccountPromoted=True,patientProfilePreserved=True,operatorAuditRecorded=True,secondAdminProvisionRejectedAtomically=True,adminReloginSuccessful=True),indent=2))
print('Five synthetic accounts created through real APIs; guarded owner bootstrap checks passed.')
