package com.example.health.mate.dto.update;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for partially updating a notification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationUpdateDTO {

    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    private Boolean isRead;
}
