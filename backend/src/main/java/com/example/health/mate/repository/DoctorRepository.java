package com.example.health.mate.repository;

import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    List<Doctor> findBySpecialization(String specialization);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    @Query("select distinct d.specialization from Doctor d order by d.specialization")
    List<String> findDistinctSpecializations();

    Optional<Doctor> findByUser(User user);

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
