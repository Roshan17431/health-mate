package com.example.health.mate.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact patient projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSummaryDTO {

    private Long id;
    private UserSummaryDTO user;
}
