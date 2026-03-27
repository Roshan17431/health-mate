package com.example.health.mate.repository;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorIdAndPatientId(Long doctorId, Long patientId);

    List<Appointment> findByDoctorIdAndAppointmentDateTime(Long doctorId, LocalDateTime appointmentDateTime);

    Optional<Appointment> findByAppointmentCode(String appointmentCode);

    Page<Appointment> findByDoctorId(Long doctorId, Pageable pageable);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findByStatus(STATUS status, Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDateTime BETWEEN :startDate AND :endDate")
    Page<Appointment> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status " +
            "AND a.appointmentDateTime BETWEEN :startDate AND :endDate")
    Page<Appointment> findByStatusAndDateRange(@Param("status") STATUS status,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);
}
