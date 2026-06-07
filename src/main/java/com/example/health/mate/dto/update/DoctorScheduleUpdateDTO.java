package com.example.health.mate.dto.update;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Request payload for partially updating a recurring doctor schedule.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleUpdateDTO {

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;

    /**
     * Validates the time range when both values are present.
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
