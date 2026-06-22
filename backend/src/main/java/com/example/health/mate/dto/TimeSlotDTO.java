package com.example.health.mate.dto;

import com.example.health.mate.domain.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {

    private Long id;

    private Long doctorId;

    private LocalDate slotDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private SlotStatus status;

    private Long appointmentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long version;

}