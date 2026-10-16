package com.example.health.mate.service;

import com.example.health.mate.dto.OtpTokenDTO;
import com.example.health.mate.dto.OtpVerificationRequestDTO;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.repository.UserRepository;
import com.example.health.mate.security.PasswordResetTokens;
import com.example.health.mate.security.OtpRequestLimiter;
import com.example.health.mate.domain.OtpPurpose;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Transactional
public class OtpTokenService {

    private static final int OTP_BOUND = 1_000_000;
    private static final int OTP_EXPIRY_MINUTES = 10;

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordResetTokens resetTokens;
    private final OtpRequestLimiter limiter;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpTokenService(OtpTokenRepository otpTokenRepository, UserRepository userRepository,
                           PasswordResetTokens resetTokens, OtpRequestLimiter limiter) {
        this.otpTokenRepository = otpTokenRepository;
        this.userRepository = userRepository;
        this.resetTokens = resetTokens;
        this.limiter = limiter;
    }


    public OtpTokenDTO createOtp(SendOtpRequestDTO request) {
        limiter.checkAccount(request.getEmail(), true);
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(request.getEmail());
        otpToken.setPurpose(request.getPurpose());
        otpToken.setOtpCode(generateOtpCode());
        otpToken.setCreatedAt(LocalDateTime.now());
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setIsUsed(Boolean.FALSE);
        return toDto(otpTokenRepository.save(otpToken));
    }


    public OtpTokenDTO verifyOtp(OtpVerificationRequestDTO request) {
        limiter.checkAccount(request.getEmail(), false);
        OtpToken otpToken = otpTokenRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDescIdDesc(request.getEmail(), request.getPurpose())
                .orElseThrow(() -> new OurException("Invalid or expired OTP"));

        if (Boolean.TRUE.equals(otpToken.getIsUsed()) || !otpToken.getExpiresAt().isAfter(LocalDateTime.now())
                || !otpToken.getOtpCode().equals(request.getOtpCode())) {
            throw new OurException("Invalid or expired OTP");
        }

        otpToken.setIsUsed(Boolean.TRUE);
        otpToken.setUsedAt(LocalDateTime.now());
        OtpTokenDTO result = toDto(otpTokenRepository.save(otpToken));
        if (otpToken.getPurpose() == OtpPurpose.PASSWORD_RESET) {
            result.setResetToken(resetTokens.issue(userRepository.findByEmail(otpToken.getEmail())
                    .orElseThrow(() -> new OurException("Invalid or expired OTP")), otpToken));
        }
        return result;
    }


    @Transactional(readOnly = true)
    public OtpTokenDTO getLatestOtp(String email, com.example.health.mate.domain.OtpPurpose purpose) {
        return toDto(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new OurException("OTP token not found")));
    }


    @Transactional(readOnly = true)
    public List<OtpTokenDTO> getOtpsByEmail(String email) {
        return otpTokenRepository.findByEmail(email).stream().map(this::toDto).toList();
    }


    public void deleteExpiredOtps() {
        otpTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }


    public OtpTokenDTO toDto(OtpToken otpToken) {
        if (otpToken == null) {
            return null;
        }
        return new OtpTokenDTO(
                otpToken.getId(),
                otpToken.getEmail(),
                otpToken.getPurpose(),
                otpToken.getCreatedAt(),
                otpToken.getExpiresAt(),
                otpToken.getIsUsed(),
                otpToken.getUsedAt(),
                null
        );
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
