package com.example.health.mate.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Compact audit log projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSummaryDTO {

    private Long id;
    private String action;
    private LocalDateTime timestamp;
}
