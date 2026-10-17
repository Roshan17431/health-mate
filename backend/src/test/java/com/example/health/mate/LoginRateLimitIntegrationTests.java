package com.example.health.mate;

import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Actual HTTP authentication against a disposable H2 database, without external services. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:login_rate_limit;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "security.login-rate-limit.ip-limit=7",
        "security.login-rate-limit.account-limit=3",
        "security.login-rate-limit.window-ms=600000"
})
class LoginRateLimitIntegrationTests extends ApiIntegrationSupport {
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void blocksRepeatedAccountsAndIpSprayingWithoutBreakingExistingSessions() throws Exception {
        var user = seedUser(USER_ROLE.ROLE_PATIENT);
        var signedIn = login(user.getEmail(), PASSWORD);
        assertThat(signedIn.statusCode()).isEqualTo(200);
        String token = json.readTree(signedIn.body()).path("token").asText();
        assertThat(token).isNotBlank();

        assertThat(login(user.getEmail().toUpperCase(java.util.Locale.ROOT), "Wrong-password-123!").statusCode()).isEqualTo(400);
        assertThat(login(user.getEmail(), "Wrong-password-123!").statusCode()).isEqualTo(400);
        var accountBlocked = login(user.getEmail(), PASSWORD);
        assertThrottled(accountBlocked);
        assertThat(accountBlocked.body()).doesNotContain(user.getEmail(), PASSWORD, "token");

        // Invalid JSON and other accounts still consume the shared IP budget.
        assertThat(post("{").statusCode()).isEqualTo(400);
        assertThat(login("other-one@example.test", "Wrong-password-123!").statusCode()).isEqualTo(400);
        assertThat(login("other-two@example.test", "Wrong-password-123!").statusCode()).isEqualTo(400);
        assertThrottled(login("other-three@example.test", "Wrong-password-123!"));

        assertThat(request("GET", "/api/users/" + user.getId(), null, token).status()).isEqualTo(200);
        assertThat(request("GET", "/api/users", null, null).status()).isEqualTo(401);
    }

    private HttpResponse<String> login(String email, String password) throws Exception {
        return post(json.writeValueAsString(Map.of("email", email, "password", password)));
    }

    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertThrottled(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(json.readTree(response.body()).path("statusCode").asInt()).isEqualTo(429);
        assertThat(Long.parseLong(response.headers().firstValue("Retry-After").orElseThrow())).isBetween(1L, 600L);
    }
}
