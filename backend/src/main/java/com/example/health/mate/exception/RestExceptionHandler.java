package com.example.health.mate.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(OtpRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleOtpRateLimit(OtpRateLimitException ex) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
                .body(Map.of("statusCode", 429, "message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).findFirst().orElse("Please check your input.");
        return ResponseEntity.badRequest().body(Map.of("statusCode", 400, "message", message));
    }

    // Custom application exceptions
    @ExceptionHandler(OurException.class)
    public ResponseEntity<Map<String, Object>> handleOurException(OurException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("statusCode", 400); // always first
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    // Handles JSON parse errors
    @ExceptionHandler(com.fasterxml.jackson.databind.exc.InvalidFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFormatException(com.fasterxml.jackson.databind.exc.InvalidFormatException ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("statusCode", 400);
        error.put("message", "Invalid value: " + ex.getValue() + ". " + ex.getOriginalMessage());
        return ResponseEntity.badRequest().body(error);
    }

    // Handles all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("statusCode", 500);
        error.put("message", ex.getMessage());
        return ResponseEntity.status(500).body(error);
    }
}
