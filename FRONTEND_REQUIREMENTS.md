# Health Mate Frontend Requirements

## 1. Document Purpose

This document is the frontend source of truth for building a modern client application for **Health Mate**, a healthcare management system backed by a Spring Boot REST API.

Frontend developers and AI coding tools should be able to build the application from this document without reading backend code. It describes:

- Product scope and expected user workflows.
- Authentication, authorization, and route protection.
- API base configuration, request/response contracts, DTO models, enums, validation rules, and error handling.
- Recommended frontend architecture, pages, components, state management, testing, accessibility, and security expectations.

Unless the backend team changes the API, frontend implementations should follow this document exactly.

## 2. Application Overview

Health Mate supports healthcare operations around users, patients, doctors, doctor schedules, appointment time slots, appointments, notifications, OTP verification, and audit logs.

Primary personas:

- **Unauthenticated visitor**: Registers, logs in, requests OTP, verifies OTP.
- **Patient**: Maintains profile, browses doctors, sees available slots, books/cancels appointments, reads notifications.
- **Doctor**: Manages schedules/time slots, views assigned appointments, updates appointment status, reads notifications.
- **Admin**: Manages users, doctors, patients, appointments, notifications, audit logs, and operational health endpoints.

The frontend must never depend on database entities. It should communicate only through the DTO contracts documented here.

## 3. Backend Runtime Assumptions

Default local backend:

```text
http://localhost:8080
```

API prefix:

```text
/api
```

Recommended frontend environment variables:

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_NAME=Health Mate
```

If using Next.js, use equivalent public runtime variables such as:

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=Health Mate
```

Backend CORS defaults allow local development from:

- `http://localhost:3000`
- `http://localhost:5173`

Preferred frontend development ports:

- Vite: `5173`
- Next.js/React: `3000`

## 4. Global API Rules

All API communication uses JSON unless the response has no content.

Required request headers for JSON endpoints:

```http
Content-Type: application/json
Accept: application/json
```

Protected endpoints also require:

```http
Authorization: Bearer <jwt>
```

Date/time formats:

- `LocalDate`: `YYYY-MM-DD`, example `1995-08-24`
- `LocalDateTime`: ISO date-time, example `2026-07-01T10:30:00`
- `LocalTime`: `HH:mm:ss`, example `09:00:00`
- `DayOfWeek`: uppercase Java day names, example `MONDAY`, `TUESDAY`

Use the user's locale for display, but send backend values in the exact formats above.

## 5. Authentication and Authorization

### 5.1 Public Endpoints

