package com.example.health.mate.security;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRequestLimiterTests {
    @Test
    void accountCaseAndWhitespaceCannotBypassTheBudgetAndBlockedRequestsDoNotExtendIt() {
        MutableClock clock = new MutableClock();
        LoginRequestLimiter limiter = new LoginRequestLimiter(clock, 600_000, 30, 2, 20);
        limiter.checkAccount(" Person@Example.test ");
        limiter.checkAccount("person@example.test");
        clock.millis.set(599_000);
        var blocked = assertThrows(LoginRequestLimiter.LimitExceededException.class,
                () -> limiter.checkAccount("PERSON@example.test"));
        assertThat(blocked.getRetryAfterSeconds()).isEqualTo(1);
        clock.millis.set(600_000);
        assertThatCode(() -> limiter.checkAccount("person@example.test")).doesNotThrowAnyException();
    }

    @Test
    void anIpCannotSprayUnlimitedAccountsAndOtherIpsKeepTheirOwnBudget() {
        LoginRequestLimiter limiter = new LoginRequestLimiter(new MutableClock(), 600_000, 2, 10, 20);
        limiter.checkIp("127.0.0.1"); limiter.checkAccount("first@example.test");
        limiter.checkIp("127.0.0.1"); limiter.checkAccount("second@example.test");
        assertThatThrownBy(() -> limiter.checkIp("127.0.0.1"))
                .isInstanceOf(LoginRequestLimiter.LimitExceededException.class);
        assertThatCode(() -> limiter.checkIp("127.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void parallelGuessesCannotExceedTheAccountBudget() {
        LoginRequestLimiter limiter = new LoginRequestLimiter(new MutableClock(), 600_000, 30, 3, 20);
        List<CompletableFuture<Boolean>> attempts = IntStream.range(0, 20)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try { limiter.checkAccount("person@example.test"); return true; }
                    catch (LoginRequestLimiter.LimitExceededException ex) { return false; }
                })).toList();
        assertThat(attempts.stream().filter(CompletableFuture::join).count()).isEqualTo(3);
    }

    @Test
    void aFullStoreRejectsNewKeysWithoutEvictingLiveBudgetsAndRecoversAfterExpiry() {
        MutableClock clock = new MutableClock();
        LoginRequestLimiter limiter = new LoginRequestLimiter(clock, 600_000, 2, 2, 2);
        limiter.checkIp("127.0.0.1"); limiter.checkAccount("person@example.test");
        assertThatThrownBy(() -> limiter.checkIp("127.0.0.2"))
                .isInstanceOf(LoginRequestLimiter.LimitExceededException.class);
        limiter.checkAccount("person@example.test");
        assertThatThrownBy(() -> limiter.checkAccount("person@example.test"))
                .isInstanceOf(LoginRequestLimiter.LimitExceededException.class);
        clock.millis.set(600_000);
        assertThatCode(() -> limiter.checkIp("127.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void invalidConfigurationCannotDisableProtection() {
        assertThatThrownBy(() -> new LoginRequestLimiter(new MutableClock(), 0, 30, 10, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoginRequestLimiter(new MutableClock(), 600_000, 0, 10, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoginRequestLimiter(new MutableClock(), 600_000, 30, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoginRequestLimiter(new MutableClock(), 600_000, 30, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static class MutableClock extends Clock {
        final AtomicLong millis = new AtomicLong();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }
}
