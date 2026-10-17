# Production deployment and recovery runbook

This runbook prepares a controlled deployment. It does not authorize public deployment or changes to the existing production database. Obtain the deployment owner's approval and change window before executing production database commands.

## Prerequisites and secrets

Use an approved container host, private PostgreSQL with separate migration-owner/runtime accounts, an HTTPS domain/edge proxy, an SMTP provider with a verified sender, and managed encrypted offsite backups. Use distinct staging/production secrets. Never paste secret values into issues, terminal arguments, build arguments, Dockerfiles, Git or CI logs. Do not run `docker compose config` without `--quiet` on real inputs or publish `docker inspect` environment output.

1. Store a completed copy of [the backend template](../../deploy/backend.env.example) outside Git, mode 600. Generate `JWT_SECRET` with a cryptographic generator (at least 32 random bytes encoded as Base64); do not reuse the repository's development/test key. Configure one-hour access tokens unless the operator's session policy requires another value. Key/password changes invalidate sessions.
2. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and exact `CORS_ALLOWED_ORIGINS`. External PostgreSQL uses `sslmode=verify-full` and its provider CA; use `compose.database-tls.yaml` with `DB_CA_FILE` to mount the public CA certificate read-only at `/run/secrets/db-ca.crt`. Keep the certificate readable by the nonroot runtime (mode 644 is appropriate for a public CA certificate); never mount a private database key or credentials there. The runtime role must not own tables, be a superuser, create schemas/databases/roles, or have replication rights.
3. SMTP port 587 requires `SMTP_AUTH=true`, `SMTP_STARTTLS_ENABLED=true`, `SMTP_STARTTLS_REQUIRED=true`, `SMTP_SSL_ENABLED=false`. Use provider-scoped credentials and a verified `SMTP_FROM`. For port 465 use implicit TLS with `SMTP_SSL_ENABLED=true` and both STARTTLS flags false. Certificate identity checks and finite timeouts remain enabled. The plaintext/unauthenticated SMTP override exists only in `compose.staging.yaml` for the private local inbox.
4. Configure provider SPF/DKIM/DMARC and verify actual delivery, TLS/authentication, failure handling and bounce/abuse monitoring. These external checks require real provider/domain credentials and cannot be inferred from local SMTP success.
5. Pin all release image references to reviewed digests; record source revision, image IDs, dependency/SBOM/advisory review, migration SHA256 and approved rollback image. The Node/JDK/JRE version tags are build inputs, not immutable release identifiers.

## Database preparation — explicit owner actions

Configure a private libpq service file and `pgpass` from [the service example](../../deploy/pg_service.conf.example) and [password-file example](../../deploy/pgpass.example), plus the database CA. Keep `pgpass` mode 600 and its directory mode 700. Commands below pass service names rather than credentials. Use a PostgreSQL client matching the server major version; test restore on the intended version.

Before upgrades, stop application writers, record row counts/schema/index definitions, and make a custom-format full database backup. Use a separate runtime role with only CONNECT, schema USAGE, table SELECT/INSERT/UPDATE/DELETE and sequence USAGE/SELECT. Revoke public schema CREATE and avoid runtime ownership. Apply the same grants for every explicitly added table/sequence under the owner's controlled migration process.

For **a new empty database only**, explicitly run [initial_schema.sql](../../backend/src/main/resources/db/manual/initial_schema.sql); it refuses an existing public schema and inserts no seed users. For an **existing database**, review and apply [booking_integrity.sql](../../backend/src/main/resources/db/manual/booking_integrity.sql) and [audit_actor_snapshot.sql](../../backend/src/main/resources/db/manual/audit_actor_snapshot.sql). Never run initial_schema.sql as an upgrade. Neither script is an automatic Spring startup migration.

Before upgrading, investigate null/unknown appointment statuses, null datetimes, orphan references, duplicate active doctor/time bookings, slot mismatches and seeded credentials. The booking migration refuses duplicate/mismatched active bookings or wrongly defined reserved indexes; it never chooses clinical records to delete. Resolve conflicts by operator review, retaining clinical history. Verify old `data.sql` privileged accounts have had their default passwords rotated or access otherwise disabled by an authorized operator. Disabling future seeding does not secure an existing seeded account.

