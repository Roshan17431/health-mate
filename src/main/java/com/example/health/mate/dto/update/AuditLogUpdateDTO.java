package com.example.health.mate.dto.update;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for partially updating an audit log entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogUpdateDTO {

    @Size(max = 500, message = "Action cannot exceed 500 characters")
    private String action;

    @Size(max = 2000, message = "Details cannot exceed 2000 characters")
    private String details;
}
