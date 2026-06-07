package com.example.health.mate.mapper;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.request.AppointmentRequestDTO;
import com.example.health.mate.dto.request.AuditLogRequestDTO;
import com.example.health.mate.dto.request.DoctorRequestDTO;
import com.example.health.mate.dto.request.DoctorScheduleRequestDTO;
import com.example.health.mate.dto.request.NotificationRequestDTO;
import com.example.health.mate.dto.request.OtpTokenRequestDTO;
import com.example.health.mate.dto.request.PatientRequestDTO;
import com.example.health.mate.dto.request.TimeSlotRequestDTO;
import com.example.health.mate.dto.request.UserRequestDTO;
import com.example.health.mate.dto.response.AppointmentResponseDTO;
import com.example.health.mate.dto.response.AuditLogResponseDTO;
import com.example.health.mate.dto.response.DoctorResponseDTO;
import com.example.health.mate.dto.response.DoctorScheduleResponseDTO;
import com.example.health.mate.dto.response.NotificationResponseDTO;
import com.example.health.mate.dto.response.OtpTokenResponseDTO;
import com.example.health.mate.dto.response.PatientResponseDTO;
import com.example.health.mate.dto.response.TimeSlotResponseDTO;
import com.example.health.mate.dto.response.UserResponseDTO;
import com.example.health.mate.dto.summary.AppointmentSummaryDTO;
import com.example.health.mate.dto.summary.AuditLogSummaryDTO;
import com.example.health.mate.dto.summary.DoctorScheduleSummaryDTO;
import com.example.health.mate.dto.summary.DoctorSummaryDTO;
import com.example.health.mate.dto.summary.NotificationSummaryDTO;
import com.example.health.mate.dto.summary.OtpTokenSummaryDTO;
import com.example.health.mate.dto.summary.PatientSummaryDTO;
import com.example.health.mate.dto.summary.TimeSlotSummaryDTO;
import com.example.health.mate.dto.summary.UserSummaryDTO;
import com.example.health.mate.dto.update.AppointmentUpdateDTO;
import com.example.health.mate.dto.update.AuditLogUpdateDTO;
import com.example.health.mate.dto.update.DoctorScheduleUpdateDTO;
import com.example.health.mate.dto.update.DoctorUpdateDTO;
import com.example.health.mate.dto.update.NotificationUpdateDTO;
import com.example.health.mate.dto.update.OtpTokenUpdateDTO;
import com.example.health.mate.dto.update.PatientUpdateDTO;
import com.example.health.mate.dto.update.TimeSlotUpdateDTO;
import com.example.health.mate.dto.update.UserUpdateDTO;
import com.example.health.mate.entity.Appointment;
import com.example.health.mate.entity.AuditLog;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.DoctorSchedule;
import com.example.health.mate.entity.Notification;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.TimeSlot;
import com.example.health.mate.entity.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Central mapper for converting between Health Mate entities and API DTOs.
 */
@Component
public class HealthMateMapper {

