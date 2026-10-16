package com.example.health.mate.security;

import com.example.health.mate.config.JwtProperties;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordResetTokensTests {
    @Test
    void grantExpiresAfterFiveMinutesEvenWhenOtpIsStillValid() {
        Instant now = Instant.now();
        JwtProperties properties = new JwtProperties();
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        User user = new User();
        user.setEmail("reset@example.test");
        user.setPassword("stored-password-hash");
        OtpToken otp = new OtpToken();
        otp.setId(1L);
        otp.setExpiresAt(LocalDateTime.ofInstant(now.plusSeconds(600), ZoneId.systemDefault()));
        PasswordResetTokens issuing = new PasswordResetTokens(properties, Clock.fixed(now, ZoneOffset.UTC));
        String grant = issuing.issue(user, otp);
        assertThat(issuing.verify(grant).getSubject()).isEqualTo(user.getEmail());
        PasswordResetTokens later = new PasswordResetTokens(properties, Clock.fixed(now.plusSeconds(301), ZoneOffset.UTC));
        assertThatThrownBy(() -> later.verify(grant)).isInstanceOf(OurException.class);
    }
}
