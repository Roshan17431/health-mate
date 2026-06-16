package com.example.health.mate.service;

import com.example.health.mate.dto.DoctorScheduleDTO;
import com.example.health.mate.dto.DoctorScheduleRequestDTO;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.DoctorSchedule;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.DoctorScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Service for doctor recurring schedule management.
 */
@Service
@Transactional
public class DoctorScheduleService {

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorService doctorService;

    public DoctorScheduleService(DoctorScheduleRepository doctorScheduleRepository, DoctorService doctorService) {
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.doctorService = doctorService;
    }

    /**
     * Creates a recurring schedule for a doctor.
     */
    public DoctorScheduleDTO createSchedule(DoctorScheduleRequestDTO request) {
        Doctor doctor = doctorService.getDoctorEntity(request.getDoctorId());
        validateTimeRange(request);
        if (doctorScheduleRepository.existsByDoctorIdAndDayOfWeek(doctor.getId(), request.getDayOfWeek())) {
            throw new OurException("Schedule already exists for this doctor and day");
        }

        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setIsActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive());
        return toDto(doctorScheduleRepository.save(schedule));
    }

    /**
     * Returns one schedule by identifier.
     */
    @Transactional(readOnly = true)
    public DoctorScheduleDTO getScheduleById(Long id) {
        return toDto(getScheduleEntity(id));
    }

    /**
     * Returns active schedules for a doctor.
     */
    @Transactional(readOnly = true)
    public List<DoctorScheduleDTO> getActiveSchedulesByDoctor(Long doctorId) {
        return doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(doctorId).stream().map(this::toDto).toList();
    }

    /**
     * Returns active schedules for a doctor and day of week.
     */
    @Transactional(readOnly = true)
    public List<DoctorScheduleDTO> getActiveSchedulesByDoctorAndDay(Long doctorId, DayOfWeek dayOfWeek) {
        return doctorScheduleRepository.findByDoctorIdAndDayOfWeekAndIsActiveTrue(doctorId, dayOfWeek)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Updates a recurring schedule.
     */
    public DoctorScheduleDTO updateSchedule(Long id, DoctorScheduleRequestDTO request) {
        DoctorSchedule schedule = getScheduleEntity(id);
        validateTimeRange(request);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setIsActive(request.getIsActive() == null ? schedule.getIsActive() : request.getIsActive());
        return toDto(doctorScheduleRepository.save(schedule));
    }

    /**
     * Enables or disables a schedule.
     */
    public DoctorScheduleDTO setScheduleActive(Long id, boolean active) {
        DoctorSchedule schedule = getScheduleEntity(id);
        schedule.setIsActive(active);
        return toDto(doctorScheduleRepository.save(schedule));
    }

    /**
     * Deletes a schedule.
     */
    public void deleteSchedule(Long id) {
        doctorScheduleRepository.delete(getScheduleEntity(id));
    }

    /**
     * Loads a schedule entity for internal service composition.
     */
    @Transactional(readOnly = true)
    public DoctorSchedule getScheduleEntity(Long id) {
        return doctorScheduleRepository.findById(id)
                .orElseThrow(() -> new OurException("Doctor schedule not found with id: " + id));
    }

    /**
     * Converts a schedule entity to a DTO without nested entity exposure.
     */
    public DoctorScheduleDTO toDto(DoctorSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return new DoctorScheduleDTO(
                schedule.getId(),
                schedule.getDoctor() != null ? schedule.getDoctor().getId() : null,
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getIsActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    private void validateTimeRange(DoctorScheduleRequestDTO request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new OurException("End time must be after start time");
        }
    }
}
