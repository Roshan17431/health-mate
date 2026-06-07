package com.example.health.mate.dto.response;

import com.example.health.mate.domain.OtpPurpose;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for an OTP token lifecycle record without exposing the OTP code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpTokenResponseDTO {

    private Long id;
    private String email;
    private OtpPurpose purpose;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isUsed;
    private LocalDateTime usedAt;
}
