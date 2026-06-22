package com.example.health.mate.dto;

import lombok.Data;

@Data

public class AuditLogRequestDTO {

    private Long userId;

    private String action;

    private String details;

}
