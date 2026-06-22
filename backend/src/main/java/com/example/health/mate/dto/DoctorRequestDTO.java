package com.example.health.mate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data

public class DoctorRequestDTO {

    @NotNull
    private Integer experience;

    @NotBlank
    private String availableHours;

    @NotBlank
    private String specialization;

    @NotNull
    private Long userId;

}