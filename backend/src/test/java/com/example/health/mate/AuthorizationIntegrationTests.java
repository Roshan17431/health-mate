package com.example.health.mate;

import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.entity.User;
import com.example.health.mate.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:authorization;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class AuthorizationIntegrationTests extends ApiIntegrationSupport {
    private Map<String, Object> registration(String role, String password) {
        String unique = UUID.randomUUID().toString();
        return Map.of("name", "Test User", "email", unique + "@example.test", "password", password,
                "phoneNumber", unique, "role", role, "gender", "OTHER", "dateOfBirth", "1990-01-01", "address", "Test address");
    }
    @Test void publicSignupCannotCreateStaff() throws Exception {
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_DOCTOR"}) {
            var body = registration(role, PASSWORD);
            assertEquals(403, request("POST", "/api/auth/register", body, null).status());
            assertTrue(users.findByEmail(body.get("email").toString()).isEmpty());
        }
        var patient = request("POST", "/api/auth/register", registration("ROLE_PATIENT", PASSWORD), null);
        assertEquals(201, patient.status());
        assertEquals("ROLE_PATIENT", patient.body().path("user").path("role").asText());
    }
    @Test void unauthenticatedAndInvalidSessionsReturn401() throws Exception {
        assertEquals(401, request("GET", "/api/users", null, null).status());
        assertEquals(401, request("GET", "/api/users", null, "invalid.jwt.token").status());
    }
    @Test void privateDirectoriesAreAdminOnly() throws Exception {
        String token = token(seedUser(USER_ROLE.ROLE_PATIENT));
        for (String path : new String[]{"/api/users", "/api/appointments", "/api/patients", "/api/audit-logs"})
            assertEquals(403, request("GET", path, null, token).status(), path);
        assertEquals(200, request("GET", "/api/users", null, token(seedUser(USER_ROLE.ROLE_ADMIN))).status());
    }
    @Test void patientCanOnlyReadAndUpdateOwnAccount() throws Exception {
        User a = seedUser(USER_ROLE.ROLE_PATIENT), b = seedUser(USER_ROLE.ROLE_PATIENT);
        String token = token(a);
        assertEquals(200, request("GET", "/api/users/" + a.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/users/" + b.getId(), null, token).status());
        assertEquals(403, request("PATCH", "/api/users/" + b.getId(), Map.of("name", "Tampered"), token).status());
        assertEquals(403, request("DELETE", "/api/users/" + a.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/patients/user/" + b.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/appointments/patient/" + b.getId(), null, token).status());
    }
    @Test void clinicalAndNotificationMutationsEnforceRole() throws Exception {
        User patient = seedUser(USER_ROLE.ROLE_PATIENT), doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        String token = token(patient);
        assertEquals(403, request("POST", "/api/doctors", Map.of("userId", patient.getId(), "experience", 5, "specialization", "Cardiology", "availableHours", "9-5"), token).status());
        assertEquals(403, request("POST", "/api/notifications", Map.of("userId", doctor.getId(), "message", "Forged"), token).status());
        assertEquals(403, request("GET", "/api/notifications/user/" + doctor.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/otp?email=" + doctor.getEmail(), null, token).status());
        assertEquals(403, request("DELETE", "/api/otp/expired", null, token).status());
        assertEquals(403, request("GET", "/api/doctor-schedules/doctor/" + doctor.getId(), null, token).status());
        assertEquals(403, request("PATCH", "/api/appointments/999999/status?status=COMPLETED", null, token).status());
    }
    @Test void doctorsCannotSeeEachOthersPrivateSchedulesOrAppointments() throws Exception {
        User a = seedUser(USER_ROLE.ROLE_DOCTOR), b = seedUser(USER_ROLE.ROLE_DOCTOR);
        String token = token(a);
        assertEquals(200, request("GET", "/api/appointments/doctor/" + a.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/appointments/doctor/" + b.getId(), null, token).status());
        assertEquals(403, request("GET", "/api/doctor-schedules/doctor/" + b.getId(), null, token).status());
        var patients = request("GET", "/api/patients", null, token);
        assertEquals(200, patients.status());
        assertEquals(0, patients.body().size(), "Doctor without appointments must see no patient directory");
    }
    @Test void staffProvisioningRequiresAdminAndDoesNotIssueStaffSession() throws Exception {
        var staff = registration("ROLE_DOCTOR", PASSWORD);
        assertEquals(403, request("POST", "/api/admin/users", staff, token(seedUser(USER_ROLE.ROLE_PATIENT))).status());
        var created = request("POST", "/api/admin/users", staff, token(seedUser(USER_ROLE.ROLE_ADMIN)));
        assertEquals(201, created.status());
        assertEquals("ROLE_DOCTOR", created.body().path("role").asText());
        assertFalse(created.body().has("token"));
        assertEquals(200, request("POST", "/api/auth/login", Map.of("email", staff.get("email"), "password", PASSWORD), null).status());
    }
    @Test void auditApiCannotForgeOrEraseEventsEvenAsAdmin() throws Exception {
        String token = token(seedUser(USER_ROLE.ROLE_ADMIN));
        assertEquals(403, request("POST", "/api/audit-logs", Map.of("action", "Forged", "details", "Forged"), token).status());
        assertEquals(403, request("DELETE", "/api/audit-logs/1", null, token).status());
    }
    @Test void excessiveUtf8PasswordIsAControlledValidationError() throws Exception {
        var response = request("POST", "/api/auth/register", registration("ROLE_PATIENT", "密".repeat(30)), null);
        assertEquals(400, response.status());
        assertFalse(response.body().toString().contains("BCrypt"));
    }
    @Test void passwordChangeInvalidatesPreviouslyIssuedJwt() throws Exception {
        User user = seedUser(USER_ROLE.ROLE_PATIENT);
        String oldToken = token(user);
        user.setPassword(encoder.encode("Changed-password-123!")); users.save(user);
        assertEquals(401, request("GET", "/api/users/" + user.getId(), null, oldToken).status());
    }
    @Test void canceledPatientCannotInspectNewPatientsSlotBooking() throws Exception {
        User a = seedUser(USER_ROLE.ROLE_PATIENT), b = seedUser(USER_ROLE.ROLE_PATIENT), doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        String ta = token(a), tb = token(b), td = token(doctor);
        String day = java.time.LocalDate.now().plusDays(5).toString();
        var slot = request("POST", "/api/time-slots", Map.of("doctorId", doctor.getId(), "slotDate", day, "startTime", "09:00", "endTime", "09:30"), td);
        assertEquals(201, slot.status());
        long slotId = slot.body().path("id").asLong();
        var first = request("POST", "/api/appointments", Map.of("doctorId", doctor.getId(), "patientId", a.getId(), "timeSlotId", slotId, "appointmentDateTime", day + "T09:00:00"), ta);
        assertEquals(201, first.status());
        assertEquals(200, request("PATCH", "/api/appointments/" + first.body().path("id").asLong() + "/cancel", null, ta).status());
        assertEquals(201, request("POST", "/api/appointments", Map.of("doctorId", doctor.getId(), "patientId", b.getId(), "timeSlotId", slotId, "appointmentDateTime", day + "T09:00:00"), tb).status());
        assertEquals(403, request("GET", "/api/time-slots/" + slotId, null, ta).status());
        var ownSlot = request("GET", "/api/time-slots/" + slotId, null, tb);
        assertEquals(200, ownSlot.status());
        assertTrue(ownSlot.body().path("appointmentId").isNull(), "Patient slot DTOs must not disclose a later booking during authorization races");
        assertEquals(200, request("GET", "/api/time-slots/" + slotId, null, td).status());
    }
    @Test void loginAuditUsesAuthenticatedAccountEvenWhenAnotherBearerIsSubmitted() throws Exception {
        User a = seedUser(USER_ROLE.ROLE_PATIENT), b = seedUser(USER_ROLE.ROLE_PATIENT);
        String ta = token(a);
        assertEquals(200, request("POST", "/api/auth/login", Map.of("email", b.getEmail(), "password", PASSWORD), ta).status());
        var logs = request("GET", "/api/audit-logs", null, token(seedUser(USER_ROLE.ROLE_ADMIN)));
        boolean attributed = false;
        for (var entry : logs.body()) {
            if (entry.path("action").asText().equals("ACCOUNT_LOGIN") && entry.path("userId").asLong() == b.getId()) attributed = true;
        }
        assertTrue(attributed, "Login audit must identify the actual login account");
    }
    @Test void selfDeletionRetainsActorOnDeletionEvent() throws Exception {
        User admin = seedUser(USER_ROLE.ROLE_ADMIN);
        String adminToken = token(admin);
        String observer = token(seedUser(USER_ROLE.ROLE_ADMIN));
        assertEquals(204, request("DELETE", "/api/users/" + admin.getId(), null, adminToken).status());
        boolean found = false;
        for (int attempt = 0; attempt < 20 && !found; attempt++) {
            var logs = request("GET", "/api/audit-logs", null, observer);
            for (var event : logs.body()) {
                if (event.path("action").asText().equals("DELETE /api/users/" + admin.getId())) {
                    assertEquals(admin.getId().longValue(), event.path("userId").asLong()); found = true;
                }
            }
            if (!found) Thread.sleep(25);
        }
        assertTrue(found, "Deletion event must preserve its verified actor after the account is gone");
    }
}
