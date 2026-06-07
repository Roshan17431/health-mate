package com.example.health.mate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating an audit log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequestDTO {

    private Long userId;

    @NotBlank(message = "Action is required")
    @Size(max = 500, message = "Action cannot exceed 500 characters")
    private String action;

    @Size(max = 2000, message = "Details cannot exceed 2000 characters")
    private String details;
}
