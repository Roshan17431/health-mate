package com.example.health.mate.repository;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByDoctorIdAndSlotDateAndStatus(Long doctor_id, LocalDate slot_date,SlotStatus slotStatus);

    List<TimeSlot> findByDoctorIdAndSlotDateBetweenAndStatus(Long doctor_id, LocalDate startDate, LocalDate endDate, SlotStatus slotStatus);

    Optional<TimeSlot> findByDoctorIdAndSlotDateAndStartTime(Long doctor_id, LocalDate slot_date, LocalTime startTime);

}