## First administrator

There is no public staff signup, automatic production admin or default admin password.

1. The trusted operator creates a new patient account through the ordinary HTTPS registration page using an operator-controlled email and a strong private password. Do not use an existing clinical account.
2. With the **table owner** connection, run [provision_initial_admin.sql](../../backend/src/main/resources/db/manual/provision_initial_admin.sql), setting psql variable `admin_email` to that new account. It locks provisioning, requires no administrator already exist, rejects doctor/appointment-linked accounts, changes only the selected account's role, preserves its patient profile, and records the trusted database operator in an audit event. Zero eligible rows fail without changes.
3. Sign out and sign in again. Verify the administrator sees all five dashboard tabs, creates staff through People, and links doctor profiles through Doctors. Provision at least two separately controlled administrators for operational continuity.
4. Treat loss of every administrator as an incident. The same guarded bootstrap can recover through a fresh unused account only when no admin exists; use an approved owner session and retain the audit record. Never enable public privileged signup as recovery.

## Build, start and health checks

Run backend `./mvnw -B -ntp verify` and frontend `npm ci`, `npm run lint`, `npm run build`. Production images run the same checks during build. The frontend uses the existing UI with relative `/api` requests through Nginx; backend/DB ports are not published by the production Compose file.

Set `BACKEND_ENV_FILE` to the private file, `BACKEND_IMAGE`/`FRONTEND_IMAGE` to approved image references and `HTTP_PORT` for the loopback edge listener. Validate with `docker compose config --quiet`, then use the explicit approved Compose project to build/start. `compose.yaml` binds only `127.0.0.1`, uses nonroot users, read-only root filesystems, bounded resources/log rotation, no extra Linux capabilities and health dependencies.

The backend image defaults to the production profile. The production profile validates JPA schema and disables SQL initialization. Missing/mismatched schema fails startup; it is not created or repaired automatically. The production image also omits development `data.sql`. Prove runtime DDL denial and compare schema/data before/after startup, as the local rehearsal does. Normal startup must not require owner credentials.

- `/healthz`: Nginx/static process check.
- `/readyz`: backend readiness with database connectivity; load balancer should stop traffic if it fails.
- Backend `/actuator/health/liveness`: application liveness; excludes the DB to avoid restart cascades. The rehearsal observed a readiness 504 during DB outage because the frontend timeout expires before pool acquisition; it is unhealthy. Align pool/proxy/probe timeouts for the actual provider and retest.
- Backend `/actuator/health/readiness`: application readiness plus DB. Full actuator details remain hidden; other actuator routes are not proxied publicly.

For an external database and TLS edge, explicitly include the optional overlays. Complete a private copy of `deploy/nginx-edge.conf.example` with the **actual immediate edge source addresses as seen inside the frontend container**. Keep both trust lists identical, use narrowly scoped trusted addresses, and ensure the edge overwrites incoming forwarding headers. The default frontend trusts no forwarded headers. The overlay remaps client IPs only from the approved edge and accepts its forwarded protocol only from that same source. Confirm Docker/NAT routing does not turn the configured trust address into an address shared with untrusted callers.

```sh
# These values are paths/public configuration, never secret values.
export BACKEND_ENV_FILE=/private/healthmate/backend.env
export DB_CA_FILE=/private/healthmate/database-ca.crt
export EDGE_TRUST_CONFIG_FILE=/private/healthmate/nginx-edge.conf
docker compose -p healthmate-production -f compose.yaml -f compose.database-tls.yaml -f compose.edge.yaml config --quiet
# Execute only in the approved release change window, with reviewed image inputs:
docker compose -p healthmate-production -f compose.yaml -f compose.database-tls.yaml -f compose.edge.yaml up -d --wait
```

At the public TLS edge, enforce HTTPS, correct certificate renewal and HSTS after validating the domain. Proxy only to the loopback frontend listener; prevent direct backend access. The frontend overwrites client-supplied forwarding headers, and the backend uses native trusted-proxy processing. Use the reviewed edge override and verify the actual TLS edge/client-IP chain; do not trust arbitrary incoming X-Forwarded-For. Tune login/OTP quotas for NAT/shared clients. The in-memory quotas support a single backend instance; multiple instances require shared gateway/store enforcement before scaling.

