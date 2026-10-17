package com.example.health.mate.support;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.User;
import com.example.health.mate.repository.DoctorRepository;
import com.example.health.mate.repository.PatientRepository;
import com.example.health.mate.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Real HTTP fixtures. Privileged identities are seeded only inside disposable test databases. */
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestSmtpInitializer.class)
public abstract class ApiIntegrationSupport {
    protected static final String PASSWORD = "Integration-pass-123!";
    @LocalServerPort protected int port;
    @Autowired protected UserRepository users;
    @Autowired protected DoctorRepository doctors;
    @Autowired protected PatientRepository patients;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected PlatformTransactionManager transactions;
    protected final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    protected User seedUser(USER_ROLE role) {
        return new TransactionTemplate(transactions).execute(status -> {
            String unique = UUID.randomUUID().toString();
            User user = new User();
            user.setName("Integration " + role);
            user.setEmail(unique + "@example.test");
            user.setPhoneNumber(unique);
            user.setPassword(encoder.encode(PASSWORD));
            user.setRole(role);
            user.setGender(GENDER.OTHER);
            user.setDateOfBirth(LocalDate.of(1990, 1, 1));
            user.setAddress("Disposable integration fixture");
            users.save(user);
            if (role == USER_ROLE.ROLE_PATIENT) {
                Patient patient = new Patient(); patient.setUser(user); patients.save(patient);
            } else if (role == USER_ROLE.ROLE_DOCTOR) {
                Doctor doctor = new Doctor(); doctor.setUser(user); doctor.setExperience(5);
                doctor.setSpecialization("Cardiology"); doctor.setAvailableHours("09:00-17:00"); doctors.save(doctor);
            }
            return user;
        });
    }

    protected String token(User user) throws Exception {
        Reply response = request("POST", "/api/auth/login", Map.of("email", user.getEmail(), "password", PASSWORD), null);
        if (response.status() != 200) throw new AssertionError(response.body().toString());
        return response.body().path("token").asText();
    }

    protected Reply request(String method, String path, Object payload, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (payload != null) builder.header("Content-Type", "application/json");
        builder.method(method, payload == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(), response.body().isBlank() ? json.nullNode() : json.readTree(response.body()));
    }

    protected record Reply(int status, JsonNode body) {}
}
