package com.example.health.mate.dto.update;

import com.example.health.mate.domain.SlotStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request payload for partially updating a doctor time slot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotUpdateDTO {

    @FutureOrPresent(message = "Slot date cannot be in the past")
    private LocalDate slotDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private SlotStatus status;

    /**
     * Validates the time range when both values are present.
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
