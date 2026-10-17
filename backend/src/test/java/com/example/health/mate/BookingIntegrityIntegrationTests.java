package com.example.health.mate;

import com.example.health.mate.domain.STATUS;
import com.example.health.mate.domain.SlotStatus;
import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.AppointmentDTO;
import com.example.health.mate.dto.AppointmentRequestDTO;
import com.example.health.mate.dto.TimeSlotDTO;
import com.example.health.mate.dto.TimeSlotRequestDTO;
import com.example.health.mate.exception.ConflictException;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.AppointmentRepository;
import com.example.health.mate.service.AppointmentService;
import com.example.health.mate.service.TimeSlotService;
import com.example.health.mate.support.ApiIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real transactions and concurrent service requests against a private, disposable database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingIntegrityIntegrationTests extends ApiIntegrationSupport {
    private static final String DATABASE = "booking_integrity_" + UUID.randomUUID();
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:" + DATABASE
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;LOCK_TIMEOUT=10000");
    }

    @Autowired AppointmentService appointments;
    @Autowired TimeSlotService slots;
    @Autowired AppointmentRepository appointmentRepository;

    @Test
    void simultaneousSlotlessBookingsHaveOneWinnerAndControlledConflicts() throws Exception {
        Fixture fixture = fixture();
        List<Object> results = concurrent(4, () -> appointments.createAppointment(request(fixture, null)));
        assertOneBookingAndConflicts(results, fixture);
    }

    @Test
    void simultaneousSlotBookingsHaveOneWinnerAndControlledConflicts() throws Exception {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        List<Object> results = concurrent(4, () -> appointments.createAppointment(request(fixture, slotId)));
        assertOneBookingAndConflicts(results, fixture);
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void slotBookingRejectsDifferentDate() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentRequestDTO request = request(fixture, slotId);
        request.setAppointmentDateTime(fixture.time().plusDays(1));
        assertThatThrownBy(() -> appointments.createAppointment(request)).isInstanceOf(OurException.class);
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void slotBookingRejectsDifferentStartTime() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentRequestDTO request = request(fixture, slotId);
        request.setAppointmentDateTime(fixture.time().plusMinutes(10));
        assertThatThrownBy(() -> appointments.createAppointment(request)).isInstanceOf(OurException.class);
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    }

    @Test
    void blockedSlotReturnsControlledConflict() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        slots.updateStatus(slotId, SlotStatus.BLOCKED);
        assertThatThrownBy(() -> appointments.createAppointment(request(fixture, slotId)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void canceledSlotlessBookingCanBeRebookedWhileHistoryRemains() {
        Fixture fixture = fixture();
        AppointmentDTO first = appointments.createAppointment(request(fixture, null));
        appointments.cancelAppointment(first.getId());
        AppointmentDTO second = appointments.createAppointment(request(fixture, null));
        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(appointments.getAppointmentById(first.getId()).getStatus()).isEqualTo(STATUS.CANCELED);
        assertThat(appointments.getAppointmentsByDoctor(fixture.doctorId())).hasSize(2);
    }

    @Test
    void canceledSlotBookingCanBeRebookedWithoutLosingItsSlotHistory() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentDTO first = appointments.createAppointment(request(fixture, slotId));
        appointments.cancelAppointment(first.getId());
        assertThat(slots.getTimeSlotById(slotId).getAppointmentId()).isNull();
        AppointmentDTO second = appointments.createAppointment(request(fixture, slotId));
        assertThat(appointments.getAppointmentById(first.getId()).getTimeSlotId()).isEqualTo(slotId);
        assertThat(appointments.getAppointmentById(first.getId()).getStatus()).isEqualTo(STATUS.CANCELED);
        assertThat(slots.getTimeSlotById(slotId).getAppointmentId()).isEqualTo(second.getId());
    }

    @Test
    void cancelingOldHistoryDoesNotReleaseTheNewBooking() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentDTO first = appointments.createAppointment(request(fixture, slotId));
        appointments.cancelAppointment(first.getId());
        AppointmentDTO second = appointments.createAppointment(request(fixture, slotId));
        appointments.cancelAppointment(first.getId());
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(slots.getTimeSlotById(slotId).getAppointmentId()).isEqualTo(second.getId());
    }

    @Test
    void deletingCanceledHistoryDoesNotReleaseTheNewBooking() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentDTO first = appointments.createAppointment(request(fixture, slotId));
        appointments.cancelAppointment(first.getId());
        AppointmentDTO second = appointments.createAppointment(request(fixture, slotId));
        appointments.deleteAppointment(first.getId());
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.BOOKED);
        assertThat(slots.getTimeSlotById(slotId).getAppointmentId()).isEqualTo(second.getId());
    }

    @Test
    void reactivatingCanceledHistoryCannotDisplaceNewBooking() {
        Fixture fixture = fixture();
        AppointmentDTO first = appointments.createAppointment(request(fixture, null));
        appointments.cancelAppointment(first.getId());
        appointments.createAppointment(request(fixture, null));
        assertThatThrownBy(() -> appointments.updateStatus(first.getId(), STATUS.CONFIRMED))
                .isInstanceOf(ConflictException.class);
        assertThat(appointments.getAppointmentById(first.getId()).getStatus()).isEqualTo(STATUS.CANCELED);
    }

    @Test
    void slotStatusCannotReleaseAnActiveBooking() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        appointments.createAppointment(request(fixture, slotId));
        assertThatThrownBy(() -> slots.updateStatus(slotId, SlotStatus.AVAILABLE))
                .isInstanceOf(ConflictException.class);
        assertThat(slots.getTimeSlotById(slotId).getStatus()).isEqualTo(SlotStatus.BOOKED);
    }

    @Test
    void simultaneousDuplicateSlotCreationHasOneWinnerAndControlledConflicts() throws Exception {
        Fixture fixture = fixture();
        List<Object> results = concurrent(4, () -> slot(fixture));
        assertThat(results.stream().filter(TimeSlotDTO.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ConflictException.class::isInstance)).hasSize(3);
        assertThat(slots.getSlotsByDoctorDateAndStatus(fixture.doctorId(), fixture.time().toLocalDate(),
                SlotStatus.AVAILABLE)).hasSize(1);
    }

    @Test
    void slotWithCanceledHistoryCannotBeDeleted() {
        Fixture fixture = fixture();
        Long slotId = slot(fixture).getId();
        AppointmentDTO appointment = appointments.createAppointment(request(fixture, slotId));
        appointments.cancelAppointment(appointment.getId());
        assertThatThrownBy(() -> slots.deleteTimeSlot(slotId)).isInstanceOf(ConflictException.class);
        assertThat(appointments.getAppointmentById(appointment.getId()).getTimeSlotId()).isEqualTo(slotId);
    }

    private void assertOneBookingAndConflicts(List<Object> results, Fixture fixture) {
        assertThat(results.stream().filter(AppointmentDTO.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(ConflictException.class::isInstance)).hasSize(3);
        assertThat(appointmentRepository.findByDoctorIdAndAppointmentDateTime(fixture.doctorId(), fixture.time()))
                .hasSize(1);
    }

    private Fixture fixture() {
        Long doctorId = seedUser(USER_ROLE.ROLE_DOCTOR).getId();
        Long patientId = seedUser(USER_ROLE.ROLE_PATIENT).getId();
        return new Fixture(doctorId, patientId, LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0));
    }

    private AppointmentRequestDTO request(Fixture fixture, Long slotId) {
        AppointmentRequestDTO request = new AppointmentRequestDTO();
        request.setDoctorId(fixture.doctorId());
        request.setPatientId(fixture.patientId());
        request.setAppointmentDateTime(fixture.time());
        request.setTimeSlotId(slotId);
        return request;
    }

    private TimeSlotDTO slot(Fixture fixture) {
        TimeSlotRequestDTO request = new TimeSlotRequestDTO();
        request.setDoctorId(fixture.doctorId());
        request.setSlotDate(fixture.time().toLocalDate());
        request.setStartTime(fixture.time().toLocalTime());
        request.setEndTime(fixture.time().plusMinutes(30).toLocalTime());
        return slots.createTimeSlot(request);
    }

    private List<Object> concurrent(int count, Callable<?> action) throws Exception {
        var executor = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Object>>();
            for (int i = 0; i < count; i++) futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Start barrier timed out");
                try { return action.call(); } catch (RuntimeException failure) { return failure; }
            }));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private record Fixture(Long doctorId, Long patientId, LocalDateTime time) {}
}
