package com.example.health.mate.dto;

import com.example.health.mate.domain.STATUS;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {

    private Long id;

    private LocalDateTime appointmentDateTime;

    private STATUS status;

    private String notes;

    private String appointmentCode;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long doctorId;

    private Long patientId;

    private Long timeSlotId;


}