    /**
     * Maps a create-user request into a user entity.
     */
    public User toEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setRole(dto.getRole());
        user.setGender(dto.getGender());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setAddress(dto.getAddress());
        return user;
    }

    /**
     * Applies non-null update fields to an existing user entity.
     */
    public void updateEntity(User user, UserUpdateDTO dto) {
        if (user == null || dto == null) {
            return;
        }
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPassword() != null) user.setPassword(dto.getPassword());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
    }

    /**
     * Maps a user entity into a full API response.
     */
    public UserResponseDTO toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getUpdatedDate())
                .build();
    }

    /**
     * Maps a user entity into a compact nested response.
     */
    public UserSummaryDTO toSummary(User user) {
        if (user == null) {
            return null;
        }
        return UserSummaryDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }

    /**
     * Maps a create-patient request into a patient entity.
     */
    public Patient toEntity(PatientRequestDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        Patient patient = new Patient();
        patient.setUser(user != null ? user : toEntity(dto.getUser()));
        return patient;
    }

    /**
     * Applies non-null update fields to an existing patient entity.
     */
    public void updateEntity(Patient patient, PatientUpdateDTO dto) {
        if (patient == null || dto == null) {
            return;
        }
        updateEntity(patient.getUser(), dto.getUser());
    }

    /**
     * Maps a patient entity into a full API response.
     */
    public PatientResponseDTO toResponse(Patient patient) {
        if (patient == null) {
            return null;
        }
        return PatientResponseDTO.builder()
                .id(patient.getId())
                .user(toSummary(patient.getUser()))
                .appointments(toAppointmentSummaries(patient.getAppointments()))
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .build();
    }

    /**
     * Maps a patient entity into a compact nested response.
     */
    public PatientSummaryDTO toSummary(Patient patient) {
        if (patient == null) {
            return null;
        }
        return PatientSummaryDTO.builder()
                .id(patient.getId())
                .user(toSummary(patient.getUser()))
                .build();
    }

    /**
     * Maps a create-doctor request into a doctor entity.
     */
    public Doctor toEntity(DoctorRequestDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        Doctor doctor = new Doctor();
        doctor.setExperience(dto.getExperience());
        doctor.setAvailableHours(dto.getAvailableHours());
        doctor.setSpecialization(dto.getSpecialization());
        doctor.setUser(user != null ? user : toEntity(dto.getUser()));
        return doctor;
    }

    /**
     * Applies non-null update fields to an existing doctor entity.
     */
    public void updateEntity(Doctor doctor, DoctorUpdateDTO dto) {
        if (doctor == null || dto == null) {
            return;
        }
        if (dto.getExperience() != null) doctor.setExperience(dto.getExperience());
        if (dto.getAvailableHours() != null) doctor.setAvailableHours(dto.getAvailableHours());
        if (dto.getSpecialization() != null) doctor.setSpecialization(dto.getSpecialization());
        updateEntity(doctor.getUser(), dto.getUser());
    }

    /**
     * Maps a doctor entity into a full API response.
     */
    public DoctorResponseDTO toResponse(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        return DoctorResponseDTO.builder()
                .id(doctor.getId())
                .experience(doctor.getExperience())
                .availableHours(doctor.getAvailableHours())
                .specialization(doctor.getSpecialization())
                .user(toSummary(doctor.getUser()))
                .appointments(toAppointmentSummaries(doctor.getAppointments()))
                .createdAt(doctor.getCreatedAt())
                .updatedAt(doctor.getUpdatedAt())
                .build();
    }

    /**
     * Maps a doctor entity into a compact nested response.
     */
    public DoctorSummaryDTO toSummary(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        return DoctorSummaryDTO.builder()
                .id(doctor.getId())
                .specialization(doctor.getSpecialization())
                .experience(doctor.getExperience())
                .user(toSummary(doctor.getUser()))
                .build();
    }

    /**
     * Maps a create-appointment request into an appointment entity.
     */
    public Appointment toEntity(AppointmentRequestDTO dto, Doctor doctor, Patient patient, TimeSlot timeSlot) {
        if (dto == null) {
            return null;
        }
        Appointment appointment = new Appointment();
        appointment.setAppointmentDateTime(dto.getAppointmentDateTime());
        appointment.setNotes(dto.getNotes());
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setTimeSlot(timeSlot);
        return appointment;
    }

    /**
     * Applies non-null update fields to an existing appointment entity.
     */
    public void updateEntity(Appointment appointment, AppointmentUpdateDTO dto, TimeSlot timeSlot) {
        if (appointment == null || dto == null) {
            return;
        }
        if (dto.getAppointmentDateTime() != null) appointment.setAppointmentDateTime(dto.getAppointmentDateTime());
        if (dto.getStatus() != null) appointment.setStatus(dto.getStatus());
        if (dto.getNotes() != null) appointment.setNotes(dto.getNotes());
        if (timeSlot != null) appointment.setTimeSlot(timeSlot);
    }

    /**
     * Maps an appointment entity into a full API response.
     */
    public AppointmentResponseDTO toResponse(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .appointmentCode(appointment.getAppointmentCode())
                .doctor(toSummary(appointment.getDoctor()))
                .patient(toSummary(appointment.getPatient()))
                .timeSlot(toSummary(appointment.getTimeSlot()))
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    /**
     * Maps an appointment entity into a compact nested response.
     */
    public AppointmentSummaryDTO toSummary(Appointment appointment) {
        if (appointment == null) {
            return null;
        }
        return AppointmentSummaryDTO.builder()
                .id(appointment.getId())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .status(appointment.getStatus())
                .appointmentCode(appointment.getAppointmentCode())
                .build();
    }

    /**
     * Maps a create-schedule request into a doctor schedule entity.
     */
    public DoctorSchedule toEntity(DoctorScheduleRequestDTO dto, Doctor doctor) {
        if (dto == null) {
            return null;
        }
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setIsActive(dto.getIsActive() == null ? Boolean.TRUE : dto.getIsActive());
        return schedule;
    }

    /**
     * Applies non-null update fields to an existing doctor schedule entity.
     */
    public void updateEntity(DoctorSchedule schedule, DoctorScheduleUpdateDTO dto) {
        if (schedule == null || dto == null) {
            return;
        }
        if (dto.getDayOfWeek() != null) schedule.setDayOfWeek(dto.getDayOfWeek());
        if (dto.getStartTime() != null) schedule.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) schedule.setEndTime(dto.getEndTime());
        if (dto.getIsActive() != null) schedule.setIsActive(dto.getIsActive());
    }

    /**
     * Maps a doctor schedule entity into a full API response.
     */
    public DoctorScheduleResponseDTO toResponse(DoctorSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return DoctorScheduleResponseDTO.builder()
                .id(schedule.getId())
                .doctor(toSummary(schedule.getDoctor()))
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .isActive(schedule.getIsActive())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }

    /**
     * Maps a doctor schedule entity into a compact nested response.
     */
    public DoctorScheduleSummaryDTO toSummary(DoctorSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return DoctorScheduleSummaryDTO.builder()
                .id(schedule.getId())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .isActive(schedule.getIsActive())
                .build();
    }

    /**
     * Maps a create-time-slot request into a time slot entity.
     */
    public TimeSlot toEntity(TimeSlotRequestDTO dto, Doctor doctor) {
        if (dto == null) {
            return null;
        }
        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDoctor(doctor);
        timeSlot.setSlotDate(dto.getSlotDate());
        timeSlot.setStartTime(dto.getStartTime());
        timeSlot.setEndTime(dto.getEndTime());
        timeSlot.setStatus(dto.getStatus() == null ? SlotStatus.AVAILABLE : dto.getStatus());
        return timeSlot;
    }

    /**
     * Applies non-null update fields to an existing time slot entity.
     */
    public void updateEntity(TimeSlot timeSlot, TimeSlotUpdateDTO dto) {
        if (timeSlot == null || dto == null) {
            return;
        }
        if (dto.getSlotDate() != null) timeSlot.setSlotDate(dto.getSlotDate());
        if (dto.getStartTime() != null) timeSlot.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) timeSlot.setEndTime(dto.getEndTime());
        if (dto.getStatus() != null) timeSlot.setStatus(dto.getStatus());
    }

    /**
     * Maps a time slot entity into a full API response.
     */
    public TimeSlotResponseDTO toResponse(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        return TimeSlotResponseDTO.builder()
                .id(timeSlot.getId())
                .doctor(toSummary(timeSlot.getDoctor()))
                .slotDate(timeSlot.getSlotDate())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .status(timeSlot.getStatus())
                .appointment(toSummary(timeSlot.getAppointment()))
                .createdAt(timeSlot.getCreatedAt())
                .updatedAt(timeSlot.getUpdatedAt())
                .version(timeSlot.getVersion())
                .build();
    }

    /**
     * Maps a time slot entity into a compact nested response.
     */
    public TimeSlotSummaryDTO toSummary(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        return TimeSlotSummaryDTO.builder()
                .id(timeSlot.getId())
                .slotDate(timeSlot.getSlotDate())
                .startTime(timeSlot.getStartTime())
                .endTime(timeSlot.getEndTime())
                .status(timeSlot.getStatus())
                .build();
    }

    /**
     * Maps a create-notification request into a notification entity.
     */
    public Notification toEntity(NotificationRequestDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(dto.getMessage());
        notification.setIsRead(Boolean.FALSE);
        return notification;
    }

    /**
     * Applies non-null update fields to an existing notification entity.
     */
    public void updateEntity(Notification notification, NotificationUpdateDTO dto) {
        if (notification == null || dto == null) {
            return;
        }
        if (dto.getMessage() != null) notification.setMessage(dto.getMessage());
        if (dto.getIsRead() != null) notification.setIsRead(dto.getIsRead());
    }

    /**
     * Maps a notification entity into a full API response.
     */
    public NotificationResponseDTO toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .user(toSummary(notification.getUser()))
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .timestamp(notification.getTimestamp())
                .build();
    }

    /**
     * Maps a notification entity into a compact nested response.
     */
    public NotificationSummaryDTO toSummary(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationSummaryDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .timestamp(notification.getTimestamp())
                .build();
    }

    /**
     * Maps a create-audit-log request into an audit log entity.
     */
    public AuditLog toEntity(AuditLogRequestDTO dto, User user) {
        if (dto == null) {
            return null;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(dto.getAction());
        auditLog.setDetails(dto.getDetails());
        return auditLog;
    }

    /**
     * Applies non-null update fields to an existing audit log entity.
     */
    public void updateEntity(AuditLog auditLog, AuditLogUpdateDTO dto) {
        if (auditLog == null || dto == null) {
            return;
        }
        if (dto.getAction() != null) auditLog.setAction(dto.getAction());
        if (dto.getDetails() != null) auditLog.setDetails(dto.getDetails());
    }

    /**
     * Maps an audit log entity into a full API response.
     */
    public AuditLogResponseDTO toResponse(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return AuditLogResponseDTO.builder()
                .id(auditLog.getId())
                .user(toSummary(auditLog.getUser()))
                .action(auditLog.getAction())
                .details(auditLog.getDetails())
                .timestamp(auditLog.getTimestamp())
                .build();
    }

    /**
     * Maps an audit log entity into a compact nested response.
     */
    public AuditLogSummaryDTO toSummary(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return AuditLogSummaryDTO.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .timestamp(auditLog.getTimestamp())
                .build();
    }

    /**
     * Maps an OTP request into an OTP token entity.
     */
    public OtpToken toEntity(OtpTokenRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(dto.getEmail());
        otpToken.setOtpCode(dto.getOtpCode());
        otpToken.setPurpose(dto.getPurpose());
        return otpToken;
    }

    /**
     * Applies non-null update fields to an existing OTP token entity.
     */
    public void updateEntity(OtpToken otpToken, OtpTokenUpdateDTO dto) {
        if (otpToken == null || dto == null) {
            return;
        }
        if (dto.getIsUsed() != null) otpToken.setIsUsed(dto.getIsUsed());
    }

    /**
     * Maps an OTP token entity into a full API response without the OTP code.
     */
    public OtpTokenResponseDTO toResponse(OtpToken otpToken) {
        if (otpToken == null) {
            return null;
        }
        return OtpTokenResponseDTO.builder()
                .id(otpToken.getId())
                .email(otpToken.getEmail())
                .purpose(otpToken.getPurpose())
                .createdAt(otpToken.getCreatedAt())
                .expiresAt(otpToken.getExpiresAt())
                .isUsed(otpToken.getIsUsed())
                .usedAt(otpToken.getUsedAt())
                .build();
    }

    /**
     * Maps an OTP token entity into a compact response without the OTP code.
     */
    public OtpTokenSummaryDTO toSummary(OtpToken otpToken) {
        if (otpToken == null) {
            return null;
        }
        return OtpTokenSummaryDTO.builder()
                .id(otpToken.getId())
                .email(otpToken.getEmail())
                .purpose(otpToken.getPurpose())
                .isUsed(otpToken.getIsUsed())
                .build();
    }

    private List<AppointmentSummaryDTO> toAppointmentSummaries(List<Appointment> appointments) {
        if (appointments == null) {
            return Collections.emptyList();
        }
        return appointments.stream().map(this::toSummary).toList();
    }
}
