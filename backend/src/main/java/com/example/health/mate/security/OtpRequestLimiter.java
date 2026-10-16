package com.example.health.mate.security;

import com.example.health.mate.exception.OtpRateLimitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Bounded, per-process recovery quotas. Multiple backend instances need a shared gateway/store. */
@Component
public class OtpRequestLimiter {
    private static final long WINDOW_MS = 600_000;
    private static final int MAX_KEYS = 20_000;
    private final Map<String, Window> windows = new HashMap<>();
    private final Clock clock;

    @Autowired
    public OtpRequestLimiter() { this(Clock.systemUTC()); }
    OtpRequestLimiter(Clock clock) { this.clock = clock; }

    public void checkAccount(String email, boolean sending) {
        consume("account:" + email.trim().toLowerCase(Locale.ROOT) + (sending ? ":send" : ":verify"), 5);
    }

    public void checkIp(String ip, boolean sending) {
        consume("ip:" + ip + (sending ? ":send" : ":verify"), sending ? 20 : 60);
    }

    private synchronized void consume(String key, int limit) {
        long now = clock.millis();
        Window window = windows.get(key);
        if (window == null || now - window.startedAt >= WINDOW_MS) {
            if (window == null && windows.size() >= MAX_KEYS) {
                windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= WINDOW_MS);
                if (windows.size() >= MAX_KEYS) throw new OtpRateLimitException(600);
            }
            window = new Window(now);
            windows.put(key, window);
        }
        if (window.count >= limit) {
            throw new OtpRateLimitException(Math.max(1, (WINDOW_MS - (now - window.startedAt) + 999) / 1000));
        }
        window.count++;
    }

    private static class Window {
        private final long startedAt;
        private int count;
        private Window(long startedAt) { this.startedAt = startedAt; }
    }
}
