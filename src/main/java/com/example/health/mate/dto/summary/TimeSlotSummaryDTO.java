package com.example.health.mate.dto.summary;

import com.example.health.mate.domain.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Compact time slot projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotSummaryDTO {

    private Long id;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;
}
