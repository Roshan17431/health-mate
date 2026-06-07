package com.example.health.mate.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Compact notification projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryDTO {

    private Long id;
    private String message;
    private Boolean isRead;
    private LocalDateTime timestamp;
}
