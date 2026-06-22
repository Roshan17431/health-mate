package com.example.health.mate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class DoctorDTO {

    private Long id;

    private Integer experience;

    private String availableHours;

    private String specialization;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long userId;

    private String name;

    private String email;

    private String phone;

}