package com.example.health.mate.security;

import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.PatientDTO;
import com.example.health.mate.entity.Appointment;
import com.example.health.mate.entity.User;
import com.example.health.mate.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Authorization uses the authenticated database identity, never a submitted actor ID. */
@Component("access")
@Transactional(readOnly = true)
public class AccessRules {
    private final AppointmentRepository appointments;
    private final DoctorScheduleRepository schedules;
    private final TimeSlotRepository slots;
    private final NotificationRepository notifications;
    public AccessRules(AppointmentRepository appointments, DoctorScheduleRepository schedules,
                       TimeSlotRepository slots, NotificationRepository notifications) {
        this.appointments = appointments; this.schedules = schedules;
        this.slots = slots; this.notifications = notifications;
    }
    public User currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }
    public Long userId() { User user = currentUser(); return user == null ? null : user.getId(); }
    public boolean admin() { return role(USER_ROLE.ROLE_ADMIN); }
    public boolean doctor() { return role(USER_ROLE.ROLE_DOCTOR); }
    private boolean role(USER_ROLE role) { User user = currentUser(); return user != null && user.getRole() == role; }
    public boolean ownUser(Long id) { return admin() || (id != null && Objects.equals(userId(), id)); }
    public boolean ownEmail(String email) {
        return admin() || (currentUser() != null && currentUser().getEmail().equals(email));
    }
    public boolean doctorOwner(Long id) { return admin() || (doctor() && ownUser(id)); }
    public boolean patientOwner(Long id) { return admin() || (role(USER_ROLE.ROLE_PATIENT) && ownUser(id)); }
    public boolean patient(Long id) {
        return patientOwner(id) || (doctor() && !appointments.findByDoctorIdAndPatientId(userId(), id).isEmpty());
    }
    public boolean appointment(Long id) {
        return admin() || appointments.findById(id).map(this::appointment).orElse(false);
    }
    public boolean appointmentCode(String code) {
        return admin() || appointments.findByAppointmentCode(code).map(this::appointment).orElse(false);
    }
    private boolean appointment(Appointment appointment) {
        return patientOwner(appointment.getPatient().getId()) || doctorOwner(appointment.getDoctor().getId());
    }
    public boolean appointmentDoctor(Long id) {
        return admin() || appointments.findById(id).map(a -> doctorOwner(a.getDoctor().getId())).orElse(false);
    }
    public boolean schedule(Long id) {
        return admin() || schedules.findById(id).map(s -> doctorOwner(s.getDoctor().getId())).orElse(false);
    }
    public boolean scheduleUpdate(Long id, Long doctorId) {
        return schedule(id) && schedules.findById(id).map(s -> Objects.equals(s.getDoctor().getId(), doctorId)).orElse(false);
    }
    public boolean slotOwner(Long id) {
        return admin() || slots.findById(id).map(s -> doctorOwner(s.getDoctor().getId())).orElse(false);
    }
    public boolean slot(Long id) {
        return admin() || slots.findById(id).map(s -> s.getStatus() == SlotStatus.AVAILABLE || doctorOwner(s.getDoctor().getId())
                || appointments.findByPatientId(userId()).stream().anyMatch(a -> a.getStatus() != com.example.health.mate.domain.STATUS.CANCELED && a.getTimeSlot() != null && a.getTimeSlot().getId().equals(id))).orElse(false);
    }
    public boolean slotDirectory(Long doctorId, SlotStatus status) {
        return status == SlotStatus.AVAILABLE || doctorOwner(doctorId);
    }
    public boolean notification(Long id) {
        return admin() || notifications.findById(id).map(n -> ownUser(n.getUser().getId())).orElse(false);
    }
    public List<PatientDTO> patientDirectory(List<PatientDTO> all) {
        if (admin()) return all;
        Set<Long> assigned = appointments.findByDoctorId(userId()).stream().map(a -> a.getPatient().getId()).collect(Collectors.toSet());
        return all.stream().filter(p -> assigned.contains(p.getId())).toList();
    }
}
