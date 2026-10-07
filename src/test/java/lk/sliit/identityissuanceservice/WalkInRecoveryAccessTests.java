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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real OTP, service and HTTP authorization; database and SMS transport are mocked. */
class WalkInRecoveryAccessTests {
    private static final String PHONE = "+94771234567";
    private static final String REFERENCE = "WALK-2026-TEST1234";
    private final SmsSender sender = mock(SmsSender.class);
    private final WalkInAppointmentRepository appointments = mock(WalkInAppointmentRepository.class);
    private final WalkInSlotRepository slots = mock(WalkInSlotRepository.class);
    private final CentreRepository centres = mock(CentreRepository.class);
    private final Clock clock = mock(Clock.class);
    private final Instant start = Instant.parse("2026-09-14T10:00:00Z");
    private final AtomicReference<String> sentCode = new AtomicReference<>();
    private final MockHttpSession session = new MockHttpSession();
    private final WalkInPassService passes = mock(WalkInPassService.class);
    private WalkInAppointmentService service;
    private MockMvc mvc;

    @BeforeEach void setUp() {
        when(clock.instant()).thenReturn(start);
        doAnswer(call -> {
            String message = call.getArgument(1);
            sentCode.set(message.substring("LankaID (OTP) - ".length()));
            return null;
        }).when(sender).send(anyString(), anyString(), anyInt());
        service = new WalkInAppointmentService(appointments, slots, centres,
                mock(AppointmentCheckInRepository.class),
                new SmsOtpService(sender, 180, 5, 60, 900, clock),
                mock(lk.sliit.identityissuanceservice.Payment_Mgmt.Service.WalkInPaymentService.class));
        mvc = MockMvcBuilders.standaloneSetup(new WalkInAppointmentController(service, passes)).build();
        Centre centre = new Centre();
        centre.setCentreId(1);
        centre.setCentreName("Test centre");
        centre.setCity("Colombo");
        centre.setDistrict("Colombo");
        when(centres.findById(1)).thenReturn(Optional.of(centre));
        when(passes.createQrCode(anyString(), anyInt())).thenReturn(new byte[]{1, 2});
        when(passes.createPass(any())).thenReturn(new byte[]{3, 4});
    }

    @Test void phoneOnlyAndDirectLinksNeverExposeBookings() throws Exception {
        mvc.perform(get("/walk-in/recover").param("phone", PHONE))
                .andExpect(status().isOk()).andExpect(model().attributeDoesNotExist("walkIns"));
        assertPassDenied(new MockHttpSession(), REFERENCE);
        assertThrows(IllegalArgumentException.class, () -> service.recoverVerifiedBookings(session));
        verifyNoInteractions(appointments, passes, sender);
    }

