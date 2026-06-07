package com.example.health.mate.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact doctor projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSummaryDTO {

    private Long id;
    private String specialization;
    private Integer experience;
    private UserSummaryDTO user;
}
