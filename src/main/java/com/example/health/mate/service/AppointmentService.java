package com.example.health.mate.service;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.AppointmentDTO;
import com.example.health.mate.dto.AppointmentRequestDTO;
import com.example.health.mate.entity.Appointment;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.TimeSlot;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.AppointmentRepository;
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
    private final DoctorService doctorService;
    private final PatientService patientService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            TimeSlotRepository timeSlotRepository,
            DoctorService doctorService,
            PatientService patientService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }


    public AppointmentDTO createAppointment(AppointmentRequestDTO request) {
        Doctor doctor = doctorService.getDoctorEntity(request.getDoctorId());
        Patient patient = patientService.getPatientEntity(request.getPatientId());
        TimeSlot timeSlot = null;

        if (!appointmentRepository
                .findByDoctorIdAndAppointmentDateTime(doctor.getId(), request.getAppointmentDateTime())
                .isEmpty()) {
            throw new OurException("Doctor already has an appointment at this time");
        }

        if (request.getTimeSlotId() != null) {
            timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                    .orElseThrow(() -> new OurException("Time slot not found with id: " + request.getTimeSlotId()));
            if (timeSlot.getStatus() != SlotStatus.AVAILABLE) {
                throw new OurException("Time slot is not available");
            }
            if (!doctor.getId().equals(timeSlot.getDoctor().getId())) {
                throw new OurException("Time slot does not belong to the selected doctor");
            }
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
        Appointment appointment = getAppointmentEntity(id);
        appointment.setStatus(status);
        if (status == STATUS.CANCELED && appointment.getTimeSlot() != null) {
            appointment.getTimeSlot().setStatus(SlotStatus.AVAILABLE);
        }
        return toDto(appointmentRepository.save(appointment));
    }


    public AppointmentDTO cancelAppointment(Long id) {
        return updateStatus(id, STATUS.CANCELED);
    }


    public void deleteAppointment(Long id) {
        Appointment appointment = getAppointmentEntity(id);
        if (appointment.getTimeSlot() != null) {
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
}