    @Test void recoveryRequiresTheSentCodeAndScopesResultsToItsPhone() throws Exception {
        Appointment appointment = storedAppointment(REFERENCE, PHONE);
        when(appointments.findAllByVisitorPhoneInAndAppointmentTypeOrderByScheduledAtDesc(
                List.of(PHONE, "0771234567"), "WALK_IN")).thenReturn(List.of(appointment));
        sendRecovery("771234567");
        verifyNoInteractions(appointments);
        mvc.perform(get("/walk-in/recover/verify").session(session))
                .andExpect(model().attribute("step", "OTP"))
                .andExpect(model().attributeDoesNotExist("walkIns"));
        verifyRecovery();
        mvc.perform(get("/walk-in/recover").session(session))
                .andExpect(model().attribute("step", "RESULTS"))
                .andExpect(model().attributeExists("walkIns"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(appointments).findAllByVisitorPhoneInAndAppointmentTypeOrderByScheduledAtDesc(
                List.of(PHONE, "0771234567"), "WALK_IN");
        clearInvocations(appointments);
        mvc.perform(get("/walk-in/recover").session(session).param("phone", "+94772222222"))
                .andExpect(model().attributeDoesNotExist("walkIns"));
        verifyNoInteractions(appointments);
        assertPassAllowed(session, REFERENCE);
    }

    @Test void verifiedSessionCannotReadAnotherPersonsPassOrShareItsAccess() throws Exception {
        storedAppointment(REFERENCE, PHONE);
        storedAppointment("WALK-OTHER", "+94772222222");
        sendRecovery(PHONE);
        verifyRecovery();
        assertPassDenied(session, "WALK-OTHER");
        assertPassDenied(new MockHttpSession(), REFERENCE);
        verifyNoInteractions(passes);
    }

    @Test void wrongExpiredAndReusedCodesDoNotGrantAccess() throws Exception {
        sendRecovery(PHONE);
        String code = sentCode.get();
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/walk-in/recover/otp/verify").session(session).param("otp", wrong))
                    .andExpect(redirectedUrl("/walk-in/recover/verify"))
                    .andExpect(flash().attributeExists("walkInError"));
        }
        assertThrows(IllegalArgumentException.class, () -> service.verifyRecovery(code, session));
        assertPassDenied(session, REFERENCE);
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        sendRecovery(PHONE);
        when(clock.instant()).thenReturn(start.plusSeconds(240));
        assertThrows(IllegalArgumentException.class, () -> service.verifyRecovery(sentCode.get(), session));
        sendRecovery(PHONE);
        verifyRecovery();
        assertThrows(IllegalArgumentException.class, () -> service.verifyRecovery(sentCode.get(), session));
        verifyNoInteractions(appointments, passes);
    }

    @Test void recoveryAccessExpiresAndResetRevokesIt() throws Exception {
        storedAppointment(REFERENCE, PHONE);
        sendRecovery(PHONE);
        verifyRecovery();
        when(clock.instant()).thenReturn(start.plusSeconds(900));
        assertPassDenied(session, REFERENCE);
        mvc.perform(get("/walk-in/recover").session(session))
                .andExpect(model().attributeDoesNotExist("walkIns"));
        sendRecovery(PHONE);
        verifyRecovery();
        mvc.perform(post("/walk-in/recover/reset").session(session))
                .andExpect(redirectedUrl("/walk-in/recover"));
        assertPassDenied(session, REFERENCE);
        verifyNoInteractions(passes);
    }

    @Test void bookingVerificationDoesNotAuthorizeRecovery() throws Exception {
        service.startPhoneVerification("Visitor", PHONE, session);
        service.verifyPhone(sentCode.get(), session);
        assertTrue(service.isPhoneVerified(session));
        assertFalse(service.isRecoveryVerified(session));
        assertThrows(IllegalArgumentException.class, () -> service.verifyRecovery(sentCode.get(), session));
        assertPassDenied(session, REFERENCE);
        verifyNoInteractions(appointments, passes);
    }

    @Test void resendHasCooldownAndFailedSendDoesNotRevealBookings() throws Exception {
        sendRecovery(PHONE);
        mvc.perform(post("/walk-in/recover/otp/resend").session(session))
                .andExpect(flash().attributeExists("walkInError"));
        verify(sender, times(1)).send(anyString(), anyString(), anyInt());
        String oldCode = sentCode.get();
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        mvc.perform(post("/walk-in/recover/otp/resend").session(session))
                .andExpect(flash().attributeExists("walkInMessage"));
        // A replacement challenge must use its newly sent code.
        if (!oldCode.equals(sentCode.get())) {
            assertThrows(IllegalArgumentException.class, () -> service.verifyRecovery(oldCode, session));
        }
        verifyRecovery();
        when(clock.instant()).thenReturn(start.plusSeconds(120));
        doThrow(new IllegalArgumentException("Gateway unavailable")).when(sender)
                .send(anyString(), anyString(), anyInt());
        mvc.perform(post("/walk-in/recover/otp/resend").session(session))
                .andExpect(flash().attributeExists("walkInError"));
        assertFalse(service.isRecoveryVerified(session));
        assertPassDenied(session, REFERENCE);
        verifyNoInteractions(appointments, passes);
    }

    @Test void invalidPhoneIsRejectedWithoutSendingOrQuerying() throws Exception {
        mvc.perform(post("/walk-in/recover/otp/send").session(session).param("phone", "12345"))
                .andExpect(redirectedUrl("/walk-in/recover"))
                .andExpect(flash().attributeExists("walkInError"));
        verifyNoInteractions(sender, appointments);
    }

    @Test void freshBookingCanOpenOnlyItsOwnPassWithoutAnotherOtp() throws Exception {
        WalkInSlot slot = new WalkInSlot();
        slot.setSlotId(1); slot.setCentreId(1); slot.setSlotCapacity(2); slot.setIsAvailable("Y");
        slot.setSlotDate(LocalDate.now().plusDays(1)); slot.setSlotTime(LocalTime.of(10, 0));
        when(slots.findByIdForBooking(1)).thenReturn(Optional.of(slot));
        when(slots.findById(1)).thenReturn(Optional.of(slot));
        when(appointments.save(any(Appointment.class))).thenAnswer(call -> {
            Appointment appointment = call.getArgument(0);
            appointment.setAppointmentId(1);
            when(appointments.findByReferenceNoIgnoreCaseAndAppointmentType(
                    appointment.getReferenceNo(), "WALK_IN")).thenReturn(Optional.of(appointment));
            return appointment;
        });
        service.startPhoneVerification("Visitor", PHONE, session);
        service.verifyPhone(sentCode.get(), session);
        service.preparePayment("NIC_APPLICATION_HELP", 1, session);
        String reference = service.payAndBook(service.getPaymentDetails(session).token(), session)
                .appointment().getReferenceNo();
        assertFalse(service.isPhoneVerified(session));
        assertFalse(service.isRecoveryVerified(session));
        assertPassAllowed(session, reference);
        assertPassDenied(session, "WALK-OTHER");
        assertPassDenied(new MockHttpSession(), reference);
    }

    @Test void recoveryTemplatesRenderPhoneOtpAndVerifiedResults() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        MockServletContext servletContext = new MockServletContext();
        var exchange = JakartaServletWebApplication.buildApplication(servletContext).buildExchange(
                new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        for (String step : List.of("PHONE", "OTP", "RESULTS")) {
            var context = new WebContext(exchange, Locale.ENGLISH,
                    Map.of("step", step, "phone", PHONE, "retryAfterSeconds", 60,
                            "walkIns", List.of()));
            String html = engine.process("citizen_frontends/walkin_appointment_recovery", context);
            assertTrue(html.contains("Verify the mobile number"));
            if (step.equals("PHONE")) assertTrue(html.contains("/walk-in/recover/otp/send"));
            if (step.equals("OTP")) {
                assertTrue(html.contains("/walk-in/recover/otp/verify"));
                assertTrue(html.contains("/walk-in/recover/otp/resend"));
            }
            if (step.equals("RESULTS")) assertTrue(html.contains("Verified:"));
        }
    }

    private Appointment storedAppointment(String reference, String phone) {
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(1); appointment.setCentreId(1);
        appointment.setReferenceNo(reference); appointment.setVisitorPhone(phone);
        appointment.setVisitorService("NIC_APPLICATION_HELP");
        when(appointments.findByReferenceNoIgnoreCaseAndAppointmentType(reference, "WALK_IN"))
                .thenReturn(Optional.of(appointment));
        return appointment;
    }

    private void sendRecovery(String phone) throws Exception {
        mvc.perform(post("/walk-in/recover/otp/send").session(session).param("phone", phone))
                .andExpect(redirectedUrl("/walk-in/recover/verify"));
    }

    private void verifyRecovery() throws Exception {
        mvc.perform(post("/walk-in/recover/otp/verify").session(session).param("otp", sentCode.get()))
                .andExpect(redirectedUrl("/walk-in/recover"));
    }

    private void assertPassAllowed(MockHttpSession active, String reference) throws Exception {
        mvc.perform(get("/walk-in/pass/{reference}", reference).session(active))
                .andExpect(status().isOk()).andExpect(model().attributeExists("walkIn"))
                .andExpect(header().string("Cache-Control", "no-store"));
        for (String suffix : List.of("/qr", "/download")) {
            mvc.perform(get("/walk-in/pass/" + reference + suffix).session(active))
                    .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        }
    }

    private void assertPassDenied(MockHttpSession active, String reference) throws Exception {
        mvc.perform(get("/walk-in/pass/{reference}", reference).session(active))
                .andExpect(redirectedUrl("/walk-in/recover"))
                .andExpect(model().attributeDoesNotExist("walkIn"));
        for (String suffix : List.of("/qr", "/download")) {
            mvc.perform(get("/walk-in/pass/" + reference + suffix).session(active))
                    .andExpect(status().isForbidden()).andExpect(content().string(""));
        }
    }
}
