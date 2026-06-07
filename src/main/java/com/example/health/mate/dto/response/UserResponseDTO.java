package com.example.health.mate.dto.response;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.USER_ROLE;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API response for an application user without sensitive credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String phoneNumber;
    private USER_ROLE role;
    private GENDER gender;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String address;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
