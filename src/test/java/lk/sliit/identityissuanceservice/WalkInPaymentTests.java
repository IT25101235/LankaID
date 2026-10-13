package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller.WalkInAppointmentController;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.WalkInSlot;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInPassService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Sms.SmsSender;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.WalkInPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WalkInPaymentTests {
    private final WalkInAppointmentRepository appointments = mock(WalkInAppointmentRepository.class);
    private final WalkInSlotRepository slots = mock(WalkInSlotRepository.class);
    private final CentreRepository centres = mock(CentreRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final SmsSender sms = mock(SmsSender.class);
    private final Clock clock = mock(Clock.class);
    private final Instant start = Instant.parse("2026-09-14T10:00:00Z");
    private final AtomicReference<String> code = new AtomicReference<>();
    private final AtomicReference<Appointment> savedAppointment = new AtomicReference<>();
    private final AtomicReference<Payment> savedPayment = new AtomicReference<>();
    private final MockHttpSession session = new MockHttpSession();
    private WalkInAppointmentService service;
    private MockMvc mvc;
    private WalkInSlot slot;

    @BeforeEach void setUp() {
        when(clock.instant()).thenReturn(start);
        doAnswer(call -> { code.set(((String) call.getArgument(1)).substring("LankaID (OTP) - ".length())); return null; })
                .when(sms).send(anyString(), anyString(), anyInt());
        service = new WalkInAppointmentService(appointments, slots, centres,
                mock(AppointmentCheckInRepository.class),
                new SmsOtpService(sms, 180, 5, 60, 900, clock), new WalkInPaymentService(payments));
        mvc = MockMvcBuilders.standaloneSetup(new WalkInAppointmentController(service, mock(WalkInPassService.class))).build();
        Centre centre = new Centre();
        centre.setCentreId(1); centre.setCentreName("Colombo centre");
        centre.setCity("Colombo"); centre.setDistrict("Colombo");
        when(centres.findById(1)).thenReturn(Optional.of(centre));
        slot = new WalkInSlot();
        slot.setSlotId(1); slot.setCentreId(1); slot.setSlotCapacity(1); slot.setIsAvailable("Y");
        slot.setSlotDate(LocalDate.now().plusDays(1)); slot.setSlotTime(LocalTime.of(10, 0));
        when(slots.findById(1)).thenReturn(Optional.of(slot));
        when(slots.findByIdForBooking(1)).thenReturn(Optional.of(slot));
        when(appointments.save(any())).thenAnswer(call -> {
            Appointment appointment = call.getArgument(0);
            appointment.setAppointmentId(10);
            savedAppointment.set(appointment);
            when(appointments.findByReferenceNoIgnoreCaseAndAppointmentType(appointment.getReferenceNo(), "WALK_IN"))
                    .thenReturn(Optional.of(appointment));
            return appointment;
        });
        when(payments.saveAndFlush(any())).thenAnswer(call -> {
            Payment payment = call.getArgument(0);
            assertEquals("PENDING_PAYMENT", savedAppointment.get().getAppointmentStatus());
            payment.setPaymentId(20);
            savedPayment.set(payment);
            return payment;
        });
    }

    @Test void selectingSlotDoesNotCreateBookingOrPayment() throws Exception {
        select("NIC_APPLICATION_HELP");
        assertNull(savedAppointment.get()); assertNull(savedPayment.get());
        verifyNoInteractions(payments);
        assertThrows(IllegalArgumentException.class, () -> service.getVisitorPass("WALK-GUESSED", session));
        mvc.perform(get("/walk-in/payment").session(session))
                .andExpect(status().isOk()).andExpect(model().attributeExists("paymentDetails"));
    }

    @Test void nicWalkInRequiresExactly1500AndCannotBePaidTwice() throws Exception {
        assertPaidBooking("NIC_APPLICATION_HELP");
    }

    @Test void passportWalkInUsesTheSame1500Fee() throws Exception {
        assertPaidBooking("PASSPORT_APPLICATION_HELP");
    }

    @Test void cannotSkipVerificationSelectionOrUseAnotherSessionsPayment() throws Exception {
        mvc.perform(post("/walk-in/payment").session(session).param("token", "fake"))
                .andExpect(flash().attributeExists("walkInError"));
        verifyPhone();
        assertThrows(IllegalArgumentException.class, () -> service.payAndBook("fake", validCard(), session));
        service.preparePayment("NIC_APPLICATION_HELP", 1, session);
        String token = service.getPaymentDetails(session).token();
        assertThrows(IllegalArgumentException.class,
                () -> service.payAndBook(token, validCard(), new MockHttpSession()));
        verifyNoInteractions(payments);
        assertNull(savedAppointment.get());
    }

    @Test void changedSelectionAndExpiredVerificationRejectPayment() throws Exception {
        select("NIC_APPLICATION_HELP");
        String oldToken = service.getPaymentDetails(session).token();
        service.preparePayment("PASSPORT_APPLICATION_HELP", 1, session);
        String currentToken = service.getPaymentDetails(session).token();
        assertThrows(IllegalArgumentException.class, () -> service.payAndBook(oldToken, validCard(), session));
        when(clock.instant()).thenReturn(start.plusSeconds(900));
        assertThrows(IllegalArgumentException.class, () -> service.payAndBook(currentToken, validCard(), session));
        verifyNoInteractions(payments);
        assertNull(savedAppointment.get());
    }

    @Test void closedOrFullSlotIsRejectedBeforePayment() throws Exception {
        select("NIC_APPLICATION_HELP");
        String token = service.getPaymentDetails(session).token();
        slot.setIsAvailable("N");
        assertThrows(IllegalArgumentException.class, () -> service.payAndBook(token, validCard(), session));
        slot.setIsAvailable("Y");
        when(appointments.countByAppointmentTypeAndCentreIdAndScheduledAtAndAppointmentStatusIn(
                anyString(), anyInt(), any(), any())).thenReturn(1L);
        assertThrows(IllegalArgumentException.class, () -> service.payAndBook(token, validCard(), session));
        verifyNoInteractions(payments);
        assertNull(savedAppointment.get());
    }

    @Test void paymentFailureDoesNotConfirmBookingOrGrantQrAccess() throws Exception {
        select("NIC_APPLICATION_HELP");
        String token = service.getPaymentDetails(session).token();
        doThrow(new DataIntegrityViolationException("Database failure")).when(payments).saveAndFlush(any());
        mvc.perform(post("/walk-in/payment").session(session).param("token", token)
                        .param("cardholderName", "Test Visitor")
                        .param("cardNumber", "4111 1111 1111 1111")
                        .param("expiry", "12/99").param("cvv", "123"))
                .andExpect(redirectedUrl("/walk-in/payment"))
                .andExpect(flash().attribute("walkInError", "Payment could not be saved. Please try again."));
        assertEquals("PENDING_PAYMENT", savedAppointment.get().getAppointmentStatus());
        assertThrows(IllegalArgumentException.class,
                () -> service.getVisitorPass(savedAppointment.get().getReferenceNo(), session));
        assertTrue(service.isPhoneVerified(session));
    }

    @Test void invalidCardDoesNotCreateAppointmentOrPayment() throws Exception {
        select("NIC_APPLICATION_HELP");
        String token = service.getPaymentDetails(session).token();
        mvc.perform(post("/walk-in/payment").session(session).param("token", token)
                        .param("cardholderName", "Test Visitor")
                        .param("cardNumber", "4111 1111 1111 1112")
                        .param("expiry", "12/99").param("cvv", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/walkin_appointment_booking"))
                .andExpect(model().attributeExists("cardErrors", "walkInError"));
        assertNull(savedAppointment.get());
        verifyNoInteractions(payments);
    }

    private void assertPaidBooking(String visitorService) throws Exception {
        select(visitorService);
        String token = service.getPaymentDetails(session).token();
        mvc.perform(post("/walk-in/payment").session(session).param("token", token)
                        .param("amount", "1.00").param("slotId", "999")
                        .param("cardholderName", "Test Visitor")
                        .param("cardNumber", "4111 1111 1111 1111")
                        .param("expiry", "12/99").param("cvv", "123"))
                .andExpect(redirectedUrlPattern("/walk-in/pass/*"));
        assertEquals(new BigDecimal("1500.00"), savedPayment.get().getAmount());
        assertEquals("PAID", savedPayment.get().getPaymentStatus());
        assertEquals(10, savedPayment.get().getAppointmentId());
        assertEquals("CARD", savedPayment.get().getPaymentMethod());
        assertEquals("VISA", savedPayment.get().getCardBrand());
        assertEquals("1111", savedPayment.get().getCardLastFour());
        assertEquals("Test Visitor", savedPayment.get().getCardholderName());
        assertEquals("12/99", savedPayment.get().getCardExpiry());
        assertNotNull(savedPayment.get().getPaidAt());
        assertEquals("CONFIRMED", savedAppointment.get().getAppointmentStatus());
        assertEquals(visitorService, savedAppointment.get().getVisitorService());
        assertNotNull(service.getVisitorPass(savedAppointment.get().getReferenceNo(), session));
        mvc.perform(post("/walk-in/payment").session(session).param("token", token))
                .andExpect(flash().attributeExists("walkInError"));
        verify(payments, times(1)).saveAndFlush(any());
    }

    private void select(String visitorService) throws Exception {
        verifyPhone();
        mvc.perform(post("/walk-in/book").session(session)
                        .param("visitorService", visitorService).param("slotId", "1"))
                .andExpect(redirectedUrl("/walk-in/payment"));
    }

    private void verifyPhone() {
        service.startPhoneVerification("Test visitor", "771234567", session);
        service.verifyPhone(code.get(), session);
    }

    private CardPaymentForm validCard() {
        CardPaymentForm card = new CardPaymentForm();
        card.setCardholderName("Test Visitor");
        card.setCardNumber("4111 1111 1111 1111");
        card.setExpiry("12/99");
        card.setCvv("123");
        return card;
    }
}
