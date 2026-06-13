package com.example.health.mate.dto;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.USER_ROLE;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    private String name;

    private String email;

    private String phoneNumber;

    private USER_ROLE role;

    private GENDER gender;

    private LocalDate dateOfBirth;

    private String address;

    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

}