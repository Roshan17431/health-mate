package com.example.health.mate.security;

import com.example.health.mate.exception.OtpRateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpRequestLimiterTests {
    @Test
    void canonicalizesAccountsAndRestoresPermitsWithoutExtendingBlockedWindows() {
        MutableClock clock = new MutableClock();
        OtpRequestLimiter limiter = new OtpRequestLimiter(clock);
        for (int i = 0; i < 5; i++) limiter.checkAccount("Person@example.test", false);
        assertThatThrownBy(() -> limiter.checkAccount(" person@EXAMPLE.test ", false))
                .isInstanceOf(OtpRateLimitException.class);
        clock.millis.set(599_000);
        assertThatThrownBy(() -> limiter.checkAccount("person@example.test", false))
                .isInstanceOf(OtpRateLimitException.class);
        clock.millis.set(600_000);
        assertThatCode(() -> limiter.checkAccount("person@example.test", false)).doesNotThrowAnyException();
    }

    @Test
    void concurrentGuessesCannotExceedTheAccountBudget() {
        OtpRequestLimiter limiter = new OtpRequestLimiter(new MutableClock());
        List<CompletableFuture<Boolean>> requests = IntStream.range(0, 20)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        limiter.checkAccount("person@example.test", false);
                        return true;
                    } catch (OtpRateLimitException ex) { return false; }
                })).toList();
        assertThat(requests.stream().filter(CompletableFuture::join).count()).isEqualTo(5);
    }

    @Test
    void ipBudgetLimitsRequestsAcrossDifferentAccounts() {
        OtpRequestLimiter limiter = new OtpRequestLimiter(new MutableClock());
        for (int i = 0; i < 20; i++) limiter.checkIp("127.0.0.1", true);
        assertThatThrownBy(() -> limiter.checkIp("127.0.0.1", true)).isInstanceOf(OtpRateLimitException.class);
        assertThatCode(() -> limiter.checkIp("127.0.0.2", true)).doesNotThrowAnyException();
    }

    private static class MutableClock extends Clock {
        final AtomicLong millis = new AtomicLong();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }
}
