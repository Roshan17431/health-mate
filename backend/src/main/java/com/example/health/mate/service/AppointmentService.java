package com.example.health.mate.service;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.AppointmentDTO;
import com.example.health.mate.dto.AppointmentRequestDTO;
import com.example.health.mate.entity.Appointment;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.TimeSlot;
import com.example.health.mate.exception.ConflictException;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.AppointmentRepository;
import com.example.health.mate.repository.DoctorRepository;
import com.example.health.mate.repository.PatientRepository;
import com.example.health.mate.repository.TimeSlotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }


    public AppointmentDTO createAppointment(AppointmentRequestDTO request) {
        // Every booking mutation locks the doctor before reading appointment or slot state.
        Doctor doctor = lockDoctor(request.getDoctorId());
        Patient patient = patientRepository.findByIdForUpdate(request.getPatientId())
                .orElseThrow(() -> new OurException("Patient not found with id: " + request.getPatientId()));
        TimeSlot timeSlot = null;

        ensureDoctorAvailable(doctor.getId(), request.getAppointmentDateTime(), null);

        if (request.getTimeSlotId() != null) {
            timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                    .orElseThrow(() -> new OurException("Time slot not found with id: " + request.getTimeSlotId()));
            if (!doctor.getId().equals(timeSlot.getDoctor().getId())) {
                throw new OurException("Time slot does not belong to the selected doctor");
            }
            if (!request.getAppointmentDateTime().equals(timeSlot.getSlotDate().atTime(timeSlot.getStartTime()))) {
                throw new OurException("Appointment date and time must match the selected time slot");
            }
            ensureSlotAvailable(timeSlot, null);
            timeSlot.setStatus(SlotStatus.BOOKED);
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setNotes(request.getNotes() == null ? "No notes" : request.getNotes());
        appointment.setStatus(STATUS.SCHEDULED);
        appointment.setAppointmentCode(generateAppointmentCode());
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setTimeSlot(timeSlot);
        return toDto(appointmentRepository.save(appointment));
    }


    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public AppointmentDTO getAppointmentById(Long id) {
        return toDto(getAppointmentEntity(id));
    }


    @Transactional(readOnly = true)
    public AppointmentDTO getAppointmentByCode(String appointmentCode) {
        return toDto(appointmentRepository.findByAppointmentCode(appointmentCode)
                .orElseThrow(() -> new OurException("Appointment not found with code: " + appointmentCode)));
    }


    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByDoctor(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByPatient(Long patientId) {
        return appointmentRepository.findByPatientId(patientId).stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getAppointmentsByStatus(STATUS status, Pageable pageable) {
        return appointmentRepository.findByStatus(status, pageable).map(this::toDto);
    }


    @Transactional(readOnly = true)
    public Page<AppointmentDTO> getAppointmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return appointmentRepository.findByDateRange(startDate, endDate, pageable).map(this::toDto);
    }


    public AppointmentDTO updateStatus(Long id, STATUS status) {
        Appointment appointment = lockAppointment(id);
        if (status == null) {
            throw new OurException("Appointment status is required");
        }
        if (appointment.getStatus() == STATUS.CANCELED && status != STATUS.CANCELED) {
            ensureDoctorAvailable(appointment.getDoctor().getId(), appointment.getAppointmentDateTime(), id);
            if (appointment.getTimeSlot() != null) {
                ensureSlotAvailable(appointment.getTimeSlot(), id);
                appointment.getTimeSlot().setStatus(SlotStatus.BOOKED);
            }
        } else if (appointment.getStatus() != STATUS.CANCELED && status == STATUS.CANCELED
                && appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(SlotStatus.AVAILABLE);
        }
        appointment.setStatus(status);
        return toDto(appointmentRepository.save(appointment));
    }


    public AppointmentDTO cancelAppointment(Long id) {
        return updateStatus(id, STATUS.CANCELED);
    }


    public void deleteAppointment(Long id) {
        Appointment appointment = lockAppointment(id);
        if (appointment.getStatus() != STATUS.CANCELED && appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(SlotStatus.AVAILABLE);
        }
        appointmentRepository.delete(appointment);
    }


    @Transactional(readOnly = true)
    public Appointment getAppointmentEntity(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new OurException("Appointment not found with id: " + id));
    }


    public AppointmentDTO toDto(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getAppointmentDateTime(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getAppointmentCode(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt(),
                appointment.getDoctor() != null ? appointment.getDoctor().getId() : null,
                appointment.getPatient() != null ? appointment.getPatient().getId() : null,
                appointment.getTimeSlot() != null ? appointment.getTimeSlot().getId() : null
        );
    }

    private String generateAppointmentCode() {
        return "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Doctor lockDoctor(Long id) {
        return doctorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new OurException("Doctor not found with id: " + id));
    }

    private Appointment lockAppointment(Long id) {
        Long doctorId = appointmentRepository.findDoctorIdById(id)
                .orElseThrow(() -> new OurException("Appointment not found with id: " + id));
        lockDoctor(doctorId);
        return getAppointmentEntity(id);
    }

    private void ensureDoctorAvailable(Long doctorId, LocalDateTime time, Long excludedId) {
        if (appointmentRepository.hasConflictingAppointment(doctorId, time, STATUS.CANCELED, excludedId)) {
            throw new ConflictException("Doctor already has an appointment at this time");
        }
    }

    private void ensureSlotAvailable(TimeSlot timeSlot, Long excludedId) {
        if (timeSlot.getStatus() != SlotStatus.AVAILABLE
                || appointmentRepository.hasConflictingSlotAppointment(timeSlot.getId(), STATUS.CANCELED, excludedId)) {
            throw new ConflictException("Time slot is not available");
        }
    }
}
