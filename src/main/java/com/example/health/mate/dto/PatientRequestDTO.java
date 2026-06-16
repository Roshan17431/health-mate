package com.example.health.mate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    /**
     * Returns the user id linked to the patient profile.
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user id linked to the patient profile.
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
