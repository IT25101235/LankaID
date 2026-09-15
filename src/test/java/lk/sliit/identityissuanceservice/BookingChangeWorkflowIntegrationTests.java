package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.*;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.*;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Sms.SmsSender;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingChangeWorkflowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired AppointmentRepository appointments;
    @Autowired CentreRepository centres;
    @Autowired CitizenApplicationRepository applications;
    @Autowired UserAccountRepository users;
    @Autowired CitizenAccountRepository citizens;
    @Autowired PaymentRepository payments;
    @Autowired AppointmentChangePaymentRepository changePayments;
    @Autowired AppointmentService bookingService;
    @Autowired BiometricBookingChangeService biometricChanges;
    @Autowired WalkInAppointmentService walkIns;
    @Autowired WalkInBookingChangeService walkInChanges;
    @Autowired WalkInSlotRepository slots;
    @MockitoBean SmsSender sms;

    @Test void biometricCancellationReleasesCapacityAndRequiresPaidRebooking() throws Exception {
        Appointment appointment = biometric();
        var session = citizenSession(appointment);
        var date = appointment.getScheduledAt().toLocalDate();
        var centre = centres.findById(appointment.getCentreId()).orElseThrow();
        assertEquals(0, bookingService.remainingCapacity(centre, date, null));
        String token = open(appointment, session);
        mvc.perform(post(bioUrl(appointment) + "/cancel").session(session).param("token", token))
                .andExpect(status().is3xxRedirection());
        assertEquals("CANCELLED", appointment.getAppointmentStatus());
        assertEquals("APPROVED", applications.findById(appointment.getApplicationId()).orElseThrow().getCurrentStatus());
        assertEquals(1, bookingService.remainingCapacity(centre, date, null));
        assertEquals(0, changePayments.countByAppointmentId(appointment.getAppointmentId()));
        assertTrue(payments.findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(appointment.getAppointmentId(), "PAID").isPresent());

        mvc.perform(post("/citizen/appointments/biometric").session(session)
                        .param("applicationId", appointment.getApplicationId().toString()).param("centreId", centre.getCentreId().toString())
                        .param("appointmentDate", date.toString()).param("appointmentTime", "10:00"))
                .andExpect(flash().attribute("appointmentError", containsString("LKR 300")));
        assertEquals("CANCELLED", appointment.getAppointmentStatus());
        token = open(appointment, session);
        mvc.perform(card(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                .param("centreId", centre.getCentreId().toString()).param("appointmentDate", date.toString()).param("appointmentTime", "10:00")))
                .andExpect(redirectedUrl("/citizen/appointments/" + appointment.getReferenceNo() + "/pass"));
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
        assertEquals(0, bookingService.remainingCapacity(centre, date, null));
        assertEquals(1, changePayments.countByAppointmentId(appointment.getAppointmentId()));
        assertEquals(0, new BigDecimal("300").compareTo(changePayments.findAll().stream()
                .filter(p -> p.getAppointmentId().equals(appointment.getAppointmentId())).findFirst().orElseThrow().getAmount()));
    }

    @Test void biometricRescheduleMovesCapacityAndDuplicatePostDoesNotChargeTwice() throws Exception {
        Appointment appointment = biometric();
        var session = citizenSession(appointment);
        var oldDate = appointment.getScheduledAt().toLocalDate();
        var newDate = oldDate.plusDays(1);
        String token = open(appointment, session);
        for (int i = 0; i < 2; i++) {
            mvc.perform(card(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                    .param("centreId", appointment.getCentreId().toString()).param("appointmentDate", newDate.toString()).param("appointmentTime", "10:00")))
                    .andExpect(i == 0 ? status().is3xxRedirection() : status().isOk());
        }
        var centre = centres.findById(appointment.getCentreId()).orElseThrow();
        assertEquals(1, bookingService.remainingCapacity(centre, oldDate, null));
        assertEquals(0, bookingService.remainingCapacity(centre, newDate, null));
        assertEquals(1, changePayments.countByAppointmentId(appointment.getAppointmentId()));
    }

    @Test void biometricInvalidCardPreservesSelectionAndDoesNotChangeBooking() throws Exception {
        Appointment appointment = biometric();
        var session = citizenSession(appointment);
        var original = appointment.getScheduledAt();
        String token = open(appointment, session);
        mvc.perform(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                .param("centreId", appointment.getCentreId().toString()).param("appointmentDate", original.plusDays(1).toLocalDate().toString())
                .param("appointmentTime", "10:00").param("cardholderName", "Example Citizen").param("cardNumber", "123"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Enter exactly 16")))
                .andExpect(content().string(containsString("Example Citizen")))
                .andExpect(content().string(containsString(original.plusDays(1).toLocalDate().toString())));
        assertEquals(original, appointment.getScheduledAt());
        assertEquals(0, changePayments.countByAppointmentId(appointment.getAppointmentId()));
    }

    @Test void biometricLateCancellationAndWrongOwnerAreRejected() throws Exception {
        Appointment appointment = biometric();
        appointment.setScheduledAt(LocalDateTime.now().plusDays(2).withNano(0));
        appointments.saveAndFlush(appointment);
        var session = citizenSession(appointment);
        String token = open(appointment, session);
        mvc.perform(post(bioUrl(appointment) + "/cancel").session(session).param("token", token))
                .andExpect(flash().attribute("changeError", containsString("72 hours")));
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
        var stranger = new MockHttpSession();
        stranger.setAttribute("loggedInUserId", appointment.getCitizenId() + 100000);
        stranger.setAttribute("loggedInUserRole", "CITIZEN");
        mvc.perform(get(bioUrl(appointment)).session(stranger)).andExpect(redirectedUrl("/citizen/applications"));
    }

    @Test void biometricFullDestinationKeepsOldBookingAndPaymentUntouched() throws Exception {
        Appointment appointment = biometric();
        Appointment occupied = biometric();
        var session = citizenSession(appointment);
        String token = open(appointment, session);
        var original = appointment.getScheduledAt();
        mvc.perform(card(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                .param("centreId", occupied.getCentreId().toString()).param("appointmentDate", occupied.getScheduledAt().toLocalDate().toString())
                .param("appointmentTime", "10:00")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("fully booked")));
        assertEquals(original, appointment.getScheduledAt());
        assertEquals(0, changePayments.countByAppointmentId(appointment.getAppointmentId()));
    }

    @Test void walkInRecoveryRescheduleAndCancellationUpdateBothSlotCounts() throws Exception {
        Appointment appointment = walkIn();
        var session = verifiedRecovery(appointment.getVisitorPhone());
        var oldTime = appointment.getScheduledAt();
        var newSlot = slots.findByCentreIdAndSlotDateAndSlotTime(appointment.getCentreId(), oldTime.toLocalDate(), LocalTime.of(10, 0)).orElseThrow();
        assertFalse(walkIns.getBookableSlots().stream().anyMatch(s -> s.centreId().equals(appointment.getCentreId()) && s.slotTime().equals(LocalTime.of(9, 0))));
        String token = open(appointment, session);
        mvc.perform(card(post(walkUrl(appointment) + "/pay").session(session).param("token", token).param("slotId", newSlot.getSlotId().toString())))
                .andExpect(redirectedUrl("/walk-in/recover"));
        assertEquals(LocalTime.of(10, 0), appointment.getScheduledAt().toLocalTime());
        assertTrue(walkIns.getBookableSlots().stream().anyMatch(s -> s.centreId().equals(appointment.getCentreId()) && s.slotTime().equals(LocalTime.of(9, 0))));
        assertFalse(walkIns.getBookableSlots().stream().anyMatch(s -> s.slotId().equals(newSlot.getSlotId())));
        mvc.perform(get("/walk-in/recover").session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("Manage booking")));
        token = open(appointment, session);
        mvc.perform(post(walkUrl(appointment) + "/cancel").session(session).param("token", token))
                .andExpect(redirectedUrl("/walk-in/recover"));
        assertEquals("CANCELLED", appointment.getAppointmentStatus());
        assertTrue(walkIns.getBookableSlots().stream().anyMatch(s -> s.slotId().equals(newSlot.getSlotId())));
        assertEquals(1, changePayments.countByAppointmentId(appointment.getAppointmentId()));
    }

    @Test void walkInCannotBeChangedWithoutRecoveryOtpOrWithWrongPhone() throws Exception {
        Appointment appointment = walkIn();
        mvc.perform(get(walkUrl(appointment))).andExpect(redirectedUrl("/walk-in/recover"));
        var other = verifiedRecovery("+94770000001");
        mvc.perform(get(walkUrl(appointment)).session(other)).andExpect(redirectedUrl("/walk-in/recover"));
        var forged = BookingChangeRequest.prepare(appointment, other);
        mvc.perform(post(walkUrl(appointment) + "/cancel").session(other).param("token", forged.token()))
                .andExpect(flash().attribute("walkInError", containsString("verified number")));
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
    }

    @Test void walkInCheckedInBookingCannotBeCancelled() throws Exception {
        Appointment appointment = walkIn();
        appointment.setAppointmentStatus("CHECKED_IN");
        appointments.saveAndFlush(appointment);
        var session = verifiedRecovery(appointment.getVisitorPhone());
        String token = open(appointment, session);
        mvc.perform(post(walkUrl(appointment) + "/cancel").session(session).param("token", token))
                .andExpect(flash().attribute("walkInError", containsString("Only confirmed")));
        assertEquals("CHECKED_IN", appointment.getAppointmentStatus());
    }

    @Test void biometricRescheduleReleasesOldHalfHourAndBlocksOccupiedHalfHour() throws Exception {
        Appointment appointment = biometric();
        var centre = centres.findById(appointment.getCentreId()).orElseThrow();
        centre.setDailyCapacity(10); centres.saveAndFlush(centre);
        Appointment other = biometric();
        other.setCentreId(centre.getCentreId());
        other.setScheduledAt(appointment.getScheduledAt().withHour(11));
        appointments.saveAndFlush(other);
        var oldTime = appointment.getScheduledAt();
        var session = citizenSession(appointment);
        String token = open(appointment, session);
        mvc.perform(card(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                .param("centreId", centre.getCentreId().toString()).param("appointmentDate", oldTime.toLocalDate().toString())
                .param("appointmentTime", "11:00")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("time slot is fully booked")));
        assertEquals(oldTime, appointment.getScheduledAt());
        assertEquals(0, changePayments.countByAppointmentId(appointment.getAppointmentId()));
        token = open(appointment, session);
        mvc.perform(card(post(bioUrl(appointment) + "/pay").session(session).param("token", token)
                .param("centreId", centre.getCentreId().toString()).param("appointmentDate", oldTime.toLocalDate().toString())
                .param("appointmentTime", "10:00"))).andExpect(status().is3xxRedirection());
        assertEquals(1, bookingService.remainingSlotCapacity(centre, oldTime, null));
        assertEquals(0, bookingService.remainingSlotCapacity(centre, oldTime.withHour(10), null));
        mvc.perform(get("/citizen/appointments/biometric/availability").session(citizenSession(other))
                .param("applicationId", other.getApplicationId().toString()).param("centreId", centre.getCentreId().toString())
                .param("date", oldTime.toLocalDate().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.times", org.hamcrest.Matchers.hasItem("09:00")))
                .andExpect(jsonPath("$.times", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("10:00"))));
    }

    @Test void walkInFullDestinationDoesNotChargeOrReleaseOldSlot() throws Exception {
        Appointment appointment = walkIn();
        var centre = centres.findById(appointment.getCentreId()).orElseThrow();
        Appointment occupied = baseAppointment("WALK_IN", centre);
        occupied.setScheduledAt(appointment.getScheduledAt().withHour(10));
        appointments.saveAndFlush(occupied);
        var slot = slots.findByCentreIdAndSlotDateAndSlotTime(centre.getCentreId(),
                appointment.getScheduledAt().toLocalDate(), LocalTime.of(10, 0)).orElseThrow();
        var session = verifiedRecovery(appointment.getVisitorPhone());
        String token = open(appointment, session);
        mvc.perform(card(post(walkUrl(appointment) + "/pay").session(session).param("token", token)
                .param("slotId", slot.getSlotId().toString())))
                .andExpect(status().isOk()).andExpect(content().string(containsString("no longer available")));
        assertEquals(LocalTime.of(9, 0), appointment.getScheduledAt().toLocalTime());
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
        assertEquals(0, changePayments.countByAppointmentId(appointment.getAppointmentId()));
    }

    @Test void staleCancellationFormCannotChangeNewBooking() throws Exception {
        Appointment appointment = biometric();
        var session = citizenSession(appointment);
        String token = open(appointment, session);
        appointment.setScheduledAt(appointment.getScheduledAt().plusDays(1));
        appointments.saveAndFlush(appointment);
        mvc.perform(post(bioUrl(appointment) + "/cancel").session(session).param("token", token))
                .andExpect(flash().attribute("changeError", containsString("has changed")));
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
    }

    private Appointment biometric() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = new UserAccount();
        user.setEmail("change-" + unique + "@example.com");
        user.setPhone("+947" + String.format("%08d", Math.abs(unique.hashCode() % 100000000)));
        user.setPasswordHash("test-only");
        users.saveAndFlush(user);
        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Example"); citizen.setFamilyName("Citizen");
        citizens.saveAndFlush(citizen);
        CitizenApplication app = new CitizenApplication();
        app.setApplicationNo("LK-NIC-CHANGE-" + unique); app.setCitizenId(user.getUserId());
        app.setServiceType("NIC"); app.setApplicationKind("NEW"); app.setCurrentStatus("APPOINTMENT_BOOKED");
        app.setDeclarationAccepted("Y"); applications.saveAndFlush(app);
        Appointment appointment = baseAppointment("BIOMETRIC", centre());
        appointment.setCitizenId(user.getUserId()); appointment.setApplicationId(app.getApplicationId());
        appointments.saveAndFlush(appointment); pay(appointment);
        return appointment;
    }

    private Appointment walkIn() {
        Centre centre = centre();
        Appointment appointment = baseAppointment("WALK_IN", centre);
        appointment.setVisitorPhone("+947" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000)));
        appointment.setVisitorName("Example Visitor");
        appointment.setVisitorService("NIC_APPLICATION_HELP");
        appointments.saveAndFlush(appointment); pay(appointment);
        walkIns.publishDay(centre.getCentreId(), appointment.getScheduledAt().toLocalDate(), 1);
        return appointment;
    }
    private Centre centre() {
        Centre centre = new Centre(); centre.setCentreName("Booking test " + UUID.randomUUID().toString().substring(0, 8));
        centre.setCity("Colombo"); centre.setDistrict("Colombo"); centre.setDailyCapacity(1);
        return centres.saveAndFlush(centre);
    }
    private Appointment baseAppointment(String type, Centre centre) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentType(type); appointment.setCentreId(centre.getCentreId());
        appointment.setScheduledAt(LocalDate.now().plusDays(5).atTime(9, 0));
        appointment.setAppointmentStatus("CONFIRMED"); appointment.setReferenceNo("CHANGE-" + UUID.randomUUID());
        appointment.setQrCode(appointment.getReferenceNo()); return appointment;
    }
    private void pay(Appointment appointment) {
        Payment payment = new Payment(); payment.setAppointmentId(appointment.getAppointmentId());
        payment.setTransactionReference("TEST-" + UUID.randomUUID()); payment.setPaymentStatus("PAID");
        payment.setAmount(new BigDecimal("2000")); payment.setPaidAt(LocalDateTime.now()); payments.saveAndFlush(payment);
    }
    private MockHttpSession citizenSession(Appointment appointment) {
        var session = new MockHttpSession(); session.setAttribute("loggedInUserId", appointment.getCitizenId());
        session.setAttribute("loggedInUserRole", "CITIZEN"); return session;
    }
    private MockHttpSession verifiedRecovery(String phone) {
        var session = new MockHttpSession(); walkIns.startRecoveryVerification(phone, session);
        var message = ArgumentCaptor.forClass(String.class);
        verify(sms, atLeastOnce()).send(anyString(), message.capture(), anyInt());
        var matcher = java.util.regex.Pattern.compile("\\b([0-9]{6})\\b").matcher(message.getValue());
        assertTrue(matcher.find()); walkIns.verifyRecovery(matcher.group(1), session); return session;
    }
    private String open(Appointment appointment, MockHttpSession session) throws Exception {
        String url = "BIOMETRIC".equals(appointment.getAppointmentType()) ? bioUrl(appointment) : walkUrl(appointment);
        return (String) mvc.perform(get(url).session(session)).andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("changeToken");
    }
    private String bioUrl(Appointment a) { return "/citizen/appointments/" + a.getReferenceNo() + "/manage"; }
    private String walkUrl(Appointment a) { return "/walk-in/bookings/" + a.getReferenceNo() + "/manage"; }
    private MockHttpServletRequestBuilder card(MockHttpServletRequestBuilder request) {
        return request.param("cardholderName", "Example Citizen").param("cardNumber", "4111 1111 1111 1111")
                .param("expiry", YearMonth.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("MM/yy"))).param("cvv", "123");
    }
}
