package com.example.health.mate.repository;

import com.example.health.mate.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule,Long> {

    @Modifying
    @Query("delete from DoctorSchedule s where s.doctor.id = :doctorId")
    void deleteByDoctorId(@Param("doctorId") Long doctorId);

    List<DoctorSchedule> findByDoctorIdAndIsActiveTrue(Long doctorId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndIsActiveTrue(Long doctorId, DayOfWeek dayOfWeek);

    boolean existsByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
}
