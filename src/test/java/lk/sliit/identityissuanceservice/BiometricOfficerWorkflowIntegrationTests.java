package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BiometricSlotView;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricAppointmentSlot;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricAppointmentSlotRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricScheduleService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Officer biometric workflow against the real services and database (rolled back after each test):
 * check-in, completion, moving a missed booking, the day locks used when closing or republishing, and
 * which slots may still be deleted.
 */
@SpringBootTest
@Transactional
class BiometricOfficerWorkflowIntegrationTests {

    @Autowired AppointmentService appointmentService;
    @Autowired BiometricService biometricService;
    @Autowired BiometricScheduleService schedules;
    @Autowired AppointmentRepository appointments;
    @Autowired BiometricAppointmentSlotRepository slotRepository;
    @Autowired CentreRepository centres;
    @Autowired CitizenApplicationRepository applications;
    @Autowired UserAccountRepository users;
    @Autowired CitizenAccountRepository citizens;
    @Autowired StaffAccountRepository staff;

    private Integer officerId;
    private Centre centre;

    @BeforeEach
    void setUp() {
        UserAccount officerUser = newUser("officer");
        StaffAccount officer = new StaffAccount();
        officer.setUserId(officerUser.getUserId());
        officer.setEmployeeNo("BIO-T-" + unique());
        officer.setStaffRole("NIC_OFFICER");
        officer.setStaffStatus("ACTIVE");
        staff.saveAndFlush(officer);
        officerId = officerUser.getUserId();

        centre = new Centre();
        centre.setCentreName("Officer test " + unique());
        centre.setCity("Colombo");
        centre.setDistrict("Colombo");
        centre.setDailyCapacity(1);
        centre = centres.saveAndFlush(centre);
    }

    // ---- Check-in and completion ----

