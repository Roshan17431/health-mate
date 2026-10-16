package com.example.health.mate.dto;

import com.example.health.mate.domain.OtpPurpose;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpTokenDTO {

    private Long id;

    private String email;

    private OtpPurpose purpose;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private Boolean isUsed;

    private LocalDateTime usedAt;

    private String resetToken;

}
