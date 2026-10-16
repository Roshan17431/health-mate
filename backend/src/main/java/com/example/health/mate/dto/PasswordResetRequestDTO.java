package com.example.health.mate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequestDTO(
        @NotBlank(message = "Verify your OTP before resetting your password")
        String resetToken,
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String newPassword,
        @NotBlank(message = "Confirm your new password")
        String confirmPassword
) {}