These endpoints do not require a JWT:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/otp/send`
- `POST /api/otp/verify`
- `GET /actuator/health`
- `GET /actuator/info`

### 5.2 Protected Endpoints

All other `/api/**` endpoints require a valid JWT bearer token.

`/actuator/**` beyond health/info requires an admin role.

### 5.3 JWT Handling

Successful registration and login return an `AuthResponseDTO`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 1,
    "name": "Asha Sharma",
    "email": "asha@example.com",
    "phoneNumber": "9876543210",
    "role": "ROLE_PATIENT",
    "gender": "FEMALE",
    "dateOfBirth": "1995-08-24",
    "address": "Pune",
    "createdDate": "2026-06-20T10:00:00",
    "updatedDate": "2026-06-20T10:00:00"
  }
}
```

Frontend requirements:

- Store the token securely. Prefer in-memory state plus refresh-on-login behavior for high-security deployments. If persistence is required, use `sessionStorage` or `localStorage` with clear logout behavior and no sensitive PHI in storage.
- Add the bearer token to every protected API request.
- On `401` or invalid/expired token, clear auth state and redirect to login.
- On `403`, show an access denied state without logging the user out.
- Use `expiresIn` to show session information if needed. The backend currently returns expiration in milliseconds.

### 5.4 Roles

Supported user roles:

```ts
type UserRole = "ROLE_ADMIN" | "ROLE_PATIENT" | "ROLE_DOCTOR";
```

Route guards:

- `ROLE_PATIENT`: patient dashboard, doctor search, appointment booking, patient appointments, notifications.
- `ROLE_DOCTOR`: doctor dashboard, schedule management, time slot management, doctor appointments, notifications.
- `ROLE_ADMIN`: admin dashboard, user management, doctor/patient management, audit logs, all appointment administration.

## 6. TypeScript API Models

Use these interfaces in the frontend API layer. Optionality reflects practical frontend handling; backend responses should include all response DTO fields unless values are absent in persistence.

```ts
export type Gender = "MALE" | "FEMALE" | "OTHER";
export type UserRole = "ROLE_ADMIN" | "ROLE_PATIENT" | "ROLE_DOCTOR";
export type AppointmentStatus = "SCHEDULED" | "COMPLETED" | "CANCELED" | "CONFIRMED";
export type SlotStatus = "AVAILABLE" | "BOOKED" | "BLOCKED";
export type OtpPurpose = "PASSWORD_CHANGE" | "PASSWORD_RESET";
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface AuthRequestDTO {
  email: string;
  password: string;
}

export interface AuthResponseDTO {
  token: string;
  tokenType: "Bearer" | string;
  expiresIn: number;
  user: UserDTO;
}

export interface RegisterRequestDTO {
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
  role?: UserRole;
  gender: Gender;
  dateOfBirth: string;
  address: string;
}

export interface UserDTO {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
  gender: Gender;
  dateOfBirth: string;
  address: string;
  createdDate?: string;
  updatedDate?: string;
}

export interface UserUpdateDTO {
  name?: string;
  phoneNumber?: string;
  gender?: Gender;
  dateOfBirth?: string;
  address?: string;
}

export interface DoctorDTO {
  id: number;
  experience: number;
  availableHours: string;
  specialization: string;
  createdAt?: string;
  updatedAt?: string;
  userId: number;
  name: string;
  email: string;
  phone: string;
}

export interface DoctorRequestDTO {
  experience: number;
  availableHours: string;
  specialization: string;
  userId: number;
}

export interface PatientDTO {
  id: number;
  name: string;
  email: string;
  phone: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PatientRequestDTO {
  userId: number;
}

export interface AppointmentDTO {
  id: number;
  appointmentDateTime: string;
  status: AppointmentStatus;
  notes?: string;
  appointmentCode: string;
  createdAt?: string;
  updatedAt?: string;
  doctorId: number;
  patientId: number;
  timeSlotId?: number;
}

export interface AppointmentRequestDTO {
  appointmentDateTime: string;
  notes?: string;
  doctorId: number;
  patientId: number;
  timeSlotId?: number;
}

export interface DoctorScheduleDTO {
  id: number;
  doctorId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DoctorScheduleRequestDTO {
  doctorId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  isActive?: boolean;
}

export interface TimeSlotDTO {
  id: number;
  doctorId: number;
  slotDate: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
  appointmentId?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface TimeSlotRequestDTO {
  doctorId: number;
  slotDate: string;
  startTime: string;
  endTime: string;
}

export interface NotificationDTO {
  id: number;
  userId: number;
  message: string;
  isRead: boolean;
  timestamp?: string;
}

export interface NotificationRequestDTO {
  userId: number;
  message: string;
}

export interface AuditLogDTO {
  id: number;
  userId?: number;
  action: string;
  details?: string;
  timestamp?: string;
}

export interface AuditLogRequestDTO {
  userId?: number;
  action: string;
  details?: string;
}

export interface SendOtpRequestDTO {
  email: string;
  purpose: OtpPurpose;
}

export interface OtpVerificationRequestDTO {
  email: string;
  otpCode: string;
  purpose: OtpPurpose;
}

export interface OtpTokenDTO {
  id: number;
  email: string;
  purpose: OtpPurpose;
  createdAt?: string;
  expiresAt?: string;
  isUsed: boolean;
  usedAt?: string;
}

export interface ApiError {
  statusCode?: number;
  message?: string;
  error?: string;
  path?: string;
  timestamp?: string;
  errors?: Record<string, string | string[]>;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
  sort?: unknown;
  pageable?: unknown;
}
```

## 7. Validation Rules

Mirror these rules in frontend forms for immediate user feedback. The backend remains the final authority.

### 7.1 Register

Fields:

- `name`: required, non-empty.
- `email`: required, valid email format.
- `password`: required, minimum 8 characters.
- `phoneNumber`: required, non-empty.
- `role`: optional. If omitted, backend behavior decides the default. For patient self-registration, send `ROLE_PATIENT`.
- `gender`: required, one of `MALE`, `FEMALE`, `OTHER`.
- `dateOfBirth`: required, must be in the past, format `YYYY-MM-DD`.
- `address`: required, non-empty.

### 7.2 Login

Fields:

- `email`: required, valid email format.
- `password`: required, non-empty.

### 7.3 User Update

All fields are optional. Send only changed fields:

- `name`
- `phoneNumber`
- `gender`
- `dateOfBirth`
- `address`

### 7.4 Doctor

Fields:

- `experience`: required number.
- `availableHours`: required string, example `09:00-17:00`.
- `specialization`: required string.
- `userId`: required number. The referenced user should have `ROLE_DOCTOR`.

### 7.5 Patient

Fields:

- `userId`: required number. The referenced user should have `ROLE_PATIENT`.

### 7.6 Appointment

Fields:

- `appointmentDateTime`: required future date-time, format `YYYY-MM-DDTHH:mm:ss`.
- `doctorId`: required number.
- `patientId`: required number.
- `timeSlotId`: optional number, strongly recommended when booking from a selected slot.
- `notes`: optional string.

### 7.7 Doctor Schedule

Fields:

- `doctorId`: required number.
- `dayOfWeek`: required uppercase day.
- `startTime`: required `HH:mm:ss`.
- `endTime`: required `HH:mm:ss`, must be after `startTime` in frontend validation.
- `isActive`: optional boolean. Default should be `true` in UI.

### 7.8 Time Slot

Fields:

- `doctorId`: required number.
- `slotDate`: required `YYYY-MM-DD`.
- `startTime`: required `HH:mm:ss`.
- `endTime`: required `HH:mm:ss`, must be after `startTime` in frontend validation.

### 7.9 Notification

Fields:

- `userId`: required number.
- `message`: required non-empty string.

### 7.10 OTP

Send OTP:

- `email`: required, valid email.
- `purpose`: required, `PASSWORD_CHANGE` or `PASSWORD_RESET`.

Verify OTP:

- `email`: required, valid email.
- `otpCode`: required, exactly 6 characters.
- `purpose`: required, must match the send request purpose.

## 8. Endpoint Catalog

### 8.1 Auth

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/auth/register` | Public | `RegisterRequestDTO` | `201 AuthResponseDTO` | Creates user and returns JWT. |
| `POST` | `/api/auth/login` | Public | `AuthRequestDTO` | `200 AuthResponseDTO` | Authenticates existing user. |

Register request example:

```json
{
  "name": "Asha Sharma",
  "email": "asha@example.com",
  "password": "password123",
  "phoneNumber": "9876543210",
  "role": "ROLE_PATIENT",
  "gender": "FEMALE",
  "dateOfBirth": "1995-08-24",
  "address": "Pune"
}
```

Login request example:

```json
{
  "email": "asha@example.com",
  "password": "password123"
}
```

### 8.2 Users

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `GET` | `/api/users` | Required | None | `200 UserDTO[]` | Admin-oriented user list. |
| `GET` | `/api/users/{id}` | Required | None | `200 UserDTO` | Fetch user by id. |
| `GET` | `/api/users/by-email?email={email}` | Required | None | `200 UserDTO` | Useful for profile lookup. |
| `PATCH` | `/api/users/{id}` | Required | `UserUpdateDTO` | `200 UserDTO` | Partial user update. |
| `DELETE` | `/api/users/{id}` | Required | None | `204 No Content` | Admin destructive action. Confirm in UI. |

User update example:

```json
{
  "name": "Asha S.",
  "phoneNumber": "9876543210",
  "gender": "FEMALE",
  "dateOfBirth": "1995-08-24",
  "address": "Pune"
}
```

### 8.3 Doctors

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/doctors` | Required | `DoctorRequestDTO` | `201 DoctorDTO` | Links an existing user to doctor profile. |
| `GET` | `/api/doctors` | Required | None | `200 DoctorDTO[]` | Optional `specialization` query filter. |
| `GET` | `/api/doctors?specialization={value}` | Required | None | `200 DoctorDTO[]` | Filter by specialization. |
| `GET` | `/api/doctors/{id}` | Required | None | `200 DoctorDTO` | Doctor detail. |
| `PUT` | `/api/doctors/{id}` | Required | `DoctorRequestDTO` | `200 DoctorDTO` | Full doctor profile update. |
| `DELETE` | `/api/doctors/{id}` | Required | None | `204 No Content` | Admin destructive action. |

Doctor create/update example:

```json
{
  "experience": 10,
  "availableHours": "09:00-17:00",
  "specialization": "Cardiology",
  "userId": 2
}
```

### 8.4 Patients

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/patients` | Required | `PatientRequestDTO` | `201 PatientDTO` | Links an existing user to patient profile. |
| `GET` | `/api/patients` | Required | None | `200 PatientDTO[]` | Admin-oriented patient list. |
| `GET` | `/api/patients/{id}` | Required | None | `200 PatientDTO` | Patient detail. |
| `GET` | `/api/patients/user/{userId}` | Required | None | `200 PatientDTO` | Resolve logged-in user to patient profile. |
| `DELETE` | `/api/patients/{id}` | Required | None | `204 No Content` | Admin destructive action. |

Patient create example:

```json
{
  "userId": 1
}
```

### 8.5 Appointments

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/appointments` | Required | `AppointmentRequestDTO` | `201 AppointmentDTO` | Book appointment. |
| `GET` | `/api/appointments` | Required | None | `200 AppointmentDTO[]` | Admin/global list. |
| `GET` | `/api/appointments/{id}` | Required | None | `200 AppointmentDTO` | Appointment detail. |
| `GET` | `/api/appointments/code/{appointmentCode}` | Required | None | `200 AppointmentDTO` | Lookup by generated code. |
| `GET` | `/api/appointments/doctor/{doctorId}` | Required | None | `200 AppointmentDTO[]` | Doctor dashboard list. |
| `GET` | `/api/appointments/patient/{patientId}` | Required | None | `200 AppointmentDTO[]` | Patient dashboard list. |
| `GET` | `/api/appointments/status/{status}` | Required | None | `200 PageResponse<AppointmentDTO>` | Supports `page`, `size`, `sort`. |
| `GET` | `/api/appointments/range?startDate={dateTime}&endDate={dateTime}` | Required | None | `200 PageResponse<AppointmentDTO>` | Supports `page`, `size`, `sort`. |
| `PATCH` | `/api/appointments/{id}/status?status={status}` | Required | None | `200 AppointmentDTO` | Update status. |
| `PATCH` | `/api/appointments/{id}/cancel` | Required | None | `200 AppointmentDTO` | Cancel appointment. |
| `DELETE` | `/api/appointments/{id}` | Required | None | `204 No Content` | Admin destructive action. |

Appointment create example:

```json
{
  "appointmentDateTime": "2026-07-01T10:30:00",
  "notes": "First consultation",
  "doctorId": 3,
  "patientId": 5,
  "timeSlotId": 12
}
```

Status update examples:

```http
PATCH /api/appointments/10/status?status=CONFIRMED
PATCH /api/appointments/10/status?status=COMPLETED
PATCH /api/appointments/10/cancel
```

Supported appointment statuses:

- `SCHEDULED`
- `CONFIRMED`
- `COMPLETED`
- `CANCELED`

### 8.6 Doctor Schedules

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/doctor-schedules` | Required | `DoctorScheduleRequestDTO` | `201 DoctorScheduleDTO` | Create recurring doctor availability. |
| `GET` | `/api/doctor-schedules/{id}` | Required | None | `200 DoctorScheduleDTO` | Schedule detail. |
| `GET` | `/api/doctor-schedules/doctor/{doctorId}` | Required | None | `200 DoctorScheduleDTO[]` | Active schedules for doctor. |
| `GET` | `/api/doctor-schedules/doctor/{doctorId}/day/{dayOfWeek}` | Required | None | `200 DoctorScheduleDTO[]` | Active schedules by day. |
| `PUT` | `/api/doctor-schedules/{id}` | Required | `DoctorScheduleRequestDTO` | `200 DoctorScheduleDTO` | Full schedule update. |
| `PATCH` | `/api/doctor-schedules/{id}/active?active={boolean}` | Required | None | `200 DoctorScheduleDTO` | Enable/disable schedule. |
| `DELETE` | `/api/doctor-schedules/{id}` | Required | None | `204 No Content` | Delete schedule. |

Schedule create/update example:

```json
{
  "doctorId": 3,
  "dayOfWeek": "MONDAY",
  "startTime": "09:00:00",
  "endTime": "13:00:00",
  "isActive": true
}
```

### 8.7 Time Slots

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/time-slots` | Required | `TimeSlotRequestDTO` | `201 TimeSlotDTO` | Create a concrete appointment slot. |
| `GET` | `/api/time-slots/{id}` | Required | None | `200 TimeSlotDTO` | Slot detail. |
| `GET` | `/api/time-slots/doctor/{doctorId}/date/{slotDate}` | Required | None | `200 TimeSlotDTO[]` | Optional `status` query. |
| `GET` | `/api/time-slots/doctor/{doctorId}/range?startDate={date}&endDate={date}` | Required | None | `200 TimeSlotDTO[]` | Optional `status` query. |
| `PATCH` | `/api/time-slots/{id}/status?status={status}` | Required | None | `200 TimeSlotDTO` | Update slot status. |
| `DELETE` | `/api/time-slots/{id}` | Required | None | `204 No Content` | Delete slot. |

Time slot create example:

```json
{
  "doctorId": 3,
  "slotDate": "2026-07-01",
  "startTime": "10:00:00",
  "endTime": "10:30:00"
}
```

Availability lookup examples:

```http
GET /api/time-slots/doctor/3/date/2026-07-01?status=AVAILABLE
GET /api/time-slots/doctor/3/range?startDate=2026-07-01&endDate=2026-07-07&status=AVAILABLE
```

Supported slot statuses:

- `AVAILABLE`
- `BOOKED`
- `BLOCKED`

### 8.8 Notifications

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/notifications` | Required | `NotificationRequestDTO` | `201 NotificationDTO` | Create notification. |
| `GET` | `/api/notifications/{id}` | Required | None | `200 NotificationDTO` | Notification detail. |
| `GET` | `/api/notifications/user/{userId}` | Required | None | `200 NotificationDTO[]` | All user notifications. |
| `GET` | `/api/notifications/user/{userId}/unread` | Required | None | `200 NotificationDTO[]` | Unread notifications. |
| `GET` | `/api/notifications/user/{userId}/unread-count` | Required | None | `200 {"count": number}` | Badge count. |
| `PATCH` | `/api/notifications/{id}/read` | Required | None | `200 NotificationDTO` | Mark as read. |
| `DELETE` | `/api/notifications/{id}` | Required | None | `204 No Content` | Delete notification. |

Notification create example:

```json
{
  "userId": 1,
  "message": "Your appointment is confirmed."
}
```

Unread count response:

```json
{
  "count": 3
}
```

### 8.9 Audit Logs

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/audit-logs` | Required | `AuditLogRequestDTO` | `201 AuditLogDTO` | Create audit record. Usually admin/system. |
| `GET` | `/api/audit-logs/{id}` | Required | None | `200 AuditLogDTO` | Audit detail. |
| `GET` | `/api/audit-logs/user/{userId}` | Required | None | `200 PageResponse<AuditLogDTO>` | Supports `page`, `size`, `sort`. |
| `GET` | `/api/audit-logs/range?start={dateTime}&end={dateTime}` | Required | None | `200 AuditLogDTO[]` | Date-time range. |
| `DELETE` | `/api/audit-logs/{id}` | Required | None | `204 No Content` | Admin destructive action. |

Audit log create example:

```json
{
  "userId": 1,
  "action": "PROFILE_UPDATED",
  "details": "User updated phone number"
}
```

### 8.10 OTP

| Method | Endpoint | Auth | Request | Response | Notes |
| --- | --- | --- | --- | --- | --- |
| `POST` | `/api/otp/send` | Public | `SendOtpRequestDTO` | `201 OtpTokenDTO` | Requests OTP generation. |
| `POST` | `/api/otp/verify` | Public | `OtpVerificationRequestDTO` | `200 OtpTokenDTO` | Verifies OTP. |
| `GET` | `/api/otp/latest?email={email}&purpose={purpose}` | Required | None | `200 OtpTokenDTO` | Admin/support lookup. |
| `GET` | `/api/otp?email={email}` | Required | None | `200 OtpTokenDTO[]` | Admin/support lookup. |
| `DELETE` | `/api/otp/expired` | Required | None | `204 No Content` | Maintenance/admin action. |

Send OTP example:

```json
{
  "email": "asha@example.com",
  "purpose": "PASSWORD_RESET"
}
```

Verify OTP example:

```json
{
  "email": "asha@example.com",
  "otpCode": "123456",
  "purpose": "PASSWORD_RESET"
}
```

Important OTP frontend notes:

- The OTP response DTO does not include the OTP code.
- Do not display internal OTP token ids to normal users.
- OTP admin lookup endpoints require authentication.

## 9. Pagination and Sorting

Some endpoints return Spring `Page<T>` responses:

- `GET /api/appointments/status/{status}`
- `GET /api/appointments/range`
- `GET /api/audit-logs/user/{userId}`

Use query params:

```http
?page=0&size=20&sort=createdAt,desc
```

Frontend behavior:

- Treat `page` as zero-based.
- Show user-facing page numbers as one-based.
- Keep `size` selectable: 10, 20, 50.
- Preserve filters and sort in the URL query string for admin pages.

Typical page response:

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "size": 20,
  "number": 0,
  "first": true,
  "last": true,
  "numberOfElements": 0,
  "empty": true
}
```

## 10. Error Handling

The backend can return custom error bodies like:

```json
{
  "statusCode": 400,
  "message": "Email already exists"
}
```

It may also return default Spring validation/problem responses, depending on the failure type. The frontend error parser must handle:

- `message`
- `error`
- field-level validation maps
- generic `status`, `path`, `timestamp`
- empty bodies for some server/proxy failures

Required frontend behavior:

- `400`: Show field or request validation message.
- `401`: Clear auth state and redirect to login.
- `403`: Show access denied.
- `404`: Show not found state.
- `409`: If introduced later, show conflict message and refresh stale data.
- `500`: Show generic server error and keep technical details out of user-facing text.

Recommended API error normalization:

```ts
export function getApiErrorMessage(error: unknown): string {
  if (typeof error === "object" && error && "message" in error) {
    const message = (error as { message?: unknown }).message;
    if (typeof message === "string" && message.trim()) return message;
  }
  return "Something went wrong. Please try again.";
}
```

## 11. Recommended API Client

Create a single API client module responsible for:

- Base URL.
- JSON serialization.
- Bearer token injection.
- Error normalization.
- Logout callback on `401`.
- Typed responses.

Example fetch wrapper:

```ts
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  token?: string
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    throw data ?? { statusCode: response.status, message: response.statusText };
  }

  return data as T;
}
```

## 12. Core Frontend Routes

Recommended route structure:

```text
/login
/register
/forgot-password
/otp/verify

/app
/app/profile
/app/notifications

/app/patient
/app/patient/doctors
/app/patient/doctors/:doctorId
/app/patient/book/:doctorId
/app/patient/appointments
/app/patient/appointments/:appointmentId

/app/doctor
/app/doctor/schedules
/app/doctor/time-slots
/app/doctor/appointments
/app/doctor/appointments/:appointmentId

/app/admin
/app/admin/users
/app/admin/doctors
/app/admin/patients
/app/admin/appointments
/app/admin/audit-logs
/app/admin/notifications
```

Route guard requirements:

- Redirect unauthenticated users from `/app/**` to `/login`.
- Redirect authenticated users away from `/login` and `/register` to their role dashboard.
- Reject role-incompatible pages with an access denied screen.

## 13. Required Screens and Workflows

### 13.1 Public Auth Screens

Login:

- Email/password form.
- Submit to `POST /api/auth/login`.
- Persist token and user on success.
- Route user by role.
- Show backend validation/auth errors.

Register:

- Full user registration form.
- Default role should be `ROLE_PATIENT` for public self-registration.
- Admin-created doctor accounts may use `ROLE_DOCTOR` through an admin flow.
- Submit to `POST /api/auth/register`.
- Persist token and user on success.

Forgot password / OTP:

- Email and purpose selection.
- Send OTP using `POST /api/otp/send`.
- Verify OTP using `POST /api/otp/verify`.
- Since no password-reset completion endpoint is currently documented, frontend should treat OTP verification as a verification step and coordinate with backend before implementing actual password change.

### 13.2 Shared App Shell

The authenticated app should include:

- Role-aware navigation.
- Current user display.
- Logout button.
- Notification unread badge.
- Responsive sidebar or top navigation.
- Main content region with loading, empty, error, and success states.

### 13.3 Profile

Use `AuthResponseDTO.user` as initial profile data.

Allow editing:

- name
- phone number
- gender
- date of birth
- address

Submit via:

```http
PATCH /api/users/{id}
```

### 13.4 Patient Dashboard

Required features:

- Resolve patient profile using `GET /api/patients/user/{userId}`.
- Show upcoming appointments using `GET /api/appointments/patient/{patientId}`.
- Show unread notifications.
- Provide quick action to find doctors.

Booking workflow:

1. List doctors with `GET /api/doctors`.
2. Optionally filter by specialization.
3. Select doctor.
4. Select date range.
5. Load available slots using `GET /api/time-slots/doctor/{doctorId}/date/{slotDate}?status=AVAILABLE`.
6. Pick a slot.
7. Submit appointment with `POST /api/appointments`.
8. Show appointment code from response.

Cancellation:

```http
PATCH /api/appointments/{id}/cancel
```

### 13.5 Doctor Dashboard

Required features:

- Show assigned appointments using `GET /api/appointments/doctor/{doctorId}`.
- Manage recurring schedule using `/api/doctor-schedules`.
- Manage concrete slots using `/api/time-slots`.
- Update appointment status to `CONFIRMED`, `COMPLETED`, or `CANCELED`.

Doctor identity note:

- The auth user contains `user.id`.
- Doctor-specific appointment endpoints require `doctorId`.
- The backend does not currently expose `GET /api/doctors/user/{userId}`. The frontend can derive the doctor record by fetching `GET /api/doctors` and matching `userId`, or the backend can add a direct lookup endpoint later.

### 13.6 Admin Dashboard

Required features:

- User table with create/register guidance, search by email, edit, delete.
- Doctor table with create/update/delete.
- Patient table with create/delete.
- Appointment administration with status/range filters.
- Audit log viewer with user and date filters.
- Notification creation and review.
- Protected operational links for actuator health/info if exposed to admins.

Admin UX requirements:

- Confirm all destructive actions.
- Show entity IDs where needed for support workflows.
- Preserve filters, sort, page, and size in URL query params.
- Avoid exposing OTP internals to non-admin users.

## 14. UI and Component Requirements

Design tone:

- Clean, clinical, trustworthy, and efficient.
- Avoid decorative clutter.
- Prioritize fast scanning of appointments, schedules, and patient/doctor data.

Required reusable components:

- `AppShell`
- `RoleGuard`
- `AuthGuard`
- `ApiErrorAlert`
- `LoadingState`
- `EmptyState`
- `ConfirmDialog`
- `DataTable`
- `PaginationControls`
- `StatusBadge`
- `DatePicker`
- `TimeInput`
- `DateTimeInput`
- `DoctorCard`
- `AppointmentCard`
- `NotificationMenu`
- `ProfileForm`

Status badge colors:

- `SCHEDULED`: neutral/blue.
- `CONFIRMED`: green.
- `COMPLETED`: gray.
- `CANCELED`: red.
- `AVAILABLE`: green.
- `BOOKED`: blue or neutral.
- `BLOCKED`: orange/red.

Tables should support:

- Loading skeletons.
- Empty results.
- Error state with retry.
- Sort where backend supports sorting.
- Mobile-friendly card layout fallback.

Forms should support:

- Client validation.
- Disabled submit while saving.
- Backend error display.
- Unsaved changes warning where data loss is likely.

## 15. State Management

Recommended split:

- Auth/session state: lightweight global store or React context.
- Server state: TanStack Query, RTK Query, SWR, or equivalent.
- Form state: React Hook Form, Formik, or framework equivalent.
- URL state: filters, pagination, selected date range.

Cache invalidation examples:

- After appointment create: invalidate patient appointments, doctor appointments, relevant time slots.
- After appointment cancel/status update: invalidate appointment detail, appointment lists, relevant time slot list.
- After notification mark-read: invalidate notifications and unread count.
- After schedule/slot changes: invalidate schedule/slot queries for that doctor.
- After profile update: update auth user cache and user detail cache.

## 16. Security and Privacy Requirements

Healthcare data is sensitive. The frontend must follow these rules:

- Do not log JWTs, patient details, appointment notes, OTP values, or audit details to console in production.
- Do not include tokens or private details in URLs.
- Clear all auth/session data on logout.
- Mask sensitive fields in admin views when practical.
- Use HTTPS in deployed environments.
- Apply role guards client-side, while assuming backend authorization remains the real enforcement layer.
- Do not store more PHI in browser storage than necessary.
- Avoid third-party analytics capturing form values or URLs containing private identifiers.
- Use autocomplete attributes carefully: allow email/name convenience, avoid storing OTP/password values.

## 17. Accessibility Requirements

Frontend must meet WCAG 2.1 AA expectations:

- Keyboard navigation for all interactive elements.
- Visible focus states.
- Proper labels for all form fields.
- Error messages associated with fields.
- Semantic table markup for tabular data.
- Color must not be the only status indicator.
- Modal dialogs must trap focus and support Escape close.
- Toasts/alerts should be announced through accessible live regions.

## 18. Responsive Requirements

Minimum supported breakpoints:

- Mobile: 360px and up.
- Tablet: 768px and up.
- Desktop: 1024px and up.
- Wide desktop: 1440px and up.

Responsive expectations:

- Navigation collapses cleanly on mobile.
- Tables become cards or horizontally scroll with clear affordances.
- Appointment booking remains usable on small screens.
- Forms use single-column layout on mobile and grouped layout on larger screens.
- Date/time inputs must not overflow on mobile.

## 19. Browser Support

Support current stable versions of:

- Chrome
- Edge
- Firefox
- Safari

Do not rely on experimental browser APIs without fallback.

## 20. Testing Requirements

Unit tests:

- API client success/error parsing.
- Auth state transitions.
- Route guard behavior.
- Form validation.
- Date/time formatting.

Integration tests:

- Login flow.
- Register flow.
- Patient booking flow.
- Doctor status update flow.
- Admin list/filter/pagination flow.
- Notification unread/mark-read flow.

E2E tests:

- Unauthenticated user cannot access `/app`.
- Login redirects to correct dashboard by role.
- Patient can select doctor, available slot, and book appointment.
- Doctor can confirm and complete appointment.
- Admin can filter appointments and audit logs.
- Expired/invalid token redirects to login.

Mocking:

- Use MSW or equivalent API mocking for frontend tests.
- Keep mock DTOs aligned with this document.

## 21. Implementation Checklist

Frontend is complete when:

- All DTO interfaces and enums are implemented.
- API client handles auth, JSON, no-content responses, and normalized errors.
- Public auth pages work.
- Protected role dashboards work.
- Patient can book and cancel appointments.
- Doctor can manage schedules, slots, and appointment statuses.
- Admin can manage core resources.
- Notifications show unread count and can be marked read.
- Pagination works for paged endpoints.
- Date/time values are sent in backend-compatible formats.
- All forms implement frontend validation.
- All destructive actions require confirmation.
- Auth logout clears token and user state.
- App is responsive and accessible.
- Core flows have automated tests.

## 22. Backend Gaps and Coordination Notes

The frontend can be built with the current backend, but these items should be coordinated with backend developers for a polished production system:

- Password reset currently has OTP send/verify endpoints, but no documented endpoint to set a new password after OTP verification.
- Doctor lookup by auth user currently requires fetching doctors and matching `userId`; a direct `GET /api/doctors/user/{userId}` endpoint would simplify doctor dashboards.
- Public doctor search may require backend security changes if unauthenticated visitors should browse doctors before login.
- Validation error response shape may vary between custom backend errors and Spring default validation errors. Frontend should normalize both.
- Role-specific backend authorization may become stricter later. Keep frontend route guards aligned with backend policy.

## 23. Suggested Folder Structure

Example React/Vite structure:

```text
src/
  app/
    router.tsx
    providers.tsx
  api/
    client.ts
    auth.api.ts
    users.api.ts
    doctors.api.ts
    patients.api.ts
    appointments.api.ts
    schedules.api.ts
    time-slots.api.ts
    notifications.api.ts
    audit-logs.api.ts
    otp.api.ts
  auth/
    auth-store.ts
    AuthGuard.tsx
    RoleGuard.tsx
  components/
    AppShell.tsx
    DataTable.tsx
    ConfirmDialog.tsx
    StatusBadge.tsx
  features/
    auth/
    profile/
    patient/
    doctor/
    admin/
    notifications/
  models/
    dto.ts
    enums.ts
  utils/
    dates.ts
    errors.ts
    routes.ts
```

## 24. Definition of Done

A frontend implementation satisfies this requirements document when a user can:

1. Register and log in.
2. Use the app only through role-appropriate protected routes.
3. Update profile information.
4. Browse doctors and book an appointment against an available time slot.
5. View, cancel, and track appointments.
6. Manage doctor schedules and time slots as a doctor.
7. Manage users, doctors, patients, appointments, notifications, and audit logs as an admin.
8. Receive clear loading, empty, success, validation, authorization, and server-error states.
9. Use the application comfortably on mobile and desktop.
10. Run frontend tests that cover core workflows.
