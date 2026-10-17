#!/usr/bin/env python3
"""Rehearse manual migrations only in an owned, disposable PostgreSQL 16 container.

Run from any directory: python3 scripts/verify_migrations.py
Requires Python 3's standard library, Docker CLI/daemon and postgres:16-alpine.
Never connects to a host PostgreSQL instance or reads product environment files.
"""

from __future__ import annotations

import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import tempfile
import time


ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / "scripts/tests"
MIGRATIONS = ROOT / "backend/src/main/resources/db/manual"
OUTPUT = ROOT / "output/playwright/phase3/migrations.json"
CONTAINER = "healthmate-phase3-migration-check"
IMAGE = "postgres:16-alpine"
TABLES = (
    "users", "doctors", "patients", "doctor_schedules", "time_slots",
    "appointments", "notifications", "otp_tokens", "audit_logs",
)
BOOKING = (MIGRATIONS / "booking_integrity.sql").read_text()
AUDIT = (MIGRATIONS / "audit_actor_snapshot.sql").read_text()


class VerificationFailure(RuntimeError):
    pass


def digest(value: object) -> str:
    return hashlib.sha256(json.dumps(value, sort_keys=True).encode()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise VerificationFailure(message)


class Postgres:
    def __init__(self) -> None:
        self.container: str | None = None
        self.database_count = 0
        self.port: str | None = None

    @staticmethod
    def command(args: list[str], data: bytes | None = None,
                check: bool = True, env: dict[str, str] | None = None,
                timeout: int = 90) -> subprocess.CompletedProcess:
        result = subprocess.run(args, input=data, capture_output=True, env=env, timeout=timeout)
        if check and result.returncode:
            raise VerificationFailure(result.stderr.decode(errors="replace")[-2000:].strip()
                                      or "Docker/PostgreSQL command failed")
        return result

    def start(self) -> None:
        existing = self.command(["docker", "container", "inspect", CONTAINER], check=False)
        require(existing.returncode != 0,
                f"Container {CONTAINER} already exists; refusing to touch unowned data")
        env = os.environ.copy()
        env["POSTGRES_PASSWORD"] = secrets.token_urlsafe(32)
        result = self.command([
            "docker", "run", "--detach", "--name", CONTAINER,
            "--label", "healthmate.verification=phase3-migrations",
            "--publish", "127.0.0.1::5432",
            "--tmpfs", "/var/lib/postgresql/data:rw,nosuid,size=512m",
            "--env", "POSTGRES_PASSWORD", IMAGE,
        ], env=env)
        self.container = result.stdout.decode().strip()
        env.pop("POSTGRES_PASSWORD", None)
        for _ in range(90):
            ready = self.exec(["pg_isready", "-h", "127.0.0.1", "-U", "postgres"], check=False)
            if ready.returncode == 0:
                break
            time.sleep(0.3)
        else:
            raise VerificationFailure("Owned PostgreSQL container did not become ready")
        ports = self.command(["docker", "port", self.container, "5432/tcp"]).stdout.decode().strip()
        require(ports.startswith("127.0.0.1:"), "Published database must be loopback-only")
        self.port = ports
        version = self.sql("postgres", "SHOW server_version;")
        require(version.startswith("16."), "Verification requires PostgreSQL 16")

    def exec(self, args: list[str], data: bytes | None = None,
             check: bool = True) -> subprocess.CompletedProcess:
        require(self.container is not None, "No owned container")
        return self.command(["docker", "exec", "-i", self.container, *args], data, check)

    def sql_result(self, db: str, sql: str) -> subprocess.CompletedProcess:
        return self.exec(["psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1", "-U", "postgres", "-d", db],
                         sql.encode(), check=False)

    def sql(self, db: str, sql: str) -> str:
        result = self.sql_result(db, sql)
        if result.returncode:
            raise VerificationFailure(result.stderr.decode(errors="replace")[-2000:].strip())
        return result.stdout.decode().strip()

    def new_database(self, fixture: bool = True) -> str:
        self.database_count += 1
        name = f"migration_case_{self.database_count}"
        self.sql("postgres", f"CREATE DATABASE {name} TEMPLATE template0;")
        if fixture:
            self.sql(name, (FIXTURES / "legacy_schema.sql").read_text())
            self.sql(name, (FIXTURES / "legacy_rows.sql").read_text())
        return name

    def rows(self, db: str, ignore_actor: bool = False) -> dict:
        result = {}
        for table in TABLES:
            key = "user_id" if table in ("doctors", "patients") else "id"
            expression = "to_jsonb(t) - 'actor_user_id'" if ignore_actor and table == "audit_logs" else "to_jsonb(t)"
            value = self.sql(db, f"SELECT coalesce(jsonb_agg({expression} ORDER BY {key}), '[]') FROM public.{table} t;")
            result[table] = json.loads(value)
        return result

    def schema(self, db: str) -> str:
        # Compare constraints, indexes, columns, foreign keys and identity definitions.
        raw = self.exec(["pg_dump", "-U", "postgres", "--schema-only", "--no-owner", "--no-privileges", db]).stdout.decode()
        # PostgreSQL 16 patch releases may include a fresh \restrict token each time.
        return "\n".join(line for line in raw.splitlines()
                         if not line.startswith(("\\restrict ", "\\unrestrict ")))

    def sequences(self, db: str) -> object:
        return json.loads(self.sql(db, """SELECT coalesce(jsonb_agg(to_jsonb(s)
            ORDER BY sequencename), '[]') FROM pg_sequences s WHERE schemaname='public';"""))

    def backup_restore(self, db: str, path: Path) -> tuple[str, dict]:
        backup = self.exec(["pg_dump", "-U", "postgres", "--format=custom", "--no-owner", "--no-privileges", db]).stdout
        require(backup.startswith(b"PGDMP"), "Backup is not PostgreSQL custom format")
        path.write_bytes(backup)
        restored = self.new_database(fixture=False)
        self.exec(["pg_restore", "-U", "postgres", "--dbname", restored,
                   "--no-owner", "--no-privileges", "--exit-on-error", "--single-transaction"], path.read_bytes())
        require(self.rows(db) == self.rows(restored), "Custom backup/restore changed table rows")
        require(self.schema(db) == self.schema(restored), "Custom backup/restore changed schema")
        require(self.sequences(db) == self.sequences(restored), "Custom backup/restore changed identity sequences")
        return restored, {"format": "custom", "bytes": len(backup),
                          "sha256": hashlib.sha256(backup).hexdigest(),
                          "schema_rows_and_identity_sequences_preserved": True}

    def close(self) -> bool:
        if self.container is None:
            return True
        # The immutable ID belongs only to the successful docker run above.
        try:
            result = self.command(["docker", "rm", "--force", "--volumes", self.container], check=False)
            absent = self.command(["docker", "container", "inspect", self.container], check=False)
            return result.returncode == 0 and absent.returncode != 0
        except (OSError, subprocess.TimeoutExpired):
            return False


def expect_abort(pg: Postgres, db: str, sql: str, message: str = "") -> None:
    before_rows, before_schema = pg.rows(db), pg.schema(db)
    result = pg.sql_result(db, sql)
    require(result.returncode != 0, "Unsafe migration unexpectedly committed")
    if message:
        require(message in result.stderr.decode(), f"Expected PostgreSQL failure containing: {message}")
    require(pg.rows(db) == before_rows, "Failed migration changed rows")
    require(pg.schema(db) == before_schema, "Failed migration changed schema")


def insert_appointment(code: str, when: str = "2035-01-03 10:00", slot: str = "2",
                       doctor: int = 1, status: str = "SCHEDULED") -> str:
    return f"""INSERT INTO public.appointments
        (appointment_date_time, status, notes, appointment_code, doctor_id, patient_id, time_slot_id)
        VALUES ('{when}', '{status}', 'Synthetic history', '{code}', {doctor}, 3, {slot});"""


def run_tests(pg: Postgres, temporary: Path, report: dict) -> None:
    def test(name, action):
        started = time.monotonic()
        try:
            detail = action() or {}
            report["tests"].append({"name": name, "status": "passed", **detail,
                                    "seconds": round(time.monotonic() - started, 3)})
            print(f"PASS {name}", flush=True)
        except Exception as error:
            report["tests"].append({"name": name, "status": "failed", "error": str(error),
                                    "seconds": round(time.monotonic() - started, 3)})
            print(f"FAIL {name}: {error}", flush=True)

    def backup_and_preservation():
        source = pg.new_database()
        original = pg.rows(source)
        restored, backup = pg.backup_restore(source, temporary / "pre-migration.dump")
        pg.sql(restored, BOOKING)
        pg.sql(restored, AUDIT)
        require(pg.rows(restored, ignore_actor=True) == original, "Migration changed legacy rows/history/codes")
        require(pg.sql(restored, "SELECT actor_user_id FROM audit_logs WHERE id=1;") == "4", "Audit actor not backfilled")
        require(pg.sql(restored, "SELECT actor_user_id IS NULL FROM audit_logs WHERE id=2;") == "t", "Anonymous audit actor invented")
        return {"tables": list(TABLES), "row_counts": {key: len(value) for key, value in original.items()},
                "legacy_data_sha256": digest(original), "backup": backup,
                "all_legacy_rows_preserved": True, "audit_actor_backfilled": True}
    test("pre_migration_custom_backup_restore_and_all_nine_table_preservation", backup_and_preservation)

    def exact_removal():
        db = pg.new_database()
        pg.sql(db, """
          CREATE UNIQUE INDEX legacy_standalone_slot ON appointments(time_slot_id);
          ALTER TABLE appointments ADD CONSTRAINT keep_composite_slot_patient UNIQUE(time_slot_id, patient_id);
          CREATE UNIQUE INDEX keep_multicolumn_slot ON appointments(time_slot_id, id);
          CREATE UNIQUE INDEX keep_expression_slot ON appointments((abs(time_slot_id)));
          CREATE UNIQUE INDEX keep_partial_slot ON appointments(time_slot_id) WHERE status = 'CANCELED';
          CREATE INDEX keep_nonunique_slot ON appointments(time_slot_id);
        """)
        before = pg.rows(db)
        pg.sql(db, BOOKING)
        require(pg.rows(db) == before, "Targeted removal changed rows")
        require(pg.sql(db, "SELECT count(*) FROM pg_constraint WHERE conrelid='appointments'::regclass AND conname='legacy Slot Uniqueness';") == "0", "Legacy constraint remains")
        require(pg.sql(db, "SELECT to_regclass('public.legacy_standalone_slot') IS NULL;") == "t", "Legacy standalone index remains")
        for name in ("keep_composite_slot_patient", "keep_multicolumn_slot", "keep_expression_slot", "keep_partial_slot", "keep_nonunique_slot"):
            require(pg.sql(db, f"SELECT to_regclass('public.{name}') IS NOT NULL;") == "t", f"Unrelated index/constraint {name} removed")
        require(pg.sql(db, "SELECT count(*) FROM pg_constraint WHERE conrelid='appointments'::regclass AND contype='f';") == "3", "Appointment foreign key removed")
        require(pg.sql(db, "SELECT count(*) FROM pg_constraint c JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attname='appointment_code' WHERE c.conrelid='appointments'::regclass AND c.contype='u' AND c.conkey=ARRAY[a.attnum]::smallint[];") == "1", "Appointment code uniqueness removed")
    test("legacy_slot_removal_targets_exact_key_and_keeps_other_constraints", exact_removal)

    def included_payload():
        db = pg.new_database()
        pg.sql(db, "CREATE UNIQUE INDEX legacy_slot_with_payload ON appointments(time_slot_id) INCLUDE(id);")
        pg.sql(db, BOOKING)
        pg.sql(db, insert_appointment("FIXTURE-INCLUDED-REBOOK"))
        require(pg.sql(db, "SELECT count(*) FROM appointments WHERE time_slot_id=2;") == "2", "Included payload blocks rebooking")
    test("standalone_slot_uniqueness_with_included_payload_removed", included_payload)

    def idempotence_and_uniqueness():
        db = pg.new_database()
        pg.sql(db, BOOKING)
        pg.sql(db, AUDIT)
        before_rows, before_schema = pg.rows(db), pg.schema(db)
        pg.sql(db, BOOKING)
        pg.sql(db, AUDIT)
        require(pg.rows(db) == before_rows and pg.schema(db) == before_schema, "Second migration run changed schema/data")
        pg.sql(db, insert_appointment("FIXTURE-REBOOKED-5"))
        pg.sql(db, insert_appointment("FIXTURE-CANCELED-6", status="CANCELED"))
        expect_abort(pg, db, "BEGIN; " + insert_appointment("FIXTURE-DUPLICATE-TIME", when="2035-01-03 09:00", slot="NULL") + " COMMIT;", "uq_appointments_active_doctor_datetime")
        expect_abort(pg, db, "BEGIN; " + insert_appointment("FIXTURE-DUPLICATE-SLOT", when="2035-01-09 09:00", slot="1", doctor=2) + " COMMIT;", "uq_appointments_active_time_slot")
        require(pg.sql(db, "SELECT count(*) FROM appointments WHERE time_slot_id=2;") == "3", "Canceled/rebooked history missing")
    test("idempotence_and_active_uniqueness_allow_canceled_rebooking", idempotence_and_uniqueness)

    invalid_cases = (
        ("duplicate_doctor_datetime", insert_appointment("FIXTURE-BAD-DUPLICATE", when="2035-01-03 09:00", slot="NULL"), "doctor and time"),
        ("duplicate_slot", 'ALTER TABLE appointments DROP CONSTRAINT "legacy Slot Uniqueness"; ' + insert_appointment("FIXTURE-BAD-SLOT", when="2035-01-03 09:30", slot="1"), "time slot"),
        ("slot_date_mismatch", "UPDATE appointments SET appointment_date_time='2035-01-04 09:00' WHERE id=1;", "differs from its slot"),
        ("slot_time_mismatch", "UPDATE appointments SET appointment_date_time='2035-01-03 09:15' WHERE id=1;", "differs from its slot"),
        ("slot_doctor_mismatch", "UPDATE appointments SET doctor_id=2 WHERE id=1;", "differs from its slot"),
    )
    for name, setup, message in invalid_cases:
        def invalid_legacy_case(setup=setup, message=message):
            db = pg.new_database()
            pg.sql(db, setup)
            expect_abort(pg, db, BOOKING, message)
        test(f"{name}_aborts_transaction_without_data_loss", invalid_legacy_case)

    index_names = ("uq_appointments_active_doctor_datetime", "uq_appointments_active_time_slot")
    for name in index_names:
        for shape in ("nonunique", "wrong_unique", "wrong_predicate", "wrong_table", "wrong_relation", "invalid"):
            def collision(name=name, shape=shape):
                db = pg.new_database()
                if shape == "nonunique":
                    setup = f"CREATE INDEX {name} ON appointments(appointment_code);"
                elif shape == "wrong_unique":
                    setup = f"CREATE UNIQUE INDEX {name} ON appointments(id);"
                elif shape == "wrong_predicate":
                    columns = "doctor_id, appointment_date_time" if name.endswith("datetime") else "time_slot_id"
                    setup = f"CREATE UNIQUE INDEX {name} ON appointments({columns}) WHERE status='CANCELED';"
                elif shape == "wrong_table":
                    setup = f"CREATE UNIQUE INDEX {name} ON users(id);"
                elif shape == "wrong_relation":
                    setup = f"CREATE TABLE {name}(id bigint);"
                else:
                    invalid = pg.sql_result(db, f"CREATE UNIQUE INDEX CONCURRENTLY {name} ON appointments(notes);")
                    require(invalid.returncode != 0, "Invalid-index fixture unexpectedly succeeded")
                    require(pg.sql(db, f"SELECT indisvalid FROM pg_index WHERE indexrelid='public.{name}'::regclass;") == "f", "Invalid-index fixture is valid")
                    setup = ""
                if setup:
                    pg.sql(db, setup)
                expect_abort(pg, db, BOOKING)
                return {"reserved_index": name, "existing_shape": shape, "atomic_abort": True}
            test(f"reserved_index_{name}_{shape}_fails_safely", collision)

    def actor_preservation():
        db = pg.new_database()
        pg.sql(db, "ALTER TABLE audit_logs ADD COLUMN actor_user_id bigint; UPDATE audit_logs SET actor_user_id=987654321 WHERE id=1;")
        before = pg.rows(db)
        pg.sql(db, AUDIT)
        pg.sql(db, AUDIT)
        require(pg.rows(db) == before, "Existing actor attribution overwritten")
        pg.sql(db, "UPDATE audit_logs SET user_id=NULL WHERE id=1; DELETE FROM users WHERE id=4;")
        require(pg.sql(db, "SELECT actor_user_id FROM audit_logs WHERE id=1;") == "987654321", "Actor attribution lost after account deletion")
    test("audit_backfill_retains_existing_actor_and_survives_account_deletion", actor_preservation)

    def rollback_rehearsal():
        db = pg.new_database()
        pg.sql(db, BOOKING)
        pg.sql(db, AUDIT)
        pg.sql(db, insert_appointment("FIXTURE-REBOOK-AFTER-MIGRATION"))
        pg.sql(db, "UPDATE time_slots SET status='BOOKED', version=version+1 WHERE id=2;")
        before = pg.rows(db)
        expect_abort(pg, db, """BEGIN;
            DROP INDEX uq_appointments_active_doctor_datetime;
            DROP INDEX uq_appointments_active_time_slot;
            ALTER TABLE appointments ADD CONSTRAINT legacy_rollback_slot_unique UNIQUE(time_slot_id);
            COMMIT;""", "could not create unique index")
        restored, backup = pg.backup_restore(db, temporary / "current-migrated-rollback.dump")
        require(pg.rows(restored) == before, "Latest-backup rollback lost post-migration activity")
        history = pg.sql(restored, "SELECT appointment_code || ':' || status FROM appointments WHERE time_slot_id=2 ORDER BY id;").splitlines()
        require(history == ["FIXTURE-CANCELED-2:CANCELED", "FIXTURE-REBOOK-AFTER-MIGRATION:SCHEDULED"], "Restored canceled/rebooked history differs")
        require(pg.sql(restored, "SELECT status FROM time_slots WHERE id=2;") == "BOOKED", "Restored slot booking lost")
        return {"backup": backup, "full_data_sha256": digest(before), "history": history,
                "old_full_uniqueness_rollback_rejected_atomically": True,
                "latest_migrated_backup_restored_with_history": True}
    test("rollback_rehearsal_preserves_canceled_and_post_migration_rebooked_history", rollback_rehearsal)


def main() -> int:
    report = {
        "started_at_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "scope": "Synthetic isolated PostgreSQL only; no host/product database or environment files read",
        "image": IMAGE, "container_name": CONTAINER,
        "fixture": "Representative legacy JPA schema with all nine tables and synthetic rows",
        "migration_sha256": {"booking_integrity.sql": hashlib.sha256(BOOKING.encode()).hexdigest(),
                             "audit_actor_snapshot.sql": hashlib.sha256(AUDIT.encode()).hexdigest()},
        "tests": [],
    }
    pg = Postgres()
    try:
        pg.start()
        report["published_loopback"] = pg.port
        with tempfile.TemporaryDirectory(prefix="healthmate-phase3-migrations-") as temporary:
            run_tests(pg, Path(temporary), report)
    except Exception as error:
        report["fatal_error"] = str(error)
        print(f"ERROR {error}", flush=True)
    finally:
        report["owned_container_created"] = pg.container is not None
        report["owned_container_removed"] = pg.close()
        report["finished_at_utc"] = dt.datetime.now(dt.timezone.utc).isoformat()
        report["passed"] = sum(test["status"] == "passed" for test in report["tests"])
        report["failed"] = sum(test["status"] == "failed" for test in report["tests"])
        report["status"] = "passed" if not report.get("fatal_error") and report["failed"] == 0 and report["owned_container_removed"] else "failed"
        OUTPUT.parent.mkdir(parents=True, exist_ok=True)
        OUTPUT.write_text(json.dumps(report, indent=2) + "\n")
    print(f"Migration verification: {report['passed']} passed, {report['failed']} failed; evidence: {OUTPUT.relative_to(ROOT)}", flush=True)
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())
