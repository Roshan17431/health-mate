package com.example.health.mate.dto.update;

import com.example.health.mate.domain.STATUS;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for partially updating an appointment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentUpdateDTO {

    @Future(message = "Appointment must be in the future")
    private LocalDateTime appointmentDateTime;

    private STATUS status;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private Long timeSlotId;
}
