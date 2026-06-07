package com.example.health.mate.dto.response;

import com.example.health.mate.dto.summary.AppointmentSummaryDTO;
import com.example.health.mate.dto.summary.UserSummaryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response for a patient profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientResponseDTO {

    private Long id;
    private UserSummaryDTO user;
    private List<AppointmentSummaryDTO> appointments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
