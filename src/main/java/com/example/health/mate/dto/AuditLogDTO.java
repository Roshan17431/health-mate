package com.example.health.mate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class AuditLogDTO {

    private Long id;

    private Long userId;

    private String action;

    private String details;

    private LocalDateTime timestamp;

}
