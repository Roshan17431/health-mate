package com.example.health.mate.dto.update;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.USER_ROLE;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for partially updating an application user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {

    private String name;

    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 8, message = "Password must contain at least 8 characters")
    private String password;

    private String phoneNumber;
    private USER_ROLE role;
    private GENDER gender;

    @Past(message = "Invalid DOB")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String address;
}
