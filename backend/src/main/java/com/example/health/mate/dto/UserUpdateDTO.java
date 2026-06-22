package com.example.health.mate.dto;

import com.example.health.mate.domain.GENDER;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateDTO {

    private String name;

    private String phoneNumber;

    private GENDER gender;

    private LocalDate dateOfBirth;

    private String address;

}
