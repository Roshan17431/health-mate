package com.example.health.mate;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises real HTTP, security filters, transactions and password hashing against H2 only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PasswordResetIntegrationTests {
    @LocalServerPort int port;
    @Autowired OtpTokenRepository otps;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String OLD_PASSWORD = "Original-password-123";
    private static final String NEW_PASSWORD = "Updated-password-456";

    @Test
    void resetsPasswordAfterVerificationAndRejectsOtpAndGrantReuse() throws Exception {
        String email = register();
        OtpToken otp = sendOtp(email, "PASSWORD_RESET");
        HttpResponse<String> verified = verify(otp);
        assertThat(verified.statusCode()).isEqualTo(200);
        String grant = body(verified).path("resetToken").asText();
        assertThat(grant).isNotBlank();
        assertThat(otps.findById(otp.getId()).orElseThrow().getIsUsed()).isTrue();
        assertThat(verify(otp).statusCode()).isEqualTo(400);

        assertThat(reset(grant, NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(200);
        assertThat(encoder.matches(NEW_PASSWORD, users.findByEmail(email).orElseThrow().getPassword())).isTrue();
        assertThat(login(email, OLD_PASSWORD).statusCode()).isEqualTo(400);
        assertThat(login(email, NEW_PASSWORD).statusCode()).isEqualTo(200);
        assertThat(reset(grant, "Another-password-789", "Another-password-789").statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsInvalidExpiredAndWrongPurposeOtps() throws Exception {
        String email = register();
        OtpToken otp = sendOtp(email, "PASSWORD_RESET");
        assertThat(post("/api/otp/verify", Map.of("email", email, "otpCode", "not-a-code", "purpose", "PASSWORD_RESET")).statusCode()).isEqualTo(400);
        assertThat(post("/api/otp/verify", Map.of("email", email, "otpCode", otp.getOtpCode(), "purpose", "PASSWORD_CHANGE")).statusCode()).isEqualTo(400);
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otps.save(otp);
        assertThat(verify(otp).statusCode()).isEqualTo(400);
        assertThat(login(email, OLD_PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void passwordChangeOtpDoesNotAuthorizeReset() throws Exception {
        OtpToken otp = sendOtp(register(), "PASSWORD_CHANGE");
        HttpResponse<String> verified = verify(otp);
        assertThat(verified.statusCode()).isEqualTo(200);
        assertThat(body(verified).path("resetToken").asText("")).isEmpty();
    }

    @Test
    void validatesPasswordsWithoutConsumingTheGrant() throws Exception {
        String email = register();
        String grant = grant(sendOtp(email, "PASSWORD_RESET"));
        for (String[] passwords : new String[][] {
                {"short", "short"}, {NEW_PASSWORD, "Does-not-match"}, {"", ""},
                {"é".repeat(40), "é".repeat(40)} }) {
            HttpResponse<String> response = reset(grant, passwords[0], passwords[1]);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
            assertThat(body(response).path("message").asText()).isNotBlank();
        }
        assertThat(login(email, OLD_PASSWORD).statusCode()).isEqualTo(200);
        assertThat(reset(grant, NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void rejectsTamperingAccessTokensAndExpiredAuthorization() throws Exception {
        String email = register();
        OtpToken otp = sendOtp(email, "PASSWORD_RESET");
        String grant = grant(otp);
        assertThat(reset("malformed", NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(400);
        String[] parts = grant.split("\\.");
        parts[1] = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"attacker@example.com\"}".getBytes());
        assertThat(reset(String.join(".", parts), NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(400);
        String accessToken = body(login(email, OLD_PASSWORD)).path("token").asText();
        assertThat(reset(accessToken, NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(400);
        HttpRequest protectedRequest = HttpRequest.newBuilder(URI.create(base() + "/api/users"))
                .header("Authorization", "Bearer " + grant).GET().build();
        assertThat(client.send(protectedRequest, HttpResponse.BodyHandlers.ofString()).statusCode()).isIn(401, 403);
        otp = otps.findById(otp.getId()).orElseThrow();
        otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        otps.save(otp);
        assertThat(reset(grant, NEW_PASSWORD, NEW_PASSWORD).statusCode()).isEqualTo(400);
    }

    @Test
    void concurrentRequestsCannotReuseOtpOrResetGrant() throws Exception {
        String email = register();
        OtpToken otp = sendOtp(email, "PASSWORD_RESET");
        CompletableFuture<HttpResponse<String>> first = CompletableFuture.supplyAsync(() -> uncheckedVerify(otp));
        CompletableFuture<HttpResponse<String>> second = CompletableFuture.supplyAsync(() -> uncheckedVerify(otp));
        HttpResponse<String> a = first.join();
        HttpResponse<String> b = second.join();
        assertThat(new int[] {a.statusCode(), b.statusCode()}).containsExactlyInAnyOrder(200, 400);
        String grant = body(a.statusCode() == 200 ? a : b).path("resetToken").asText();
        first = CompletableFuture.supplyAsync(() -> uncheckedReset(grant));
        second = CompletableFuture.supplyAsync(() -> uncheckedReset(grant));
        assertThat(new int[] {first.join().statusCode(), second.join().statusCode()}).containsExactlyInAnyOrder(200, 400);
    }

    @Test
    void onlyLatestCodeCanAuthorizeAReset() throws Exception {
        String email = register();
        OtpToken earlier = sendOtp(email, "PASSWORD_RESET");
        OtpToken latest = sendOtp(email, "PASSWORD_RESET");
        assertThat(verify(earlier).statusCode()).isEqualTo(400);
        assertThat(verify(latest).statusCode()).isEqualTo(200);
    }

    @Test
    void limitsIssuanceAndGuessesEvenAfterRequestingAnotherCode() throws Exception {
        String email = register();
        OtpToken otp = sendOtp(email, "PASSWORD_RESET");
        String incorrect = otp.getOtpCode().equals("000000") ? "999999" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThat(post("/api/otp/verify", Map.of("email", email, "otpCode", incorrect,
                    "purpose", "PASSWORD_RESET")).statusCode()).isEqualTo(400);
        }
        OtpToken latest = sendOtp(email, "PASSWORD_RESET");
        assertThat(verify(latest).statusCode()).isEqualTo(429);
        for (int i = 0; i < 3; i++) sendOtp(email, "PASSWORD_RESET");
        assertThat(post("/api/otp/send", Map.of("email", email, "purpose", "PASSWORD_RESET")).statusCode()).isEqualTo(429);
        assertThat(login(email, OLD_PASSWORD).statusCode()).isEqualTo(200);
    }

    private String register() throws Exception {
        String id = UUID.randomUUID().toString();
        String email = "reset-" + id + "@example.test";
        HttpResponse<String> registered = post("/api/auth/register", Map.of(
                "name", "Reset Test", "email", email, "password", OLD_PASSWORD,
                "phoneNumber", id, "gender", "OTHER", "dateOfBirth", "1990-01-01", "address", "Test address"));
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        assertThat(body(registered).path("token").asText()).isNotBlank();
        assertThat(login(email, OLD_PASSWORD).statusCode()).isEqualTo(200);
        return email;
    }

    private OtpToken sendOtp(String email, String purpose) throws Exception {
        assertThat(post("/api/otp/send", Map.of("email", email, "purpose", purpose)).statusCode()).isEqualTo(201);
        return otps.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.valueOf(purpose)).orElseThrow();
    }

    private HttpResponse<String> verify(OtpToken otp) throws Exception {
        return post("/api/otp/verify", Map.of("email", otp.getEmail(), "otpCode", otp.getOtpCode(), "purpose", otp.getPurpose().name()));
    }

    private String grant(OtpToken otp) throws Exception {
        HttpResponse<String> response = verify(otp);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        String grant = body(response).path("resetToken").asText();
        assertThat(grant).isNotBlank();
        return grant;
    }

    private HttpResponse<String> reset(String grant, String password, String confirmation) throws Exception {
        return post("/api/auth/password-reset", Map.of("resetToken", grant, "newPassword", password, "confirmPassword", confirmation));
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return post("/api/auth/login", Map.of("email", email, "password", password));
    }

    private HttpResponse<String> post(String path, Object payload) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String base() { return "http://localhost:" + port; }
    private JsonNode body(HttpResponse<String> response) throws Exception { return json.readTree(response.body()); }
    private HttpResponse<String> uncheckedVerify(OtpToken otp) {
        try { return verify(otp); } catch (Exception ex) { throw new RuntimeException(ex); }
    }
    private HttpResponse<String> uncheckedReset(String grant) {
        try { return reset(grant, NEW_PASSWORD, NEW_PASSWORD); } catch (Exception ex) { throw new RuntimeException(ex); }
    }
}
