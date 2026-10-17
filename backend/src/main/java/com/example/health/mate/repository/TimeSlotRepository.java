package com.example.health.mate.repository;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Modifying
    @Query("delete from TimeSlot s where s.doctor.id = :doctorId")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);

    @Query("select s.doctor.id from TimeSlot s where s.id = :id")
    Optional<Long> findDoctorIdById(@Param("id") Long id);

    List<TimeSlot> findByDoctorIdAndSlotDateAndStatus(Long doctor_id, LocalDate slot_date,SlotStatus slotStatus);

    List<TimeSlot> findByDoctorIdAndSlotDateBetweenAndStatus(Long doctor_id, LocalDate startDate, LocalDate endDate, SlotStatus slotStatus);

    Optional<TimeSlot> findByDoctorIdAndSlotDateAndStartTime(Long doctor_id, LocalDate slot_date, LocalTime startTime);

}
