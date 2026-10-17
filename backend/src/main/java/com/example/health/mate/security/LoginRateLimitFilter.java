package com.example.health.mate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Runs inside the security chain so ordinary CORS handling applies to throttled responses. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_LOGIN_BODY_BYTES = 16_384;
    private final LoginRequestLimiter limiter;
    private final ObjectMapper json;

    public LoginRateLimitFilter(LoginRequestLimiter limiter, ObjectMapper json) {
        this.limiter = limiter; this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !"/api/auth/login".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] body;
        try {
            limiter.checkIp(request.getRemoteAddr());
            body = request.getInputStream().readNBytes(MAX_LOGIN_BODY_BYTES + 1);
            if (body.length > MAX_LOGIN_BODY_BYTES) {
                response.setStatus(413); response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":413,\"message\":\"Sign-in request is too large.\"}");
                return;
            }
            try {
                var node = json.readTree(body);
                var email = node == null ? null : node.get("email");
                if (email != null && email.isTextual() && !email.asText().isBlank()) limiter.checkAccount(email.asText());
            } catch (JacksonException ignored) {
                // MVC returns its normal validation response; malformed requests still consumed an IP permit.
            }
        } catch (LoginRequestLimiter.LimitExceededException ex) {
            response.setStatus(429); response.setHeader("Retry-After", Long.toString(ex.getRetryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"statusCode\":429,\"message\":\"Too many sign-in attempts. Please try again later.\"}");
            return;
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        private CachedBodyRequest(HttpServletRequest request, byte[] body) { super(request); this.body = body; }
        @Override public ServletInputStream getInputStream() {
            var input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public int read(byte[] buffer, int offset, int length) { return input.read(buffer, offset, length); }
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) {
                    throw new IllegalStateException("Login request body is synchronous");
                }
            };
        }
        @Override public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
