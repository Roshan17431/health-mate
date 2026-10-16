package com.example.health.mate.controller;

import com.example.health.mate.dto.AuthRequestDTO;
import com.example.health.mate.dto.AuthResponseDTO;
import com.example.health.mate.dto.RegisterRequestDTO;
import com.example.health.mate.service.AuthService;
import com.example.health.mate.service.PasswordResetService;
import com.example.health.mate.dto.PasswordResetRequestDTO;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }


    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated. You can now sign in."));
    }
}
