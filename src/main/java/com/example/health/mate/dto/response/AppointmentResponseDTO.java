package com.example.health.mate.dto.response;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.dto.summary.DoctorSummaryDTO;
import com.example.health.mate.dto.summary.PatientSummaryDTO;
import com.example.health.mate.dto.summary.TimeSlotSummaryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for an appointment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentResponseDTO {

    private Long id;
    private LocalDateTime appointmentDateTime;
    private STATUS status;
    private String notes;
    private String appointmentCode;
    private DoctorSummaryDTO doctor;
    private PatientSummaryDTO patient;
    private TimeSlotSummaryDTO timeSlot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
