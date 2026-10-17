-- Explicit trusted database-owner operation; never executed by application startup.
-- First create a fresh, unused patient through the normal application registration.
-- Run with psql -X -v ON_ERROR_STOP=1 -v admin_email='operator-controlled@example.com' -f this-file.sql
\if :{?admin_email}
\else
  \echo 'admin_email is required.'
  \quit 1
\endif
\unset provisioned_id
BEGIN;
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_class c JOIN pg_roles r ON r.rolname=current_user
        WHERE c.oid='public.users'::regclass AND (r.rolsuper OR c.relowner=r.oid)
    ) THEN RAISE EXCEPTION 'Initial administrator provisioning requires the table owner.';
    END IF;
END $$;
LOCK TABLE public.users IN SHARE ROW EXCLUSIVE MODE;
UPDATE public.users u SET role='ROLE_ADMIN', updated_date=CURRENT_TIMESTAMP
WHERE u.email=:'admin_email' AND u.role='ROLE_PATIENT'
  AND NOT EXISTS (SELECT 1 FROM public.users WHERE role='ROLE_ADMIN')
  AND NOT EXISTS (SELECT 1 FROM public.doctors d WHERE d.user_id=u.id)
  AND NOT EXISTS (SELECT 1 FROM public.appointments a WHERE a.patient_id=u.id OR a.doctor_id=u.id)
RETURNING u.id AS provisioned_id \gset
\if :{?provisioned_id}
INSERT INTO public.audit_logs(user_id,actor_user_id,action,details,timestamp)
VALUES (:provisioned_id,:provisioned_id,'INITIAL_ADMIN_PROVISIONED','Trusted database operator: '||current_user,CURRENT_TIMESTAMP);
COMMIT;
\echo 'Initial administrator provisioned. Sign in again to obtain a new role-aware session.'
\else
ROLLBACK;
\echo 'No eligible unused patient, or an administrator already exists. No records changed.'
\quit 1
\endif
