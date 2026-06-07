package com.example.health.mate.dto.summary;

import com.example.health.mate.domain.STATUS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Compact appointment projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSummaryDTO {

    private Long id;
    private LocalDateTime appointmentDateTime;
    private STATUS status;
    private String appointmentCode;
}
