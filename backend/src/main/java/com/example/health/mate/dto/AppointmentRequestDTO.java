package com.example.health.mate.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data

public class AppointmentRequestDTO {

    @NotNull
    @Future
    private LocalDateTime appointmentDateTime;

    private String notes;

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    private Long timeSlotId;

}