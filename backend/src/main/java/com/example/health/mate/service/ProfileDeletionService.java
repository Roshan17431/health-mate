package com.example.health.mate.service;

import com.example.health.mate.entity.Doctor;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.exception.ConflictException;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.AppointmentRepository;
import com.example.health.mate.repository.DoctorRepository;
import com.example.health.mate.repository.DoctorScheduleRepository;
import com.example.health.mate.repository.PatientRepository;
import com.example.health.mate.repository.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates profile removal without cascading into an account or clinical history. */
@Service
@Transactional
public class ProfileDeletionService {
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AppointmentRepository appointments;
    private final DoctorScheduleRepository schedules;
    private final TimeSlotRepository slots;

    public ProfileDeletionService(DoctorRepository doctors, PatientRepository patients,
                                  AppointmentRepository appointments, DoctorScheduleRepository schedules,
                                  TimeSlotRepository slots) {
        this.doctors = doctors;
        this.patients = patients;
        this.appointments = appointments;
        this.schedules = schedules;
        this.slots = slots;
    }

    public void deleteDoctor(Long id) {
        Doctor doctor = doctors.findByIdForUpdate(id)
                .orElseThrow(() -> new OurException("Doctor not found with id: " + id));
        requireNoDoctorHistory(id);
        removeDoctor(doctor);
    }

    public void deletePatient(Long id) {
        Patient patient = patients.findByIdForUpdate(id)
                .orElseThrow(() -> new OurException("Patient not found with id: " + id));
        requireNoPatientHistory(id);
        patients.delete(patient);
    }

    public void deleteProfilesForUser(Long userId) {
        // Shared primary keys identify the profiles. Match booking's doctor-then-patient lock order.
        Doctor doctor = doctors.findByIdForUpdate(userId).orElse(null);
        Patient patient = patients.findByIdForUpdate(userId).orElse(null);
        if (doctor != null) requireNoDoctorHistory(doctor.getId());
        if (patient != null) requireNoPatientHistory(patient.getId());
        if (doctor != null) removeDoctor(doctor);
        if (patient != null) patients.delete(patient);
    }

    private void requireNoDoctorHistory(Long id) {
        if (!appointments.findByDoctorId(id).isEmpty()) {
            throw new ConflictException("Doctor has appointment history and cannot be deleted");
        }
    }

    private void requireNoPatientHistory(Long id) {
        if (!appointments.findByPatientId(id).isEmpty()) {
            throw new ConflictException("Patient has appointment history and cannot be deleted");
        }
    }

    private void removeDoctor(Doctor doctor) {
        slots.deleteByDoctorId(doctor.getId());
        schedules.deleteByDoctorId(doctor.getId());
        doctors.delete(doctor);
    }
}
