package com.example.health.mate.controller;

import com.example.health.mate.dto.DoctorScheduleDTO;
import com.example.health.mate.dto.DoctorScheduleRequestDTO;
import com.example.health.mate.service.DoctorScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;

/**
 * REST endpoints for recurring doctor schedules.
 */
@RestController
@RequestMapping("/api/doctor-schedules")
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    public DoctorScheduleController(DoctorScheduleService doctorScheduleService) {
        this.doctorScheduleService = doctorScheduleService;
    }

    @PostMapping
    public ResponseEntity<DoctorScheduleDTO> createSchedule(
            @Valid @RequestBody DoctorScheduleRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorScheduleService.createSchedule(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorScheduleDTO> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorScheduleService.getScheduleById(id));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<DoctorScheduleDTO>> getActiveSchedulesByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(doctorScheduleService.getActiveSchedulesByDoctor(doctorId));
    }

    @GetMapping("/doctor/{doctorId}/day/{dayOfWeek}")
    public ResponseEntity<List<DoctorScheduleDTO>> getActiveSchedulesByDoctorAndDay(
            @PathVariable Long doctorId,
            @PathVariable DayOfWeek dayOfWeek
    ) {
        return ResponseEntity.ok(doctorScheduleService.getActiveSchedulesByDoctorAndDay(doctorId, dayOfWeek));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorScheduleDTO> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody DoctorScheduleRequestDTO request
    ) {
        return ResponseEntity.ok(doctorScheduleService.updateSchedule(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<DoctorScheduleDTO> setScheduleActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(doctorScheduleService.setScheduleActive(id, active));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        doctorScheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
