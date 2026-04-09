package com.example.health.mate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorDTO {

    private Long id;
    private Integer experience;

    private String availableHours;

    private String specialization;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Nested User details (important!)
    private UserDTO user;

    private List<AppointmentDTO> appointments;
}
