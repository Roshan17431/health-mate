package com.example.health.mate.dto.summary;

import com.example.health.mate.domain.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact OTP token projection that excludes the OTP code.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpTokenSummaryDTO {

    private Long id;
    private String email;
    private OtpPurpose purpose;
    private Boolean isUsed;
}
