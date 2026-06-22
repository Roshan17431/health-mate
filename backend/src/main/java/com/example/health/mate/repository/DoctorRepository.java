package com.example.health.mate.repository;

import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findBySpecialization(String specialization);

    Optional<Doctor> findByUser(User user);

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}

