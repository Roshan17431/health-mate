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
    expected_index record;
    existing_relation oid;
BEGIN
    -- IF NOT EXISTS only checks the relation name. A wrong, nonunique, or invalid
    -- existing index must not silently disable booking protection. Validate both
    -- reserved names before removing any legacy uniqueness; abort for review.
    FOR expected_index IN
        SELECT * FROM (VALUES
            ('uq_appointments_active_doctor_datetime',
             ARRAY['doctor_id', 'appointment_date_time'],
             ARRAY['int8_ops', 'timestamp_ops'],
             'status<>''CANCELED'''),
            ('uq_appointments_active_time_slot',
             ARRAY['time_slot_id'],
             ARRAY['int8_ops'],
             'time_slot_idISNOTNULLANDstatus<>''CANCELED''')
        ) AS expected(index_name, key_columns, operator_classes, predicate)
    LOOP
        existing_relation := to_regclass(format('public.%I', expected_index.index_name));
        IF existing_relation IS NOT NULL AND NOT EXISTS (
            SELECT 1
            FROM pg_index i
            JOIN pg_class idx ON idx.oid = i.indexrelid
            JOIN pg_am am ON am.oid = idx.relam
            WHERE i.indexrelid = existing_relation
              AND i.indrelid = 'public.appointments'::regclass
              AND i.indisunique AND i.indisvalid AND i.indisready AND i.indislive
              AND i.indimmediate AND am.amname = 'btree'
              AND i.indexprs IS NULL
              AND i.indnkeyatts = cardinality(expected_index.key_columns)
              AND ARRAY(
                  SELECT a.attname::text
                  FROM unnest(i.indkey) WITH ORDINALITY AS key(attnum, position)
                  JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = key.attnum
                  WHERE key.position <= i.indnkeyatts ORDER BY key.position
              ) = expected_index.key_columns
              AND ARRAY(
                  SELECT op.opcname::text
                  FROM unnest(i.indclass) WITH ORDINALITY AS key(opclass, position)
                  JOIN pg_opclass op ON op.oid = key.opclass
                  JOIN pg_namespace ns ON ns.oid = op.opcnamespace AND ns.nspname = 'pg_catalog'
                  ORDER BY key.position
              ) = expected_index.operator_classes
              AND regexp_replace(pg_get_expr(i.indpred, i.indrelid),
                                 '[[:space:]()]|::text', '', 'g') = expected_index.predicate
        ) THEN
            RAISE EXCEPTION 'Reserved index % has an unexpected or invalid definition. Review it before migrating.', expected_index.index_name;
        END IF;
    END LOOP;

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
    -- multi-key indexes, expression indexes, or indexes supporting other constraints.
    -- INCLUDE payload does not change uniqueness on the single time_slot_id key.
    FOR old_index IN
        SELECT n.nspname, idx.relname
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_namespace n ON n.oid = idx.relnamespace
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attname = 'time_slot_id'
        WHERE i.indrelid = 'public.appointments'::regclass
          AND i.indisunique
          AND i.indnkeyatts = 1
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
