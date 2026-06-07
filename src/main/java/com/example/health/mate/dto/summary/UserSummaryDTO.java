package com.example.health.mate.dto.summary;

import com.example.health.mate.domain.USER_ROLE;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Compact user projection for nested API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private USER_ROLE role;
}
