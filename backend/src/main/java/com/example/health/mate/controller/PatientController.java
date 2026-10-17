package com.example.health.mate.controller;

import com.example.health.mate.dto.PatientDTO;
import com.example.health.mate.dto.PatientRequestDTO;
import com.example.health.mate.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final com.example.health.mate.security.AccessRules access;

    public PatientController(PatientService patientService, com.example.health.mate.security.AccessRules access) {
        this.patientService = patientService;
        this.access = access;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientDTO> createPatient(@Valid @RequestBody PatientRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<List<PatientDTO>> getAllPatients() {
        return ResponseEntity.ok(access.patientDirectory(patientService.getAllPatients()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.patient(#id)")
    public ResponseEntity<PatientDTO> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@access.patient(#userId)")
    public ResponseEntity<PatientDTO> getPatientByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(patientService.getPatientByUserId(userId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
