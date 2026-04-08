package com.example.health.mate.dto;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.USER_ROLE;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

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
