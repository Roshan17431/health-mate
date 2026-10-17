package com.example.health.mate.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("statusCode", status, "message", message));
    }
    @ExceptionHandler(OtpRateLimitException.class)
    public ResponseEntity<Map<String, Object>> rateLimit(OtpRateLimitException ex) {
        return ResponseEntity.status(429).header("Retry-After", Long.toString(ex.getRetryAfterSeconds()))
                .body(Map.of("statusCode", 429, "message", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        return error(400, ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage()).findFirst().orElse("Please check your input."));
    }
    @ExceptionHandler(OurException.class)
    public ResponseEntity<Map<String, Object>> application(OurException ex) { return error(400, ex.getMessage()); }
    @ExceptionHandler(OtpDeliveryException.class)
    public ResponseEntity<Map<String, Object>> delivery(OtpDeliveryException ex) {
        return error(503, "Verification email is temporarily unavailable. Please try again later.");
    }
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException ex) { return error(409, ex.getMessage()); }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<Map<String, Object>> integrity(Exception ex) {
        return error(409, "This change conflicts with existing records. Refresh and try again.");
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return error(403, "You do not have permission for this action.");
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> malformed(Exception ex) { return error(400, "Please check your input."); }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> missing(Exception ex) { return error(404, "Resource not found."); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception ex) {
        log.error("Unexpected request failure", ex);
        return error(500, "Something went wrong. Please try again later.");
    }
}
