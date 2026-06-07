package com.example.health.mate.dto.response;

import com.example.health.mate.dto.summary.UserSummaryDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponseDTO {

    private Long id;
    private UserSummaryDTO user;
    private String message;
    private Boolean isRead;
    private LocalDateTime timestamp;
}
