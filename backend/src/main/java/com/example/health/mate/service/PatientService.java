package com.example.health.mate.service;

import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.PatientDTO;
import com.example.health.mate.dto.PatientRequestDTO;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.StreamSupport;


@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserService userService;

    public PatientService(PatientRepository patientRepository, UserService userService) {
        this.patientRepository = patientRepository;
        this.userService = userService;
    }


    public PatientDTO createPatient(PatientRequestDTO request) {
        User user = userService.getUserEntity(request.getUserId());
        if (patientRepository.existsByUserId(user.getId())) {
            throw new OurException("Patient profile already exists for user id: " + user.getId());
        }

        user.setRole(USER_ROLE.ROLE_PATIENT);
        Patient patient = new Patient();
        patient.setUser(user);
        return toDto(patientRepository.save(patient));
    }


    @Transactional(readOnly = true)
    public List<PatientDTO> getAllPatients() {
        return StreamSupport.stream(patientRepository.findAll().spliterator(), false)
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public PatientDTO getPatientById(Long id) {
        return toDto(getPatientEntity(id));
    }


    public PatientDTO getPatientByUserId(Long userId) {
        return toDto(patientRepository.findByUserId(userId)
                .orElseGet(() -> createMissingPatientProfile(userId)));
    }


    public void deletePatient(Long id) {
        patientRepository.delete(getPatientEntity(id));
    }


    @Transactional(readOnly = true)
    public Patient getPatientEntity(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new OurException("Patient not found with id: " + id));
    }


    public PatientDTO toDto(Patient patient) {
        if (patient == null) {
            return null;
        }
        User user = patient.getUser();
        return new PatientDTO(
                patient.getId(),
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }

    private Patient createMissingPatientProfile(Long userId) {
        User user = userService.getUserEntity(userId);
        if (user.getRole() != USER_ROLE.ROLE_PATIENT) {
            throw new OurException("Patient not found for user id: " + userId);
        }

        Patient patient = new Patient();
        patient.setUser(user);
        return patientRepository.save(patient);
    }
}
