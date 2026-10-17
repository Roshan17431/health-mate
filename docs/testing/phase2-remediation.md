# Health Mate Phase 2 remediation verification

Verified on 2026-10-17. The existing interface was preserved while repairing authentication, authorization, booking integrity, account deletion, audit attribution and OTP delivery.

## Final checks

| Check | Result |
| --- | --- |
| Backend `./mvnw -B -ntp verify` | 58 tests passed; no failures, errors or skips; executable JAR built |
| Frontend `npm run lint` | TypeScript check passed |
| Frontend `npm run build` | Vite production build passed |
| Real Spring Boot/PostgreSQL HTTP checks | 85 assertions passed across the primary, supplemental and final runs |
| Playwright CLI | Nine workflow result groups passed; 24 supporting screenshots captured |
| Production startup without `JWT_SECRET` | Failed closed with a required-secret configuration error |

Backend automated tests use isolated H2 databases and an actual loopback SMTP server. Separate integration checks used a packaged Spring Boot backend, a fresh PostgreSQL 16.15 database and a local Mailpit SMTP inbox. API responses were not mocked or intercepted. Existing production database data was not modified. The dedicated browser, servers and test containers were removed after evidence capture.

## Verified workflows

- Patient-only public signup; rejected doctor/admin public signup; administrator-created staff accounts without replacing the administrator's browser session.
- Server-side role and resource ownership checks for accounts, appointments, schedules, notifications, OTP metadata and audit events; doctor patient directories limited to assigned patients.
- Four concurrent booking requests produce one success and three controlled conflicts, both with and without a slot. Exact doctor/date/start-time matching is enforced.
- Cancellation preserves clinical history, immediately refreshes available slots, and allows rebooking. An old cancellation cannot release a later booking. Slot responses cannot expose another patient's appointment identifier.
- Account/profile deletion with appointment history is blocked. Unlinking an unused profile retains its account. Audit attribution survives legacy-account removal and administrator self-deletion.
- Doctor slot/schedule creation and schedule updates persist. Appointment confirmation/completion remains visible after refresh.
- All five administrator tabs render; doctor linking, notification creation/delivery and visible protected-deletion errors work.
- SMTP failure returns 503 and rolls back the OTP row. A code delivered through SMTP completes browser password recovery. Old passwords, reused OTPs, reused reset grants and pre-reset JWTs are rejected.
- Invalid JWTs and corrupt persisted sessions return the browser to sign-in. Notification read state persists after refresh.
- Patient/doctor dashboards and all five administrator tabs fit 1440px, 390px and 320px viewports without page-wide horizontal overflow or unnamed visible form controls. The expanded staff form also passes at 320px. Both favicon files return HTTP 200.

Detailed local evidence is retained under `output/playwright/remediation/` and includes the report, structured results, logs, SQL verification and screenshots. Generated browser state, raw logs and unrelated design artifacts are not part of the source commits.

## Deployment requirements

1. Back up the intended PostgreSQL database, stop writers and review/apply [the booking migration](../../backend/src/main/resources/db/manual/booking_integrity.sql) and [the audit snapshot migration](../../backend/src/main/resources/db/manual/audit_actor_snapshot.sql). They do not run automatically. The booking migration aborts on conflicting legacy records instead of deleting history. It was tested with the former full slot uniqueness constraint and with canceled/rebooked history.
2. Configure a private Base64 `JWT_SECRET` containing at least 32 random bytes. Production schema changes and seed initialization default to disabled.
3. Configure SMTP credentials and a verified sender using [the environment template](../../backend/.env.example). Local SMTP delivery passed; external-provider authentication, TLS and mailbox delivery require verification with deployment credentials.
4. A trusted deployment operator must provision the first administrator for a new installation. Public signup cannot grant administrative access.

See [backend setup and upgrade guidance](../../backend/README.md) for configuration details. The live integration run was a targeted remediation retest, not an exhaustive penetration or load test.
