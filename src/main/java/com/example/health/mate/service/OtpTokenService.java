package com.example.health.mate.service;

import com.example.health.mate.dto.OtpTokenDTO;
import com.example.health.mate.dto.OtpVerificationRequestDTO;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.OtpTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for OTP token creation and verification.
 */
@Service
@Transactional
public class OtpTokenService {

    private static final int OTP_BOUND = 1_000_000;
    private static final int OTP_EXPIRY_MINUTES = 10;

    private final OtpTokenRepository otpTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpTokenService(OtpTokenRepository otpTokenRepository) {
        this.otpTokenRepository = otpTokenRepository;
    }

    /**
     * Creates a new OTP token record.
     *
     * <p>The returned DTO intentionally excludes the generated OTP code. Delivery should be handled by
     * an email or SMS adapter that receives the generated value inside this service boundary.</p>
     */
    public OtpTokenDTO createOtp(SendOtpRequestDTO request) {
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(request.getEmail());
        otpToken.setPurpose(request.getPurpose());
        otpToken.setOtpCode(generateOtpCode());
        otpToken.setCreatedAt(LocalDateTime.now());
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setIsUsed(Boolean.FALSE);
        return toDto(otpTokenRepository.save(otpToken));
    }

    /**
     * Verifies an active OTP token and marks it as used.
     */
    public OtpTokenDTO verifyOtp(OtpVerificationRequestDTO request) {
        OtpToken otpToken = otpTokenRepository
                .findByEmailAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
                        request.getEmail(),
                        request.getOtpCode(),
                        request.getPurpose(),
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new OurException("Invalid or expired OTP"));

        otpToken.setIsUsed(Boolean.TRUE);
        otpToken.setUsedAt(LocalDateTime.now());
        return toDto(otpTokenRepository.save(otpToken));
    }

    /**
     * Returns the latest OTP token for an email and purpose.
     */
    @Transactional(readOnly = true)
    public OtpTokenDTO getLatestOtp(String email, com.example.health.mate.domain.OtpPurpose purpose) {
        return toDto(otpTokenRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> new OurException("OTP token not found")));
    }

    /**
     * Returns every OTP token for an email.
     */
    @Transactional(readOnly = true)
    public List<OtpTokenDTO> getOtpsByEmail(String email) {
        return otpTokenRepository.findByEmail(email).stream().map(this::toDto).toList();
    }

    /**
     * Deletes expired OTP tokens.
     */
    public void deleteExpiredOtps() {
        otpTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * Converts an OTP entity to a DTO without exposing the OTP code.
     */
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
                otpToken.getUsedAt()
        );
    }

    private String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(OTP_BOUND));
    }
}
