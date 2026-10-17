package com.example.health.mate.controller;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.dto.AppointmentDTO;
import com.example.health.mate.dto.AppointmentRequestDTO;
import com.example.health.mate.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("@access.patientOwner(#request.patientId)")
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.appointment(#id)")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/code/{appointmentCode}")
    @PreAuthorize("@access.appointmentCode(#appointmentCode)")
    public ResponseEntity<AppointmentDTO> getAppointmentByCode(@PathVariable String appointmentCode) {
        return ResponseEntity.ok(appointmentService.getAppointmentByCode(appointmentCode));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize("@access.doctorOwner(#doctorId)")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("@access.patientOwner(#patientId)")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppointmentDTO>> getAppointmentsByStatus(
            @PathVariable STATUS status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByStatus(status, pageable));
    }

    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppointmentDTO>> getAppointmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDateRange(startDate, endDate, pageable));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@access.appointmentDoctor(#id)")
    public ResponseEntity<AppointmentDTO> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestParam STATUS status
    ) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, status));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("@access.appointment(#id)")
    public ResponseEntity<AppointmentDTO> cancelAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
