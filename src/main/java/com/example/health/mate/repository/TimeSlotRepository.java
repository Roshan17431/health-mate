package com.example.health.mate.repository;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.entity.Appointment;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

import java.time.LocalTime;
import java.util.List;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Integer> {

    List<TimeSlot> findAllByDoctor(Doctor doctor);

    List<TimeSlot> findBySlotDate(LocalDate slotDate);

    List<TimeSlot> findByStartTime(LocalTime startTime);

    List<TimeSlot> findByEndTime(LocalTime endTime);

    List<TimeSlot> findByStatus(SlotStatus status);

    List<TimeSlot> findByStartTimeAndEndTime(LocalTime startTime, LocalTime endTime);

    List<TimeSlot> findByAppointment(Appointment appointment);


}
