package com.example.health.mate.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Compact doctor schedule projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleSummaryDTO {

    private Long id;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;
}
