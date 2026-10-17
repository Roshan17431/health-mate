package com.example.health.mate.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/** Bounded per-process login quotas. Multiple instances require a shared gateway or store. */
@Component
public class LoginRequestLimiter {
    private final Clock clock;
    private final long windowMs;
    private final int ipLimit;
    private final int accountLimit;
    private final int maxKeys;
    private final Map<String, Window> windows = new HashMap<>();

    @Autowired
    public LoginRequestLimiter(@Value("${security.login-rate-limit.window-ms:600000}") long windowMs,
                               @Value("${security.login-rate-limit.ip-limit:30}") int ipLimit,
                               @Value("${security.login-rate-limit.account-limit:10}") int accountLimit,
                               @Value("${security.login-rate-limit.max-keys:20000}") int maxKeys) {
        this(Clock.systemUTC(), windowMs, ipLimit, accountLimit, maxKeys);
    }

    LoginRequestLimiter(Clock clock, long windowMs, int ipLimit, int accountLimit, int maxKeys) {
        if (windowMs <= 0 || ipLimit <= 0 || accountLimit <= 0 || maxKeys <= 0) {
            throw new IllegalArgumentException("Login rate-limit settings must be positive");
        }
        this.clock = clock; this.windowMs = windowMs; this.ipLimit = ipLimit;
        this.accountLimit = accountLimit; this.maxKeys = maxKeys;
    }

    public void checkIp(String ip) { consume("ip:" + ip, ipLimit); }

    public void checkAccount(String email) {
        // Fixed-size keys bound memory even before MVC validates the submitted email length.
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            consume("account:" + HexFormat.of().formatHex(digest), accountLimit);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    private synchronized void consume(String key, int limit) {
        long now = clock.millis();
        Window window = windows.get(key);
        if (window == null || now - window.startedAt >= windowMs) {
            if (window == null && windows.size() >= maxKeys) {
                windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMs);
                if (windows.size() >= maxKeys) throw new LimitExceededException(seconds(windowMs));
            }
            window = new Window(now);
            windows.put(key, window);
        }
        if (window.count >= limit) {
            throw new LimitExceededException(seconds(windowMs - (now - window.startedAt)));
        }
        window.count++;
    }

    private static long seconds(long millis) { return Math.max(1, (millis - 1) / 1000 + 1); }

    public static final class LimitExceededException extends RuntimeException {
        private final long retryAfterSeconds;
        private LimitExceededException(long retryAfterSeconds) {
            super("Too many sign-in attempts. Please try again later.");
            this.retryAfterSeconds = retryAfterSeconds;
        }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    private static final class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
