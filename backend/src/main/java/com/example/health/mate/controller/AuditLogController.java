package com.example.health.mate.controller;

import com.example.health.mate.dto.AuditLogDTO;
import com.example.health.mate.dto.AuditLogRequestDTO;
import com.example.health.mate.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAllAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogDTO> getAuditLogById(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getAuditLogById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogsByUser(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsByUser(userId, pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(auditLogService.getAuditLogsBetween(start, end));
    }

}