    @Test
    void aSecondCheckInIsRejected() {
        Appointment appointment = booking("CONFIRMED", LocalDate.now().atTime(9, 0), null);

        appointmentService.checkIn(appointment.getReferenceNo(), "NIC", officerId, "QR");
        assertEquals("CHECKED_IN", appointments.findById(appointment.getAppointmentId()).orElseThrow().getAppointmentStatus());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> appointmentService.checkIn(appointment.getReferenceNo(), "NIC", officerId, "QR"));
        assertTrue(error.getMessage().contains("already been checked in"));
    }

    @Test
    void biometricsNeedACheckInAndCanOnlyBeCollectedOnce() {
        Appointment appointment = booking("CONFIRMED", LocalDate.now().atTime(9, 0), null);
        String reference = appointment.getReferenceNo();

        IllegalArgumentException notCheckedIn = assertThrows(IllegalArgumentException.class,
                () -> biometricService.completeCollection(reference, "NIC", officerId));
        assertTrue(notCheckedIn.getMessage().contains("Check in the citizen"));

        appointmentService.checkIn(reference, "NIC", officerId, "REFERENCE_NUMBER");
        biometricService.completeCollection(reference, "NIC", officerId);
        assertEquals("COMPLETED", appointments.findById(appointment.getAppointmentId()).orElseThrow().getAppointmentStatus());
        assertEquals("BIOMETRIC_COLLECTED",
                applications.findById(appointment.getApplicationId()).orElseThrow().getCurrentStatus());

        IllegalArgumentException twice = assertThrows(IllegalArgumentException.class,
                () -> biometricService.completeCollection(reference, "NIC", officerId));
        assertTrue(twice.getMessage().contains("already been collected"));
    }

    // ---- Moving a missed booking ----

    @Test
    void aMissedBookingMovesToTheNextFutureSlotAtTheSameCentre() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        schedules.publishDateRange("NIC", centre.getCentreId(), tomorrow, tomorrow,
                LocalTime.of(9, 0), LocalTime.of(16, 30), 1, officerId);
        Appointment missed = booking("CONFIRMED", LocalDate.now().minusDays(1).atTime(9, 0), null);

        appointmentService.rescheduleMissed(missed.getReferenceNo(), "NIC", officerId);

        Appointment moved = appointments.findById(missed.getAppointmentId()).orElseThrow();
        assertEquals(tomorrow.atTime(9, 0), moved.getScheduledAt());
        assertEquals("CONFIRMED", moved.getAppointmentStatus());
        assertEquals(1, schedules.bookedCount(moved.getBiometricSlotId(), null));
    }

    @Test
    void aMissedBookingCannotMoveWhenNoFutureSlotIsPublished() {
        Appointment missed = booking("CONFIRMED", LocalDate.now().minusDays(1).atTime(9, 0), null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> appointmentService.rescheduleMissed(missed.getReferenceNo(), "NIC", officerId));
        assertTrue(error.getMessage().contains("No future slots"));
    }

    @Test
    void anAppointmentThatIsNotMissedCannotBeMovedHere() {
        Appointment upcoming = booking("CONFIRMED", LocalDate.now().plusDays(3).atTime(9, 0), null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> appointmentService.rescheduleMissed(upcoming.getReferenceNo(), "NIC", officerId));
        assertTrue(error.getMessage().contains("earlier date"));
    }

    // ---- Closing and republishing a day ----

    @Test
    void closingADayCancelsConfirmedBookingsAndKeepsCheckedInOnes() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        publish(tomorrow);
        Appointment confirmed = booking("CONFIRMED", tomorrow.atTime(9, 0), slotAt(tomorrow, 9, 0).getSlotId());
        Appointment checkedIn = booking("CHECKED_IN", tomorrow.atTime(9, 30), slotAt(tomorrow, 9, 30).getSlotId());

        int closedDays = schedules.closeDateRange("NIC", centre.getCentreId(), tomorrow, tomorrow);

        assertEquals(1, closedDays);
        assertEquals("CANCELLED", appointments.findById(confirmed.getAppointmentId()).orElseThrow().getAppointmentStatus());
        assertEquals("APPROVED", applications.findById(confirmed.getApplicationId()).orElseThrow().getCurrentStatus());
        assertEquals("CHECKED_IN", appointments.findById(checkedIn.getAppointmentId()).orElseThrow().getAppointmentStatus());
        assertTrue(schedules.getSchedule("NIC", centre.getCentreId(), tomorrow, tomorrow).stream()
                .noneMatch(BiometricSlotView::available));
    }

    @Test
    void publishingAgainReopensClosedSlotsAndKeepsTheirBookingsCancelled() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        publish(tomorrow);
        Appointment confirmed = booking("CONFIRMED", tomorrow.atTime(9, 0), slotAt(tomorrow, 9, 0).getSlotId());
        schedules.closeDateRange("NIC", centre.getCentreId(), tomorrow, tomorrow);

        publish(tomorrow);

        assertTrue(schedules.getSchedule("NIC", centre.getCentreId(), tomorrow, tomorrow).stream()
                .allMatch(BiometricSlotView::available));
        assertEquals("CANCELLED", appointments.findById(confirmed.getAppointmentId()).orElseThrow().getAppointmentStatus());
    }

    // ---- Which slots may be deleted ----

    @Test
    void onlyFutureSlotsThatNeverHadABookingAreDeletable() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        publish(tomorrow);
        BiometricAppointmentSlot booked = slotAt(tomorrow, 9, 0);
        BiometricAppointmentSlot cancelledHistory = slotAt(tomorrow, 9, 30);
        BiometricAppointmentSlot untouched = slotAt(tomorrow, 10, 0);
        BiometricAppointmentSlot past = pastSlot(yesterday);
        booking("CONFIRMED", tomorrow.atTime(9, 0), booked.getSlotId());
        booking("CANCELLED", tomorrow.atTime(9, 30), cancelledHistory.getSlotId());

        List<BiometricSlotView> view = schedules.getSchedule("NIC", centre.getCentreId(), yesterday, tomorrow);
        var deletable = schedules.getDeletableSlotIds(view);

        assertTrue(deletable.contains(untouched.getSlotId()));
        assertFalse(deletable.contains(booked.getSlotId()));
        assertFalse(deletable.contains(cancelledHistory.getSlotId()));
        assertFalse(deletable.contains(past.getSlotId()));
        assertTrue(schedules.getDeletableSlotIds(List.of()).isEmpty());
    }

    // ---- fixtures ----

    private void publish(LocalDate date) {
        schedules.publishDateRange("NIC", centre.getCentreId(), date, date,
                LocalTime.of(9, 0), LocalTime.of(16, 30), 1, officerId);
        slotRepository.flush();
    }

    private BiometricAppointmentSlot slotAt(LocalDate date, int hour, int minute) {
        return slotRepository.findByCentreIdAndServiceTypeAndSlotDateAndSlotTime(
                centre.getCentreId(), "NIC", date, LocalTime.of(hour, minute)).orElseThrow();
    }

    /** Slots in the past cannot be published through the service, so this one is saved directly. */
    private BiometricAppointmentSlot pastSlot(LocalDate date) {
        BiometricAppointmentSlot slot = new BiometricAppointmentSlot();
        slot.setCentreId(centre.getCentreId());
        slot.setServiceType("NIC");
        slot.setSlotDate(date);
        slot.setSlotTime(LocalTime.of(9, 0));
        slot.setSlotCapacity(1);
        slot.setIsAvailable("Y");
        slot.setCreatedBy(officerId);
        return slotRepository.saveAndFlush(slot);
    }

    /** A citizen with a NIC application and a biometric booking in the given status. */
    private Appointment booking(String status, LocalDateTime scheduledAt, Integer slotId) {
        UserAccount user = newUser("citizen");
        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Example");
        citizen.setFamilyName("Citizen");
        citizens.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-NIC-OFF-" + unique());
        application.setCitizenId(user.getUserId());
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("APPOINTMENT_BOOKED");
        application.setDeclarationAccepted("Y");
        application = applications.saveAndFlush(application);

        Appointment appointment = new Appointment();
        appointment.setCitizenId(user.getUserId());
        appointment.setApplicationId(application.getApplicationId());
        appointment.setCentreId(centre.getCentreId());
        appointment.setBiometricSlotId(slotId);
        appointment.setAppointmentType("BIOMETRIC");
        appointment.setScheduledAt(scheduledAt);
        appointment.setAppointmentStatus(status);
        appointment.setReferenceNo("BIO-TEST-" + UUID.randomUUID());
        appointment.setQrCode(appointment.getReferenceNo());
        return appointments.saveAndFlush(appointment);
    }

    private UserAccount newUser(String kind) {
        UserAccount user = new UserAccount();
        user.setEmail(kind + "-" + unique() + "@example.com");
        user.setPhone("+947" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000)));
        user.setPasswordHash("test-only");
        return users.saveAndFlush(user);
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
