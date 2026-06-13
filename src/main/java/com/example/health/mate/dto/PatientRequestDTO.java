package com.example.health.mate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientRequestDTO {

    @NotNull(message = "User ID is required")

    private Long userId;

}
