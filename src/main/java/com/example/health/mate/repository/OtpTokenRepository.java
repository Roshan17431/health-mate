package com.example.health.mate.repository;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.entity.OtpToken;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpTokenRepository extends CrudRepository<OtpToken,Long> {

    Optional<OtpToken> findTopByEmailAndPurposeOrderByCreatedAtDesc(
            String email,
            OtpPurpose purpose
    );

    // Get valid OTP (not used and not expired)
    Optional<OtpToken> findByEmailAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
            String email,
            String otpCode,
            OtpPurpose purpose,
            LocalDateTime currentTime
    );

    //  Get all OTPs for email (optional use)
    List<OtpToken> findByEmail(String email);

    //  Delete expired OTPs
    void deleteByExpiresAtBefore(LocalDateTime time);

    //  Mark OTP as used (optional helper via fetch + save)
    Optional<OtpToken> findByEmailAndPurposeAndIsUsedFalse(
            String email,
            OtpPurpose purpose
    );
}
