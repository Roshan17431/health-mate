package com.example.health.mate.security;

import com.example.health.mate.config.JwtProperties;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

/** Short-lived reset credentials use a separate key so they cannot authenticate API requests. */
@Component
public class PasswordResetTokens {
    public static final String INVALID_TOKEN = "Password reset has expired or is invalid. Request a new code.";
    private final SecretKey signingKey;
    private final Clock clock;

    @Autowired
    public PasswordResetTokens(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    PasswordResetTokens(JwtProperties properties, Clock clock) {
        this.clock = clock;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Decoders.BASE64.decode(properties.getSecret()), "HmacSHA256"));
            signingKey = Keys.hmacShaKeyFor(mac.doFinal("health-mate/password-reset/v1".getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to initialize password reset signing key", ex);
        }
    }

    public String issue(User user, OtpToken otp) {
        Instant now = clock.instant();
        Instant otpExpiry = otp.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        Instant expiry = now.plusSeconds(300).isBefore(otpExpiry) ? now.plusSeconds(300) : otpExpiry;
        return Jwts.builder().subject(user.getEmail()).id(otp.getId().toString())
                .claim("token_use", "password_reset").claim("password_fingerprint", fingerprint(user.getPassword()))
                .issuedAt(Date.from(now)).expiration(Date.from(expiry)).signWith(signingKey).compact();
    }

    public Claims verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload();
            if (!"password_reset".equals(claims.get("token_use", String.class))
                    || claims.getSubject() == null || claims.getId() == null || claims.getExpiration() == null) {
                throw new OurException(INVALID_TOKEN);
            }
            return claims;
        } catch (RuntimeException ex) {
            throw new OurException(INVALID_TOKEN);
        }
    }

    public boolean matchesPassword(Claims claims, String passwordHash) {
        String expected = claims.get("password_fingerprint", String.class);
        return expected != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                fingerprint(passwordHash).getBytes(StandardCharsets.UTF_8));
    }

    private String fingerprint(String passwordHash) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
