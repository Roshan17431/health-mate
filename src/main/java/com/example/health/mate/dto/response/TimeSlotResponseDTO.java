package com.example.health.mate.dto.response;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.summary.AppointmentSummaryDTO;
import com.example.health.mate.dto.summary.DoctorSummaryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * API response for a doctor time slot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimeSlotResponseDTO {

    private Long id;
    private DoctorSummaryDTO doctor;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;
    private AppointmentSummaryDTO appointment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
