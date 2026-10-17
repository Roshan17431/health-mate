package com.example.health.mate.service;

import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.DoctorDTO;
import com.example.health.mate.dto.DoctorRequestDTO;
import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserService userService;
    private final ProfileDeletionService profileDeletion;

    public DoctorService(DoctorRepository doctorRepository, UserService userService,
                         ProfileDeletionService profileDeletion) {
        this.doctorRepository = doctorRepository;
        this.userService = userService;
        this.profileDeletion = profileDeletion;
    }


    public DoctorDTO createDoctor(DoctorRequestDTO request) {
        User user = userService.getUserEntity(request.getUserId());
        if (user.getRole() != USER_ROLE.ROLE_DOCTOR) {
            throw new OurException("Doctor profiles require a doctor account");
        }
        if (doctorRepository.existsByUserId(user.getId())) {
            throw new OurException("Doctor profile already exists for user id: " + user.getId());
        }

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setExperience(request.getExperience());
        doctor.setAvailableHours(request.getAvailableHours());
        doctor.setSpecialization(request.getSpecialization());
        return toDto(doctorRepository.save(doctor));
    }


    @Transactional(readOnly = true)
    public List<DoctorDTO> getAllDoctors() {
        return doctorRepository.findAll().stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public DoctorDTO getDoctorById(Long id) {
        return toDto(getDoctorEntity(id));
    }


    @Transactional(readOnly = true)
    public List<DoctorDTO> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization).stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public List<String> getAllSpecializations() {
        return doctorRepository.findDistinctSpecializations();
    }


    public DoctorDTO updateDoctor(Long id, DoctorRequestDTO request) {
        Doctor doctor = getDoctorEntity(id);
        doctor.setExperience(request.getExperience());
        doctor.setAvailableHours(request.getAvailableHours());
        doctor.setSpecialization(request.getSpecialization());
        return toDto(doctorRepository.save(doctor));
    }


    public void deleteDoctor(Long id) {
        profileDeletion.deleteDoctor(id);
    }


    @Transactional(readOnly = true)
    public Doctor getDoctorEntity(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new OurException("Doctor not found with id: " + id));
    }


    public DoctorDTO toDto(Doctor doctor) {
        if (doctor == null) {
            return null;
        }
        User user = doctor.getUser();
        return new DoctorDTO(
                doctor.getId(),
                doctor.getExperience(),
                doctor.getAvailableHours(),
                doctor.getSpecialization(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null
        );
    }
}
