package com.example.health.mate.dto.request;

import com.example.health.mate.domain.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for issuing or validating an OTP token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpTokenRequestDTO {

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(regexp = "\\d{6}", message = "OTP code must contain exactly 6 digits")
    private String otpCode;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;
}
