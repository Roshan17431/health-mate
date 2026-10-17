package com.example.health.mate;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.DoctorRequestDTO;
import com.example.health.mate.dto.PatientRequestDTO;
import com.example.health.mate.entity.*;
import com.example.health.mate.exception.ConflictException;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.*;
import com.example.health.mate.service.DoctorService;
import com.example.health.mate.service.PatientService;
import com.example.health.mate.service.UserService;
import com.example.health.mate.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real transactions and foreign keys in a disposable database; never the configured PostgreSQL database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountIntegrityIntegrationTests extends ApiIntegrationSupport {
    private static final String DATABASE = "jdbc:h2:mem:account_integrity_" + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> DATABASE);
    }
    @Autowired DoctorService doctorService;
    @Autowired PatientService patientService;
    @Autowired UserService userService;
    @Autowired AppointmentRepository appointments;
    @Autowired DoctorScheduleRepository schedules;
    @Autowired TimeSlotRepository slots;
    @Autowired AuditLogRepository audits;
    @Autowired NotificationRepository notifications;

    @ParameterizedTest
    @EnumSource(value = USER_ROLE.class, names = {"ROLE_PATIENT", "ROLE_ADMIN"})
    void doctorProfileCannotChangeAnExistingAccountsRole(USER_ROLE role) {
        User user = seedUser(role);
        assertThatThrownBy(() -> doctorService.createDoctor(doctorRequest(user.getId())))
                .isInstanceOf(OurException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getRole()).isEqualTo(role);
        assertThat(doctors.findByUserId(user.getId())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = USER_ROLE.class, names = {"ROLE_DOCTOR", "ROLE_ADMIN"})
    void patientProfileCannotChangeAnExistingAccountsRole(USER_ROLE role) {
        User user = seedUser(role);
        assertThatThrownBy(() -> patientService.createPatient(patientRequest(user.getId())))
                .isInstanceOf(OurException.class);
        assertThat(users.findById(user.getId()).orElseThrow().getRole()).isEqualTo(role);
        assertThat(patients.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void matchingRoleAccountsCanBeLinkedToProfiles() {
        User doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        User patient = seedUser(USER_ROLE.ROLE_PATIENT);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            doctors.deleteById(doctor.getId()); patients.deleteById(patient.getId());
        });
        assertThat(doctorService.createDoctor(doctorRequest(doctor.getId())).getUserId()).isEqualTo(doctor.getId());
        assertThat(patientService.createPatient(patientRequest(patient.getId())).getId()).isEqualTo(patient.getId());
        assertThat(users.findById(doctor.getId()).orElseThrow().getRole()).isEqualTo(USER_ROLE.ROLE_DOCTOR);
        assertThat(users.findById(patient.getId()).orElseThrow().getRole()).isEqualTo(USER_ROLE.ROLE_PATIENT);
    }

    @Test
    void deletingDoctorProfilePreservesAccountAndRemovesUnusedSchedulingData() {
        User doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        SchedulingIds scheduling = scheduling(doctor.getId());
        doctorService.deleteDoctor(doctor.getId());
        assertThat(doctors.existsById(doctor.getId())).isFalse();
        assertThat(users.existsById(doctor.getId())).isTrue();
        assertThat(schedules.existsById(scheduling.schedule())).isFalse();
        assertThat(slots.existsById(scheduling.slot())).isFalse();
    }

    @Test
    void deletingPatientProfilePreservesAccount() {
        User patient = seedUser(USER_ROLE.ROLE_PATIENT);
        patientService.deletePatient(patient.getId());
        assertThat(patients.existsById(patient.getId())).isFalse();
        assertThat(users.existsById(patient.getId())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(STATUS.class)
    void doctorAndPatientProfileDeletionPreservesEveryAppointmentStatus(STATUS status) {
        User doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        User patient = seedUser(USER_ROLE.ROLE_PATIENT);
        Long appointmentId = appointment(doctor.getId(), patient.getId(), status);
        assertThatThrownBy(() -> doctorService.deleteDoctor(doctor.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> patientService.deletePatient(patient.getId())).isInstanceOf(ConflictException.class);
        assertThat(appointments.findById(appointmentId).orElseThrow().getStatus()).isEqualTo(status);
        assertThat(doctors.existsById(doctor.getId())).isTrue();
        assertThat(patients.existsById(patient.getId())).isTrue();
        assertThat(users.existsById(doctor.getId())).isTrue();
        assertThat(users.existsById(patient.getId())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = USER_ROLE.class, names = {"ROLE_DOCTOR", "ROLE_PATIENT", "ROLE_ADMIN"})
    void deletingAccountRemovesOwnedDependentsAndPreservesAuditHistory(USER_ROLE role) {
        User user = seedUser(role);
        SchedulingIds scheduling = role == USER_ROLE.ROLE_DOCTOR ? scheduling(user.getId()) : null;
        Long auditId = audit(user);
        Long notificationId = notification(user);
        userService.deleteUser(user.getId());
        assertThat(users.existsById(user.getId())).isFalse();
        assertThat(doctors.findByUserId(user.getId())).isEmpty();
        assertThat(patients.findByUserId(user.getId())).isEmpty();
        assertThat(notifications.existsById(notificationId)).isFalse();
        AuditLog audit = audits.findById(auditId).orElseThrow();
        assertThat(audit.getUser()).isNull();
        assertThat(audit.getAction()).isEqualTo("ACCOUNT_REVIEWED");
        assertThat(audit.getDetails()).isEqualTo("Keep this historical record");
        if (scheduling != null) {
            assertThat(schedules.existsById(scheduling.schedule())).isFalse();
            assertThat(slots.existsById(scheduling.slot())).isFalse();
        }
    }

    @ParameterizedTest
    @EnumSource(value = USER_ROLE.class, names = {"ROLE_DOCTOR", "ROLE_PATIENT"})
    void deletingAccountWithAppointmentHistoryFailsWithoutDeletingDependents(USER_ROLE role) {
        User doctor = seedUser(USER_ROLE.ROLE_DOCTOR);
        User patient = seedUser(USER_ROLE.ROLE_PATIENT);
        User target = role == USER_ROLE.ROLE_DOCTOR ? doctor : patient;
        Long appointmentId = appointment(doctor.getId(), patient.getId(), STATUS.CANCELED);
        Long auditId = audit(target);
        Long notificationId = notification(target);
        assertThatThrownBy(() -> userService.deleteUser(target.getId())).isInstanceOf(ConflictException.class);
        assertThat(users.existsById(target.getId())).isTrue();
        assertThat(appointments.existsById(appointmentId)).isTrue();
        assertThat(notifications.existsById(notificationId)).isTrue();
        assertThat(audits.findById(auditId).orElseThrow().getUser().getId()).isEqualTo(target.getId());
    }

    private DoctorRequestDTO doctorRequest(Long userId) {
        DoctorRequestDTO request = new DoctorRequestDTO();
        request.setUserId(userId); request.setExperience(5);
        request.setAvailableHours("09:00-17:00"); request.setSpecialization("Cardiology");
        return request;
    }
    private PatientRequestDTO patientRequest(Long userId) {
        PatientRequestDTO request = new PatientRequestDTO(); request.setUserId(userId); return request;
    }
    private SchedulingIds scheduling(Long doctorId) {
        return new TransactionTemplate(transactions).execute(status -> {
            Doctor doctor = doctors.findById(doctorId).orElseThrow();
            DoctorSchedule schedule = new DoctorSchedule(); schedule.setDoctor(doctor);
            schedule.setDayOfWeek(DayOfWeek.MONDAY); schedule.setStartTime(LocalTime.of(9, 0));
            schedule.setEndTime(LocalTime.of(17, 0)); schedules.save(schedule);
            TimeSlot slot = new TimeSlot(); slot.setDoctor(doctor); slot.setSlotDate(LocalDate.now().plusDays(1));
            slot.setStartTime(LocalTime.of(9, 0)); slot.setEndTime(LocalTime.of(9, 30)); slots.save(slot);
            return new SchedulingIds(schedule.getId(), slot.getId());
        });
    }
    private Long appointment(Long doctorId, Long patientId, STATUS status) {
        return new TransactionTemplate(transactions).execute(transaction -> {
            Appointment appointment = new Appointment();
            appointment.setDoctor(doctors.findById(doctorId).orElseThrow());
            appointment.setPatient(patients.findById(patientId).orElseThrow());
            appointment.setAppointmentDateTime(LocalDateTime.now().plusDays(1));
            appointment.setAppointmentCode(UUID.randomUUID().toString()); appointment.setStatus(status);
            return appointments.save(appointment).getId();
        });
    }
    private Long audit(User user) {
        AuditLog audit = new AuditLog(); audit.setUser(user); audit.setAction("ACCOUNT_REVIEWED");
        audit.setDetails("Keep this historical record"); return audits.save(audit).getId();
    }
    private Long notification(User user) {
        Notification notification = new Notification(); notification.setUser(user); notification.setMessage("Fixture");
        return notifications.save(notification).getId();
    }
    private record SchedulingIds(Long schedule, Long slot) {}
}
