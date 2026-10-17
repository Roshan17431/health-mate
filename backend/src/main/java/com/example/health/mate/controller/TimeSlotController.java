package com.example.health.mate.controller;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.TimeSlotDTO;
import com.example.health.mate.dto.TimeSlotRequestDTO;
import com.example.health.mate.service.TimeSlotService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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


@RestController
@RequestMapping("/api/time-slots")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    private final com.example.health.mate.security.AccessRules access;

    public TimeSlotController(TimeSlotService timeSlotService, com.example.health.mate.security.AccessRules access) {
        this.timeSlotService = timeSlotService;
        this.access = access;
    }

    @PostMapping
    @PreAuthorize("@access.doctorOwner(#request.doctorId)")
    public ResponseEntity<TimeSlotDTO> createTimeSlot(@Valid @RequestBody TimeSlotRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeSlotService.createTimeSlot(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.slot(#id)")
    public ResponseEntity<TimeSlotDTO> getTimeSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(present(timeSlotService.getTimeSlotById(id)));
    }

    @GetMapping("/doctor/{doctorId}/date/{slotDate}")
    @PreAuthorize("@access.slotDirectory(#doctorId, #status)")
    public ResponseEntity<List<TimeSlotDTO>> getSlotsByDoctorDateAndStatus(
            @PathVariable Long doctorId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate slotDate,
            @RequestParam(defaultValue = "AVAILABLE") SlotStatus status
    ) {
        return ResponseEntity.ok(timeSlotService.getSlotsByDoctorDateAndStatus(doctorId, slotDate, status).stream().map(this::present).toList());
    }

    @GetMapping("/doctor/{doctorId}/range")
    @PreAuthorize("@access.slotDirectory(#doctorId, #status)")
    public ResponseEntity<List<TimeSlotDTO>> getSlotsByDoctorDateRangeAndStatus(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "AVAILABLE") SlotStatus status
    ) {
        return ResponseEntity.ok(
                timeSlotService.getSlotsByDoctorDateRangeAndStatus(doctorId, startDate, endDate, status).stream().map(this::present).toList()
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@access.slotOwner(#id)")
    public ResponseEntity<TimeSlotDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam SlotStatus status
    ) {
        return ResponseEntity.ok(timeSlotService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@access.slotOwner(#id)")
    public ResponseEntity<Void> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }
    private TimeSlotDTO present(TimeSlotDTO slot) {
        // Availability checks and DTO reads can straddle a concurrent booking.
        // Patients use their own appointment endpoint for private booking identifiers.
        if (!access.doctorOwner(slot.getDoctorId())) slot.setAppointmentId(null);
        return slot;
    }
}
