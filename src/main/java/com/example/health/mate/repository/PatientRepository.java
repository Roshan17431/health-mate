package com.example.health.mate.repository;

import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends CrudRepository<Patient, Long> {

    Optional<Patient> findByUser(User user);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}
