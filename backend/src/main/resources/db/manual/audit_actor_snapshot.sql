-- Operator-only migration: back up and stop application writers before running.
-- Preserve attribution independently of the optional live account relationship.
BEGIN;
ALTER TABLE public.audit_logs ADD COLUMN IF NOT EXISTS actor_user_id bigint;
UPDATE public.audit_logs SET actor_user_id = user_id WHERE actor_user_id IS NULL AND user_id IS NOT NULL;
COMMIT;
