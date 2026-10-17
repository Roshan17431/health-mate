# Health Mate — Phase 3 production-readiness report

Date: 2026-10-17. **Decision: NOT READY for public production deployment.** The deployment configuration and local staging rehearsal are prepared and validated. Security and production-infrastructure gates remain open; passing functional tests does not close them.

Reviewed source: the five commits on `codex/phase2-production-fixes`, from `d097d8d` through `8b2a05f`. Preparation branch: `codex/phase3-production-readiness`; the staging rehearsal ran locally. No public service was deployed, no branch was merged, and no production database was modified. The existing React components/design were preserved.

## Environment and scope

The rehearsal used built React/Vite assets served by nonroot Nginx, the Java 17 Spring Boot production profile, PostgreSQL 16.15 in a dedicated Docker project, and a private Mailpit SMTP sink. An additional disposable TLS edge and CA exercised the optional trusted-edge/database-TLS overlays, including real PostgreSQL TLS and distinct client IPs. The staging frontend was **http://localhost:3300**; it proxied `/api` to the internal backend. PostgreSQL and SMTP inspection ports were loopback-only. Existing 3000/8080 services and the normal database were separate.

All workflow results below used the real Spring backend, PostgreSQL and SMTP. There was no API mocking, interception, or fabricated response. Synthetic records and a representative legacy schema were backed up/restored into isolation; no production clinical dump was taken. Compatibility with actual production schema drift, data conflicts, server version, extensions and provider configuration therefore remains an operator release gate.

## Configuration and review outcomes

