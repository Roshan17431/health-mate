package com.example.health.mate.controller;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.dto.OtpTokenDTO;
import com.example.health.mate.dto.OtpVerificationRequestDTO;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.service.OtpTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for OTP token creation and verification.
 */
@RestController
@RequestMapping("/api/otp")
public class OtpTokenController {

    private final OtpTokenService otpTokenService;

    public OtpTokenController(OtpTokenService otpTokenService) {
        this.otpTokenService = otpTokenService;
    }

    @PostMapping("/send")
    public ResponseEntity<OtpTokenDTO> createOtp(@Valid @RequestBody SendOtpRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(otpTokenService.createOtp(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<OtpTokenDTO> verifyOtp(@Valid @RequestBody OtpVerificationRequestDTO request) {
        return ResponseEntity.ok(otpTokenService.verifyOtp(request));
    }

    @GetMapping("/latest")
    public ResponseEntity<OtpTokenDTO> getLatestOtp(
            @RequestParam String email,
            @RequestParam OtpPurpose purpose
    ) {
        return ResponseEntity.ok(otpTokenService.getLatestOtp(email, purpose));
    }

    @GetMapping
    public ResponseEntity<List<OtpTokenDTO>> getOtpsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(otpTokenService.getOtpsByEmail(email));
    }

    @DeleteMapping("/expired")
    public ResponseEntity<Void> deleteExpiredOtps() {
        otpTokenService.deleteExpiredOtps();
        return ResponseEntity.noContent().build();
    }
}
