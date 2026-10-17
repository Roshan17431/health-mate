-- Operator-only PostgreSQL migration; this directory is not an automatic migration location.
-- Take a backup, stop application writers, and run explicitly against the intended database.
-- This script preserves all appointment rows. Conflicting legacy data aborts the transaction
-- and must be reviewed by an operator before retrying; no records are selected for deletion.
BEGIN;

LOCK TABLE public.appointments IN ACCESS EXCLUSIVE MODE;

DO $$
DECLARE
    old_constraint record;
    old_index record;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.appointments
        WHERE status <> 'CANCELED'
        GROUP BY doctor_id, appointment_date_time
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Multiple non-canceled appointments share a doctor and time. Review existing data before migrating.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.appointments
        WHERE time_slot_id IS NOT NULL AND status <> 'CANCELED'
        GROUP BY time_slot_id
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Multiple non-canceled appointments share a time slot. Review existing data before migrating.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM public.appointments a
        JOIN public.time_slots s ON s.id = a.time_slot_id
        WHERE a.status <> 'CANCELED'
          AND (a.doctor_id <> s.doctor_id
               OR a.appointment_date_time <> s.slot_date + s.start_time)
    ) THEN
        RAISE EXCEPTION 'A non-canceled appointment differs from its slot doctor, date, or start time. Review existing data before migrating.';
    END IF;

    -- Hibernate's former OneToOne generated a full unique constraint on time_slot_id.
    -- Remove only constraints on that exact single column, retaining foreign keys.
    FOR old_constraint IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'time_slot_id'
        WHERE c.conrelid = 'public.appointments'::regclass
          AND c.contype = 'u'
          AND c.conkey = ARRAY[a.attnum]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE public.appointments DROP CONSTRAINT %I', old_constraint.conname);
    END LOOP;

    -- Cover equivalent standalone legacy indexes, without touching partial indexes,
    -- multi-column indexes, expression indexes, or indexes supporting other constraints.
    FOR old_index IN
        SELECT n.nspname, idx.relname
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_namespace n ON n.oid = idx.relnamespace
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attname = 'time_slot_id'
        WHERE i.indrelid = 'public.appointments'::regclass
          AND i.indisunique
          AND i.indnkeyatts = 1
          AND i.indnatts = 1
          AND i.indkey[0] = a.attnum
          AND i.indpred IS NULL
          AND i.indexprs IS NULL
          AND NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conindid = i.indexrelid)
    LOOP
        EXECUTE format('DROP INDEX %I.%I', old_index.nspname, old_index.relname);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_active_doctor_datetime
    ON public.appointments (doctor_id, appointment_date_time)
    WHERE status <> 'CANCELED';

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointments_active_time_slot
    ON public.appointments (time_slot_id)
    WHERE time_slot_id IS NOT NULL AND status <> 'CANCELED';

COMMIT;
