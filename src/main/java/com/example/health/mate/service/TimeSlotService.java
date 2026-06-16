package com.example.health.mate.service;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.dto.TimeSlotDTO;
import com.example.health.mate.dto.TimeSlotRequestDTO;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.TimeSlot;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@Transactional
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final DoctorService doctorService;

    public TimeSlotService(TimeSlotRepository timeSlotRepository, DoctorService doctorService) {
        this.timeSlotRepository = timeSlotRepository;
        this.doctorService = doctorService;
    }


    public TimeSlotDTO createTimeSlot(TimeSlotRequestDTO request) {
        Doctor doctor = doctorService.getDoctorEntity(request.getDoctorId());
        validateTimeRange(request);
        timeSlotRepository.findByDoctorIdAndSlotDateAndStartTime(
                doctor.getId(),
                request.getSlotDate(),
                request.getStartTime()
        ).ifPresent(existing -> {
            throw new OurException("Time slot already exists for this doctor, date, and start time");
        });

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setDoctor(doctor);
        timeSlot.setSlotDate(request.getSlotDate());
        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        timeSlot.setStatus(SlotStatus.AVAILABLE);
        return toDto(timeSlotRepository.save(timeSlot));
    }


    @Transactional(readOnly = true)
    public TimeSlotDTO getTimeSlotById(Long id) {
        return toDto(getTimeSlotEntity(id));
    }


    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getSlotsByDoctorDateAndStatus(Long doctorId, LocalDate slotDate, SlotStatus status) {
        return timeSlotRepository.findByDoctorIdAndSlotDateAndStatus(doctorId, slotDate, status)
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public List<TimeSlotDTO> getSlotsByDoctorDateRangeAndStatus(
            Long doctorId,
            LocalDate startDate,
            LocalDate endDate,
            SlotStatus status
    ) {
        return timeSlotRepository.findByDoctorIdAndSlotDateBetweenAndStatus(doctorId, startDate, endDate, status)
                .stream()
                .map(this::toDto)
                .toList();
    }


    public TimeSlotDTO updateStatus(Long id, SlotStatus status) {
        TimeSlot timeSlot = getTimeSlotEntity(id);
        timeSlot.setStatus(status);
        return toDto(timeSlotRepository.save(timeSlot));
    }


    public void deleteTimeSlot(Long id) {
        TimeSlot timeSlot = getTimeSlotEntity(id);
        if (timeSlot.getAppointment() != null) {
            throw new OurException("Cannot delete a time slot linked to an appointment");
        }
        timeSlotRepository.delete(timeSlot);
    }


    @Transactional(readOnly = true)
    public TimeSlot getTimeSlotEntity(Long id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new OurException("Time slot not found with id: " + id));
    }


    public TimeSlotDTO toDto(TimeSlot timeSlot) {
        if (timeSlot == null) {
            return null;
        }
        return new TimeSlotDTO(
                timeSlot.getId(),
                timeSlot.getDoctor() != null ? timeSlot.getDoctor().getId() : null,
                timeSlot.getSlotDate(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime(),
                timeSlot.getStatus(),
                timeSlot.getAppointment() != null ? timeSlot.getAppointment().getId() : null,
                timeSlot.getCreatedAt(),
                timeSlot.getUpdatedAt(),
                timeSlot.getVersion()
        );
    }

    private void validateTimeRange(TimeSlotRequestDTO request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new OurException("End time must be after start time");
        }
    }
}
