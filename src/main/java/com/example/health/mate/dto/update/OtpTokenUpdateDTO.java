package com.example.health.mate.dto.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for marking an OTP token state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpTokenUpdateDTO {

    private Boolean isUsed;
}
