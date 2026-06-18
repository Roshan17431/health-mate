package com.example.health.mate.controller;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.TimeSlotDTO;
import com.example.health.mate.dto.TimeSlotRequestDTO;
import com.example.health.mate.service.TimeSlotService;
import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoints for doctor time slots.
 */
@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    public TimeSlotController(TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @PostMapping
    public ResponseEntity<TimeSlotDTO> createTimeSlot(@Valid @RequestBody TimeSlotRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createTimeSlot(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimeSlotDTO> getTimeSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(timeSlotService.getTimeSlotById(id));
    }

    @GetMapping("/doctor/{doctorId}/date/{slotDate}")
    public ResponseEntity<List<TimeSlotDTO>> getSlotsByDoctorDateAndStatus(
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate,
            @RequestParam(defaultValue = "AVAILABLE") SlotStatus status
    ) {
        return ResponseEntity.ok(timeSlotService.getSlotsByDoctorDateAndStatus(doctorId, slotDate, status));
    }

    @GetMapping("/doctor/{doctorId}/range")
    public ResponseEntity<List<TimeSlotDTO>> getSlotsByDoctorDateRangeAndStatus(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "AVAILABLE") SlotStatus status
    ) {
        return ResponseEntity.ok(
                timeSlotService.getSlotsByDoctorDateRangeAndStatus(doctorId, startDate, endDate, status)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TimeSlotDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam SlotStatus status
    ) {
        return ResponseEntity.ok(timeSlotService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }
}
