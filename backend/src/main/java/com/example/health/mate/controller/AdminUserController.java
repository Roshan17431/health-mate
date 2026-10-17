package com.example.health.mate.controller;

import com.example.health.mate.dto.RegisterRequestDTO;
import com.example.health.mate.dto.UserDTO;
import com.example.health.mate.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AuthService auth;
    public AdminUserController(AuthService auth) { this.auth = auth; }
    @PostMapping
    public ResponseEntity<UserDTO> createStaff(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(201).body(auth.createStaff(request));
    }
}
