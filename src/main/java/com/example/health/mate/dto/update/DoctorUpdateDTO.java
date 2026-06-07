package com.example.health.mate.dto.update;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for partially updating a doctor profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorUpdateDTO {

    @PositiveOrZero(message = "Experience cannot be negative")
    private Integer experience;

    private String availableHours;
    private String specialization;

    @Valid
    private UserUpdateDTO user;
}
