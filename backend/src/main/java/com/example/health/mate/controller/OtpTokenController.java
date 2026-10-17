package com.example.health.mate.controller;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.dto.OtpTokenDTO;
import com.example.health.mate.dto.OtpVerificationRequestDTO;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.service.OtpTokenService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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


@RestController
@RequestMapping("/api/otp")
public class OtpTokenController {

    private final OtpTokenService otpTokenService;

    public OtpTokenController(OtpTokenService otpTokenService) {
        this.otpTokenService = otpTokenService;
    }

    @PostMapping("/send")
    public ResponseEntity<java.util.Map<String, String>> createOtp(@Valid @RequestBody SendOtpRequestDTO request) {
        otpTokenService.createOtp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(java.util.Map.of("message", "Verification email sent. Check your inbox for the code."));
    }

    @PostMapping("/verify")
    public ResponseEntity<OtpTokenDTO> verifyOtp(@Valid @RequestBody OtpVerificationRequestDTO request) {
        return ResponseEntity.ok(otpTokenService.verifyOtp(request));
    }

    @GetMapping("/latest")
    @PreAuthorize("@access.ownEmail(#email)")
    public ResponseEntity<OtpTokenDTO> getLatestOtp(
            @RequestParam String email,
            @RequestParam OtpPurpose purpose
    ) {
        return ResponseEntity.ok(otpTokenService.getLatestOtp(email, purpose));
    }

    @GetMapping
    @PreAuthorize("@access.ownEmail(#email)")
    public ResponseEntity<List<OtpTokenDTO>> getOtpsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(otpTokenService.getOtpsByEmail(email));
    }

    @DeleteMapping("/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteExpiredOtps() {
        otpTokenService.deleteExpiredOtps();
        return ResponseEntity.noContent().build();
    }
}
