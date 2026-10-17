package com.example.health.mate.service;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.dto.PasswordResetRequestDTO;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.repository.UserRepository;
import com.example.health.mate.security.PasswordResetTokens;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {
    private final UserRepository users;
    private final OtpTokenRepository otps;
    private final PasswordEncoder encoder;
    private final PasswordResetTokens tokens;
    private final AuditLogService audit;

    public PasswordResetService(UserRepository users, OtpTokenRepository otps,
                                PasswordEncoder encoder, PasswordResetTokens tokens, AuditLogService audit) {
        this.users = users;
        this.otps = otps;
        this.encoder = encoder;
        this.tokens = tokens;
        this.audit = audit;
    }

    @Transactional
    public void resetPassword(PasswordResetRequestDTO request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new OurException("Passwords do not match.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new OurException("Password must be at most 72 bytes.");
        }
        Claims claims = tokens.verify(request.resetToken());
        // Serialize changes for this account: changing the salted hash invalidates every earlier grant.
        User user = users.findByEmailForUpdate(claims.getSubject())
                .orElseThrow(() -> new OurException(PasswordResetTokens.INVALID_TOKEN));
        tokens.verify(request.resetToken()); // Recheck expiry after any wait for the account lock.
        OtpToken otp;
        try {
            otp = otps.findById(Long.valueOf(claims.getId()))
                    .orElseThrow(() -> new OurException(PasswordResetTokens.INVALID_TOKEN));
        } catch (NumberFormatException ex) {
            throw new OurException(PasswordResetTokens.INVALID_TOKEN);
        }
        if (!tokens.matchesPassword(claims, user.getPassword())
                || !user.getEmail().equals(otp.getEmail()) || otp.getPurpose() != OtpPurpose.PASSWORD_RESET
                || !Boolean.TRUE.equals(otp.getIsUsed()) || otp.getUsedAt() == null
                || !otp.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new OurException(PasswordResetTokens.INVALID_TOKEN);
        }
        user.setPassword(encoder.encode(request.newPassword()));
        users.save(user);
        audit.logAction(user.getId(), "PASSWORD_RESET", "Password updated after OTP verification");
    }
}