- **Startup and database safety:** the backend image defaults to `prod`; production uses `ddl-auto=validate` and SQL initialization `never`. The production image omits development `data.sql`. Runtime connections use a separate non-owner account without schema/table CREATE/ALTER privileges. Startup/recreation preserved schema and all existing rows; a missing schema failed startup without creating tables. External operator overrides still need review; least-privilege DB credentials are the hard DDL boundary.
- **JWT and origins:** production requires explicit private database configuration, an exact HTTPS origin allowlist (loopback HTTP is allowed for rehearsal), and a private Base64 signing key of at least 32 random bytes. The repository development key is rejected. Wildcard CORS was replaced with the explicit allowlist. Access tokens default to one hour in deployment templates; password-reset grants remain short-lived and use a separate derived signing key.
- **Authentication:** added bounded per-process login quotas with generic 429 responses and `Retry-After`, account normalization, bounded request bodies and retained MVC validation. Existing login, registration, authorization and password reset passed regression tests. Multi-instance deployments require shared enforcement before scaling.
- **Containers and ingress:** multi-stage builds, nonroot users, read-only filesystems, bounded resources, dropped capabilities, health dependencies, graceful shutdown and loopback frontend publishing are configured. Nginx serves the existing SPA and same-origin API paths, security/cache headers and favicon. Optional `compose.edge.yaml` and an explicit HTTP-context trust file preserve client IP/protocol only from approved immediate edges; the default ignores supplied forwarding headers. `compose.database-tls.yaml` mounts the operator-provided public database CA read-only for `verify-full`, which validates chain and hostname as described by [pgJDBC](https://jdbc.postgresql.org/documentation/ssl/). Dynamic Docker DNS supports backend recreation. Health checks follow actual Spring `SERVER_PORT`/`PORT` precedence; the image does not force a conflicting port.
- **SMTP:** private environment templates configure authenticated STARTTLS on 587, required TLS, certificate identity verification and finite timeouts. Port 465 instructions cover implicit TLS. Only the isolated local override disables SMTP TLS/authentication. Real external provider delivery remains unverified.
- **Administrator:** an explicit owner-operated SQL bootstrap promotes only a freshly registered, unused patient when no admin exists, retains its patient profile, and records the trusted operator. Clinical identities and a second initial-admin attempt were refused atomically. Ordinary staff provisioning remains authenticated/admin-only.
- **Render alternative:** the Blueprint has manual deployment triggers, readiness checks, explicit secret inputs, safe schema/seed settings and corrected frontend output paths. Official schema validation passed. The Render alternative was not deployed or exercised; the Docker deployment is the rehearsed topology.

See the [deployment plan](production-plan.md), [operator runbook](runbook.md), and [dependency review](dependency-review.md).

## Passed checks and evidence

Counts in this table are separate evidence groups, not a combined coverage percentage. Evidence links refer to generated files in the local `output/playwright/phase3/` directory; raw logs, screenshots and temporary test artifacts are excluded from source commits. The scripts and this report are versioned so the rehearsal can be reproduced.

| Check | Result | Evidence |
|---|---|---|
| Backend Maven `verify` | 72 tests; zero failures/errors/skips, including the prior 58 plus 14 configuration/CORS/login-limit regressions; Java 17 Docker build succeeded | [Build log](../../output/playwright/phase3/docker-build-final.log) |
| Frontend clean install, TypeScript `lint`, Vite build | Passed; no separate frontend unit-test script exists | [Docker build log](../../output/playwright/phase3/docker-build.log) |
| Frontend dependency audit | Zero reported findings after compatible lockfile updates | [npm audit](../../output/playwright/phase3/npm-audit.json) |
| Manual migration verifier | 23 passed, zero failed | [Migration results](../../output/playwright/phase3/migrations.json) |
| Restored legacy staging and runtime DDL denial | Nine-table restore/clinical comparison passed; CREATE and ALTER denied | [Preparation](../../output/playwright/phase3/staging-prepare.json) |
| Production startup/recreation | Schema/data unchanged, nonroot and read-only containers healthy | [Startup](../../output/playwright/phase3/startup.json), [final metadata](../../output/playwright/phase3/final-container-metadata.json) |
| Fresh/missing schema and port override | Fresh explicit baseline validates; empty schema refuses startup without DDL; `PORT` health works with image's default production profile | [Fresh schema](../../output/playwright/phase3/fresh-schema.json) |
| First administrator | Guarded bootstrap, profile preservation, operator audit and atomic refusal cases passed | [Admin provisioning](../../output/playwright/phase3/admin-provisioning.json) |
| Primary real HTTP/PostgreSQL suite | 67 distinct checks, 72 recorded assertions including repeat logins after a harness interruption; all passed | [API results](../../output/playwright/phase3/api-results.json) |
| Proxy/CORS/headers/favicon/image checks | 11 passed | [HTTP configuration](../../output/playwright/phase3/http-configuration.json) |
| Browser-action database verification and expired SMTP OTP | 9 passed | [Database verification](../../output/playwright/phase3/database-browser-verification.json) |
| Real JWT and production-key startup checks | 3 passed; valid issued token 200, identical credential claims with expired signed token 401, public development key refused without writes | [JWT checks](../../output/playwright/phase3/jwt-startup-verification.json) |
| Outage, restart, backup and rate-limit drills | 13 passed | [Operations](../../output/playwright/phase3/operations.json) |
| PostgreSQL TLS and edge trust | 8 passed using a disposable CA: nonroot CA mount, actual verify-full DB connections, certificate-verified HTTPS, distinct client quotas and forwarded-protocol/header spoof protection | [TLS/proxy checks](../../output/playwright/phase3/tls-proxy.json) |
| Final current-state restore plus restored role logins/data | 8 passed | [Final restore](../../output/playwright/phase3/final-restore.json) |
| Playwright CLI real-browser workflows | Seven recorded flow groups; 21 role/tab/viewport cases; nine additional final-image role/viewport smoke checks; zero unexpected console errors | [Browser results](../../output/playwright/phase3/browser-results.json), [final smoke](../../output/playwright/phase3/final-smoke.json), [console](../../output/playwright/phase3/browser-console-all.log) |
| Render schema, Python compilation, whitespace checks | Passed; no deployment performed | [Render validation](../../output/playwright/phase3/render-validation.json) |

### Authentication and permissions

Registration persisted a patient with BCrypt password storage. Public privileged signup, unauthorized admin/staff operations, cross-patient and cross-doctor private access, missing/invalid/expired tokens, forged audit mutations and incorrect passwords were rejected. Existing valid sessions worked. Login quotas returned 429 plus `Retry-After` on repeated attempts.

Both API and browser recovery used real email received through SMTP: wrong/expired OTPs failed, a correct OTP produced the new-password form, used OTP/reset grants could not be reused, the old password stopped working, the new password authenticated, and pre-reset JWTs failed. Browser logout cleared session storage; a real backend 401 cleared an invalid restored session. This verifies client logout, not server-side revocation of every logged-out access token.

### Patient, doctor and admin workflows

Patients fetched real specialties/doctors/slots, booked, refreshed, canceled and rebooked. PostgreSQL retained canceled and current/completed history. Concurrent bookings produced one success and controlled conflicts rather than duplicates; slot/time mismatches failed. Clinical account/profile deletion was refused without losing appointments.

Doctors created concrete slots and weekly schedules, updated availability, retrieved assigned patients/appointments, confirmed and completed visits, and retained changes after refresh. Another doctor's private schedules/appointments were forbidden.

Administrators used all five tabs (People, Doctors, Appointments, Audit logs, Notifications), created a doctor account and linked its profile, retained the admin session, created a delivered notification and saw a visible history-protection error when deletion was refused. The patient marked the notification read and PostgreSQL confirmed that state.

### Browser and visual checks

The bundled Playwright CLI wrapper drove the built staging site. Patient and doctor dashboards, plus each admin tab, were checked at **1440px, 390px and 320px**. Visible controls had names/associated labels, page width did not exceed the viewport, and ordinary successful flows raised no page errors. Existing inner table scrolling was preserved. Sample screenshots were inspected visually.

The six console error messages were expected negative tests: protected deletion (409), wrong OTP/old password (400), and invalid-session 401 requests plus the shell's corresponding log. No additional console/CSP/resource errors were found. This was Chromium verification, not a cross-browser/device certification or a screen-reader audit. Transient loading states were not exhaustively timing-tested.

Screenshots: [patient 320](../../output/playwright/phase3/patient-320.png), [doctor 320](../../output/playwright/phase3/doctor-320.png), [completed visit](../../output/playwright/phase3/doctor-completed-320.png), [admin People 320](../../output/playwright/phase3/admin-people-320.png), [admin Appointments 1440](../../output/playwright/phase3/admin-appointments-1440.png), [reset form 320](../../output/playwright/phase3/reset-form-320.png), [invalid-session clearing](../../output/playwright/phase3/invalid-session-320.png). Other widths/tabs are in the same evidence directory.

## Migration, backup and rollback findings

`booking_integrity.sql` originally could silently accept an incorrectly defined reserved index and missed a legacy unique index with included payload columns. It now validates existing reserved definitions before removing old uniqueness and targets the correct key columns. Tests cover wrong/nonunique/invalid/predicate/table definitions, duplicate active bookings, doctor/date/time mismatches, atomic refusal, idempotence and canceled-slot rebooking. It deletes no appointments. `audit_actor_snapshot.sql` retains prior actor snapshots and backfills missing ones without removing history.

A synthetic legacy custom-format backup was restored before migration; all nine tables were compared. Final migrated backups, schema and identity sequences were restored into separate databases after real browser writes. All three roles logged in and retrieved authorized data from a restored database under the non-owner runtime role. Backup checksums are recorded; temporary dumps contained synthetic records only.

The fresh-install SQL is for an empty public schema only and inserts no users. Production upgrades must review null/unknown appointment statuses, null datetimes, orphaned or inconsistent legacy references, duplicates, exact indexes and target PostgreSQL compatibility. The migration's ordinary comparisons alone do not certify malformed nullable legacy rows. Do not delete clinical history to force a migration through.

Rollback must retain a backup of the **latest** migrated state and reconcile writes before any new-database cutover. Reinstating old full slot uniqueness would conflict with legitimate canceled/rebooked history. Restoring only a pre-migration backup would lose later clinical writes. No destructive rollback was run. The current build was tested against restored migrated state; a previous application image was not certified as compatible, so automatic image rollback is not authorized.

## Failures found and resolved during preparation

- Wildcard CORS accepted an untrusted origin: a failing regression reproduced it; the exact allowlist now rejects it.
- Missing login throttling: repeated sign-ins initially exceeded the intended limit; unit/real-HTTP tests and the production staging quota now pass.
- Invalid reserved migration indexes and legacy INCLUDE uniqueness: unsafe/no-op cases now refuse atomically or remove only the obsolete exact key; 23 migration checks pass.
- Missing production CA mount and unspecified edge trust: explicit optional overlays and runbook invocations now supply these; local TLS and two-client quota tests verify the pattern. Actual public-edge addresses/provider certificates still require operator validation.
- Container port precedence and Nginx readiness Host: real startup/proxy tests caught mismatches; final port-override health and `/readyz` now return correctly with a healthy DB.
- Five frontend dependency findings: compatible lockfile updates and fresh builds reduced the audit to zero.
- Test-harness issues were corrected: PostgreSQL's temporary initialization socket was mistaken for final readiness (TCP readiness now used), synthetic explicit-ID sequences were aligned, and stopped SMTP-container lookup now uses `ps -a`. The API run resumed after that harness interruption; successful evidence is retained alongside initial logs. Browser harness waits were corrected for the refreshed doctor selection and lowercase status text. All affected flows were rerun successfully; historical failed harness logs are not counted as passing runs.

## Remaining blockers and recommended fixes

| Priority | Finding / unverified gate | Required action |
|---|---|---|
| P1 — release blocker | **Backend advisory backlog:** 20 packages match 77 known advisories, including 10 critical and 29 high severity IDs. Functional tests do not establish exploitability or remediation. | Upgrade/triage a supported compatible dependency baseline, rerun all checks, scan immutable images/build dependencies and record any accepted findings. See [dependency details and caveats](dependency-review.md). |
| P1 — release blocker | **Audit durability:** `AuditTrailFilter` writes after the service transaction has committed. A crash/audit-write failure can leave a successful clinical mutation without a durable audit event; a response may already be committed. Confirmed by code review; fault injection was not performed. | Design and test transactional audit/outbox persistence, retention and failure semantics before clinical production use. No business-logic rewrite was silently made. |
| P1 — operator gate | **Existing production data/schema and seeded credentials were not touched or certified.** Staging used representative synthetic legacy data. | Back up and restore an authorized production-equivalent clone; compare target schema, records, constraints/sequences and PostgreSQL version; review conflict/null preflight; verify/rotate any old seeded privileged credentials using an authorized operator. |
| P1 — infrastructure gate | **Public HTTPS and actual proxy trust chain unverified.** The optional exact-edge trust configuration passed disposable HTTPS, client isolation and spoof tests. Actual public-edge addresses and Docker/NAT routing are not verified; omitting or misconfiguring the overlay can still collapse quotas to the edge IP or lose HTTPS identity. | Configure and verify domain/TLS renewal/HSTS, edge-to-frontend trust boundaries, original client IP/proto, private backend access and rate-limit behavior for real clients/NAT. |
| P1 — infrastructure gate | **External PostgreSQL TLS/access, SMTP TLS/auth/delivery and production backup/PITR/alerting unverified.** Local certificate-verified PostgreSQL TLS, SMTP and Docker restore do not prove provider operation. | Validate certificate identity/CA, scoped credentials, SMTP sender/SPF/DKIM/DMARC, failure/bounce handling, encrypted offsite retention, monitored scheduled backups, RPO/RTO and a timed managed-provider restore. |
| P2 — before scaling | Login/OTP quotas are per-process and reset with restarts; no load/soak/capacity, multi-instance booking or failover campaign was run. | Keep a controlled single instance until shared gateway/store quotas and target-host concurrency/capacity/failover testing are approved. |
| P2 — operational tuning | Database outage made `/readyz` return **504** because the 5-second proxy timeout elapsed before the connection pool's acquisition timeout. It correctly became unhealthy; liveness stayed UP and readiness recovered. | Align connection-acquisition, proxy and load-balancer probe timeouts for the selected provider; repeat the outage drill. Do not interpret the observed 504 as successful DB readiness. |
| P2 — additional hardening | OTP codes remain short-lived plaintext in the private DB; quotas and server-side locking/reuse prevention passed. Last-admin deletion is not guarded; bootstrap recovery is documented. | Consider hashing OTP challenges and an administrative continuity guard with dedicated security/regression tests. |

Container OS/build dependency scanning, a signed SBOM, real production resource sizing, external observability/alert delivery, cross-browser/accessibility certification and public-host end-to-end tests are **not complete**. The verified local rehearsal does not certify those scenarios.

## Final review

An independent final review checked the production defaults, authentication additions, migration/bootstrap safety and deployment configuration. Its CA-mount and trusted-edge findings were addressed with explicit overlays and documented invocation. Follow-up review found no additional actionable defect in those updates. Known release blockers above remain open.

## Evidence and completion state

The [machine-readable summary](../../output/playwright/phase3/checks-summary.json) and evidence under `output/playwright/phase3/` record the checks above. Generated keys/passwords stayed in owner-only temporary files outside Git; report files do not contain signing keys, database credentials, access/reset tokens or OTP codes. Disposable staging containers, volumes and private inputs were removed after final evidence capture; the staging URL on port 3300 is now stopped. Original frontend PID 86685 on 3000, backend PID 91288 on 8080 and PostgreSQL on 5432 remain running. See [cleanup verification](../../output/playwright/phase3/cleanup.json). Deployment and merge remain manual, gated actions.
