package com.example.health.mate.dto;

import com.example.health.mate.domain.OtpPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendOtpRequestDTO {

    @Email
    @NotBlank
    private String email;

    @NotNull
    private OtpPurpose purpose;

}