Smoke test registration, all role logins, protected endpoints, booking/cancellation/rebooking/history, doctor availability/status, notifications, SMTP recovery, JWT revocation, refresh restoration and desktop/mobile widths. Monitor API errors/latency, failed auth/rate limits, mail failures, DB capacity/connections, backup age and certificate expiry. Logs and alerts must omit patient notes, passwords, tokens and credentials. Restrict access and define retention for audit and application logs.

## Backup and restore

At minimum back up before every migration and on an approved recurring schedule. Managed PITR/WAL retention, encryption, offsite retention, restore ownership and recovery time/point objectives must be chosen by the deployment owner and tested. A backup is not verified until a restore and clinical-record comparison succeed.

Example owner-operated client commands (mount a private directory as `/run/secrets` read-only; secret values stay in files):

```sh
umask 077
# Run inside an approved PostgreSQL client with PGSERVICEFILE=/run/secrets/pg_service.conf.
pg_dump 'service=healthmate_owner' --format=custom --no-owner --file=/private-backups/healthmate.dump
pg_restore --list /private-backups/healthmate.dump > /private-backups/healthmate.contents
# Only after creating a separate EMPTY recovery database under explicit approval:
pg_restore --dbname='service=healthmate_recovery' --no-owner --exit-on-error /private-backups/healthmate.dump
```

Encrypt backups before offsite storage, restrict owner-only access, verify checksums and protect encryption keys separately. Never put a dump under the frontend's public directory or Git. Restore into an isolated database, validate constraints/indexes, identity sequences and all nine tables, then run application smoke tests with SMTP pointed at a safe sink. The local `scripts/verify_migrations.py` rehearsal verifies custom-format backup, restore, record/sequence preservation, atomic migration refusal and canceled/rebooked history; production retention/encryption/PITR remain separate operator checks.

## Rollback preserving clinical writes

1. Stop writers/traffic and preserve a **new backup of the latest migrated database**, along with current image/config revision and a change ledger. Do not overwrite that database or destroy the failed deployment's evidence.
2. Prefer an application-image rollback that has been proven compatible with the expanded schema. Phase 2's full unique slot constraint is incompatible with intentional canceled/rebooked history; do not reinstate it or delete canceled bookings to force it back.
3. If database recovery is needed, restore the selected backup into a **new database**, reconcile all clinical writes made since that backup, compare all records/sequences/indexes, and verify against an SMTP sink before an explicitly approved cutover.
4. Restoring only the old pre-migration backup discards later clinical writes. It is not an automatic rollback plan. Keep the current database/backup until reconciliation and retention requirements are satisfied.
5. Revalidate readiness, all roles, booking/history, notifications and auth. Rotate compromised secrets if relevant; expect old JWTs to require sign-in. Remove obsolete resources only after approval and verified backup retention.

## Local staging rehearsal

`python3 scripts/staging.py init`, `prepare`, `build`, `up`, `startup-check` create a local-only Compose project with private random credentials, a restored synthetic legacy backup, explicit migrations, a separately validated fresh schema and a non-owner runtime role. Frontend is `http://localhost:3300`, local SMTP inbox `http://localhost:18026`; existing 3000/8080 services are separate. After the initial no-write startup check, run `scripts/staging_schema.py`, `scripts/staging_fixtures.py`, `scripts/staging_api.py`, and `scripts/staging_http.py`. Use the Playwright CLI skill wrapper against port 3300; the Phase 3 evidence directory contains the recorded browser scripts. Run outage/recovery scripts only after closing browser sessions and completing writes. `scripts/staging_tls.py` rehearses the optional CA/edge overlays with disposable certificates, a real PostgreSQL TLS connection, separate client IPs and forwarding-header spoof tests. It returns to the local base topology afterward. `scripts/staging_operations.py` and `scripts/staging_restore.py` create separate recovery databases, validate restores and exercise backend recovery. These scripts are intended for one fresh rehearsal context, not repeated fixture creation in an existing context.

Use `down` to remove only that project's services/volumes/private credentials after evidence capture. Do not use the staging override in production.
