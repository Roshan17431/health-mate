# 🏥 Health Mate

Health Mate is a healthcare management system built using **Spring Boot**, designed to simplify doctor-patient interactions, appointment scheduling, notifications, and healthcare administration.

---

## 🚀 Features

### 👤 User Management
- User Registration & Authentication
- JWT-Based Security
- Role-Based Access Control
- Profile Management

### 🩺 Doctor Management
- Doctor Registration
- Specialization Management
- Experience Tracking
- Availability Management

### 🧑‍⚕️ Patient Management
- Patient Profiles
- Appointment History
- Healthcare Information Tracking

### 📅 Appointment Management
- Book Appointments
- Cancel Appointments
- Appointment Status Tracking
- Unique Appointment Codes

### ⏰ Time Slot Management
- Doctor Time Slot Creation
- Slot Availability Tracking
- Prevent Double Booking using Optimistic Locking

### 🔔 Notification System
- Appointment Notifications
- Read/Unread Tracking

### 🔐 OTP Verification
- Email OTP Generation
- OTP Verification
- Password Reset Support

### 📋 Audit Logging
- User Activity Tracking
- System Action Logging

---

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate

### Database
- MySQL / PostgreSQL

### Authentication
- JWT (JSON Web Token)

### Documentation
- Swagger / OpenAPI

### Build Tool
- Maven

### Utilities
- Lombok
- Jakarta Validation

---

## 📂 Project Structure

```text
src/main/java/com/example/health/mate
│
├── config
├── controller
├── domain
├── dto
├── entity
├── exception
├── repository
├── security
├── service
└── HealthMateApplication
```

---

## 🗄️ Entities

- User
- Doctor
- Patient
- Appointment
- TimeSlot
- DoctorSchedule
- Notification
- OtpToken
- AuditLog

---

## 🔑 User Roles

### Admin
- Manage Users
- Manage Doctors
- View Audit Logs

### Doctor
- Manage Schedule
- View Appointments
- Update Availability

### Patient
- Book Appointments
- View Appointment History
- Manage Profile

---

## ⚙️ Setup & Installation

### Clone Repository

```bash
git clone https://github.com/your-username/health-mate.git
cd health-mate
```

### Configure Database

Update `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/health_mate
spring.datasource.username=root
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### Configure OTP Email

Copy `.env.example` to `.env` in the backend directory and set `SMTP_HOST`,
`SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, and `SMTP_FROM` using your email
provider's SMTP credentials and a sender address verified with that provider.
Keep `.env` private. Port 587 defaults to authenticated SMTP with STARTTLS
required and server certificate identity checking enabled. For a provider that
requires implicit TLS on port 465, set `SMTP_SSL_ENABLED=true`,
`SMTP_STARTTLS_ENABLED=false`, and `SMTP_STARTTLS_REQUIRED=false`.

OTP issuance sends the six-digit code to the requested email address. Codes
expire in 10 minutes and are never returned in the send response. Missing SMTP
configuration or failed delivery returns a generic service-unavailable response;
the failed attempt leaves no new OTP row in the database. The existing account
and IP rate limits still apply to delivery attempts.

For a local SMTP sink, point `SMTP_HOST=localhost` and `SMTP_PORT` to the sink's
SMTP listener and use a test-only `SMTP_FROM`. Set `SMTP_AUTH=false`,
`SMTP_STARTTLS_ENABLED=false`, and `SMTP_STARTTLS_REQUIRED=false` only for that
local sink. Integration tests start their own loopback SMTP sink on an ephemeral
port and use H2; no external emails or production database writes are needed.

### Run Application

```bash
mvn clean install
mvn spring-boot:run
```

Application runs on:

```text
http://localhost:8080
```

---

## 📖 API Documentation

Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

---

## 🔒 Security Features

- JWT Authentication
- BCrypt Password Encryption
- Role-Based Authorization
- Secure REST APIs
- Global Exception Handling

---

## 📌 Future Enhancements

- Video Consultation
- Prescription Management
- Medical Records Storage
- Payment Gateway Integration
- Email & SMS Notifications
- AI Health Assistant
- Analytics Dashboard

---

## 👨‍💻 Author

**Roshan Mathew**

Built using Spring Boot, Spring Security, JPA, JWT, and modern backend development practices.

### Applying the Phase 2 security repairs

Public registration now creates patients only. An authenticated administrator creates doctor/admin accounts through **People → Add a clinic team member** (`POST /api/admin/users`); create the doctor's clinical profile separately in **Doctors**. Existing administrators remain available. New installations need an administrator provisioned by the trusted deployment operator; public signup cannot bootstrap administrative access.

Before upgrading an existing PostgreSQL database, back it up and stop application writers. Review and explicitly run `src/main/resources/db/manual/booking_integrity.sql` and `src/main/resources/db/manual/audit_actor_snapshot.sql` against the intended database. These scripts are **not run automatically**. The booking script aborts if legacy bookings conflict, preserves every appointment, replaces the old slot uniqueness constraint with active-booking uniqueness, and permits canceled-slot rebooking. Resolve any reported legacy conflicts by reviewing records before retrying; do not delete clinical history to force the migration through. `ddl-auto=update` alone cannot remove the old slot constraint.

Account/profile deletion returns HTTP 409 when appointment history exists. Unlinking an unused clinical profile preserves its user account. Audit events come from the server, retain actor IDs after account deletion, and cannot be created/deleted through the API. Password reset invalidates previously issued access tokens; older tokens without a credential fingerprint require sign-in again.

Production startup requires `JWT_SECRET` (a private Base64 signing key of at least 32 random bytes). The production profile fixes `ddl-auto=validate` and SQL initialization to `never`; schema creation and seeds require explicit operator actions. Use explicit schema setup only for a new empty database or a reviewed deployment migration. Keep SMTP credentials and JWT signing keys out of Git.

## Production deployment

Use the [deployment plan](../docs/deployment/production-plan.md), [runbook](../docs/deployment/runbook.md), and [readiness report](../docs/deployment/production-readiness-report.md). Production validates the explicitly migrated schema, disables SQL seeds, and requires private configuration. Local staging uses synthetic records and separate services; it does not authorize changes to production.
