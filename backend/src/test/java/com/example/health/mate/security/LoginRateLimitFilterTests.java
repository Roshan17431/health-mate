package com.example.health.mate.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimitFilterTests {
    @Test
    void malformedJsonStillReachesMvcUnchangedAndConsumesTheIpBudget() throws Exception {
        var filter = filter();
        var malformed = login("{");
        filter.doFilter(malformed, new MockHttpServletResponse(), (request, response) ->
                assertThat(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{"));
        var blocked = new MockHttpServletResponse();
        filter.doFilter(login("{}"), blocked, (request, response) -> { throw new AssertionError("Throttled request reached MVC"); });
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
    }

    @Test
    void oversizedLoginBodiesAreRejectedBeforeAuthentication() throws Exception {
        var response = new MockHttpServletResponse();
        filter().doFilter(login("x".repeat(16_385)), response,
                (request, result) -> { throw new AssertionError("Oversized request reached authentication"); });
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).doesNotContain("xxxxx");
    }

    @Test
    void unrelatedRoutesDoNotConsumeTheLoginBudget() throws Exception {
        var filter = filter();
        var other = login("{}"); other.setServletPath("/api/auth/register");
        AtomicBoolean otherHandled = new AtomicBoolean(), loginHandled = new AtomicBoolean();
        filter.doFilter(other, new MockHttpServletResponse(), (request, response) -> otherHandled.set(true));
        filter.doFilter(login("{}"), new MockHttpServletResponse(), (request, response) -> loginHandled.set(true));
        assertThat(otherHandled).isTrue(); assertThat(loginHandled).isTrue();
    }

    private LoginRateLimitFilter filter() {
        return new LoginRateLimitFilter(new LoginRequestLimiter(Clock.systemUTC(), 600_000, 1, 1, 20), new ObjectMapper());
    }

    private MockHttpServletRequest login(String body) {
        var request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login"); request.setRemoteAddr("127.0.0.1");
        request.setContentType("application/json"); request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
