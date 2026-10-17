# Health Mate production deployment plan

Date: 2026-10-17. Source: `codex/phase2-production-fixes` at `8b2a05f`; preparation branch: `codex/phase3-production-readiness`.

## Intended outcome and limits

Prepare and locally rehearse the existing React/Spring Boot/PostgreSQL application for a controlled production release. Preserve the interface and clinical history. Do not connect deployment tools to the existing production database, publish a service, merge or push automatically. External provider credentials, the public domain and an approved production change window are operator-supplied release inputs.

## Deployment design

- Build React with `npm ci` and Vite, and serve immutable static assets from nonroot Nginx. Use relative `/api` URLs and proxy to an internal backend, preserving existing API paths and UI flows.
- Build a Java 17 Spring Boot executable JAR in a multi-stage image; run it as a nonroot user with graceful termination, bounded memory, a read-only filesystem and health probes. Expose only the frontend on loopback during rehearsal. Optional read-only CA and trusted-edge overlays are supplied. A production TLS edge and provider certificates remain explicit operator inputs.
- Set the production profile to schema **validation**, never creation/update, and disable SQL seed initialization. Require explicit database, signing-key and origin configuration. Use a distinct database owner for migrations and a runtime role with CRUD/sequence usage only, without schema/table ownership or CREATE rights.
- Apply the two reviewed migrations manually after a custom-format backup has been restored and validated in isolation. Use an explicit fresh-install schema file only for empty databases, never as an upgrade.
- Exact CORS origins; fail startup for unsafe JWT keys/configuration; bounded login and OTP quotas. SMTP uses environment-supplied credentials, verified sender, certificate identity checking and mandatory TLS in production.
- Provide operator-only first-admin provisioning using a freshly registered, unused patient account, a transactionally guarded SQL promotion and an audit record. Never add public staff signup or a default administrator password.

## Work sequence and acceptance criteria

- [x] Review all five Phase 2 commits and record actionable release findings.
- [x] Add failing CORS/startup/login-limit regressions; implement only the necessary security/configuration fixes.
- [x] Harden Render configuration so it cannot restore automatic seed/schema mutation; validate against the official Blueprint schema without deployment.
- [x] Create frontend/backend container configuration, private networking, probe routes and secret-free environment templates; build both images.
- [x] Produce an explicit schema baseline from an empty isolated database and compare it with JPA validation. No production startup may generate schemas.
- [x] Back up synthetic legacy data, restore it to staging, run both migrations and compare every preserved record. Verify conflicts abort atomically, idempotence, exact index definitions and rollback/restore without clinical loss.
- [x] Create/test a least-privilege runtime account; verify DDL is denied and the app can register, authenticate, book/cancel/rebook, update doctor availability/status and send notifications.
- [x] Document/test guarded first-admin provisioning and refusal to promote existing clinical accounts or create another initial admin.
- [x] Rehearse Docker staging with the production profile, a private PostgreSQL database and a local SMTP inbox. Verify CORS, JWT/RBAC, health/readiness, outage behavior, graceful restart and schema/data stability across startup.
- [x] Run Playwright CLI against built static assets and the containerized real backend at 1440/390/320px. Use real SMTP for recovery; no mocked/intercepted API responses.
- [x] Run backend tests and frontend type/build checks, review dependency findings and container metadata, then clean only disposable staging resources.
- [x] Write a final report with evidence and separate verified checks from remaining release blockers.

The checked items record completed local preparation/rehearsal, not approval to release. See [the readiness report](production-readiness-report.md) for remaining gates and [the dependency review](dependency-review.md) for the 77 advisory matches.

## Release gates

A public production release requires all critical gates satisfied: validated data-preserving migrations and tested restore; no known seed credentials; private JWT key; an explicit origin/domain allowlist; external database TLS/access controls; externally verified SMTP TLS/auth/delivery; public HTTPS/headers; dependency/advisory review; production backup retention/restore ownership and alerting; and an explicit decision on durable transactional clinical auditing. The existing post-commit audit filter cannot guarantee audit persistence if its final write fails; staging success must not hide that limitation.

## Rollback decision

Prefer restoring the previous application image while retaining a compatible expanded schema, after testing compatibility. Never reinstate full slot uniqueness when canceled/rebooked history shares a slot. Stop writers before recovery, preserve a fresh backup of the current migrated database, and restore into a **new** database for reconciliation/cutover. An old pre-migration backup alone loses subsequent clinical writes and must not be blindly restored over production. No destructive rollback is automatic.

## Primary configuration references

- Docker production composition and health dependencies: https://docs.docker.com/compose/how-tos/production/ and https://docs.docker.com/compose/how-tos/startup-order/
- Spring Boot health groups: https://docs.spring.io/spring-boot/4.0/reference/actuator/endpoints.html
- Nginx nonroot container: https://github.com/nginx/docker-nginx-unprivileged
- Render Blueprint schema: https://render.com/schema/render.yaml.json
