package com.example.health.mate.dto.update;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for partially updating a patient profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateDTO {

    @Valid
    private UserUpdateDTO user;
}
