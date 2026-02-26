package com.example.health.mate.entity;

import com.example.health.mate.domain.USER_ROLE;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Entity
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message="Name is required")
    private String name;

    @Column(unique=true,nullable=false)
    @Email(message="Email should be valid")
    @NotBlank(message="Email is required")
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    private String password;

    @Column(unique=true,nullable=false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private USER_ROLE role = USER_ROLE.ROLE_PATIENT;
}

