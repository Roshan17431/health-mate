package com.example.health.mate.repository;

import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends CrudRepository<Patient, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> findByIdForUpdate(@Param("id") Long id);

    Optional<Patient> findByUser(User user);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}
