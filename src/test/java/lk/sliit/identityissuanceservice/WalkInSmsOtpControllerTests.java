package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller.WalkInAppointmentController;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInSlotRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInPassService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the real controller and OTP/booking services without a database or live SMS. */
class WalkInSmsOtpControllerTests {
    private final SmsSender sender = mock(SmsSender.class);
    private final WalkInAppointmentRepository appointments = mock(WalkInAppointmentRepository.class);
    private final WalkInSlotRepository slots = mock(WalkInSlotRepository.class);
    private final WalkInAppointmentService service = new WalkInAppointmentService(appointments, slots,
            mock(CentreRepository.class), mock(AppointmentCheckInRepository.class),
            new SmsOtpService(sender, 180, 5, 60, 900),
            mock(lk.sliit.identityissuanceservice.Payment_Mgmt.Service.WalkInPaymentService.class));
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new WalkInAppointmentController(
            service, mock(WalkInPassService.class))).build();
    private final MockHttpSession session = new MockHttpSession();

    @Test void acceptsNineDigitsAndVerifiesTheCodeActuallySent() throws Exception {
        send("771234567");
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("+94771234567"), message.capture(), eq(180));
        String code = message.getValue().substring("LankaID (OTP) - ".length());
        assertFalse(service.isPhoneVerified(session));
        mvc.perform(post("/walk-in/otp/verify").session(session).param("otp", code))
                .andExpect(redirectedUrl("/walk-in/book"));
        assertTrue(service.isPhoneVerified(session));
        assertEquals("+94771234567", service.sessionPhone(session));
        mvc.perform(get("/walk-in/book").session(session))
                .andExpect(model().attribute("step", "BOOK"));
    }

    @Test void rejectsMalformedPhoneBeforeCallingGateway() throws Exception {
        mvc.perform(post("/walk-in/otp/send").session(session)
                        .param("visitorName", "Test Visitor").param("visitorPhone", "123456789"))
                .andExpect(redirectedUrl("/walk-in"))
                .andExpect(flash().attributeExists("walkInError"));
        verifyNoInteractions(sender);
        assertFalse(service.isPhoneVerified(session));
    }

    @Test void refusesBookingWithoutVerification() throws Exception {
        mvc.perform(post("/walk-in/book").session(session)
                        .param("visitorService", "NIC_APPLICATION_HELP").param("slotId", "1"))
                .andExpect(flash().attribute("walkInError", "Verify your phone number before booking."));
        verifyNoInteractions(appointments, slots);
        mvc.perform(get("/walk-in/book").session(session)).andExpect(redirectedUrl("/walk-in"));
    }

    @Test void gatewayFailureReturnsToPhoneFormWithoutVerification() throws Exception {
        doThrow(new IllegalArgumentException("SMS unavailable")).when(sender)
                .send(anyString(), anyString(), anyInt());
        mvc.perform(post("/walk-in/otp/send").session(session)
                        .param("visitorName", "Test Visitor").param("visitorPhone", "771234567"))
                .andExpect(redirectedUrl("/walk-in"))
                .andExpect(flash().attribute("walkInError", org.hamcrest.Matchers.startsWith("We couldn't send your code. Please try again in ")))
                .andExpect(flash().attributeExists("retryAfterSeconds"))
                .andExpect(flash().attribute("visitorPhone", "771234567"))
                .andExpect(flash().attribute("visitorName", "Test Visitor"));
        assertFalse(service.isPhoneVerified(session));
        mvc.perform(get("/walk-in/verify").session(session)).andExpect(redirectedUrl("/walk-in"));
    }

    @Test void resendEndpointEnforcesCooldown() throws Exception {
        send("771234567");
        mvc.perform(get("/walk-in/verify").session(session))
                .andExpect(model().attribute("retryAfterSeconds", org.hamcrest.Matchers.greaterThan(0L)));
        mvc.perform(post("/walk-in/otp/resend").session(session))
                .andExpect(redirectedUrl("/walk-in/verify"))
                .andExpect(flash().attributeExists("walkInError"));
        verify(sender, times(1)).send(anyString(), anyString(), anyInt());
    }

    private void send(String phone) throws Exception {
        mvc.perform(post("/walk-in/otp/send").session(session)
                        .param("visitorName", "Test Visitor").param("visitorPhone", phone))
                .andExpect(redirectedUrl("/walk-in/verify"));
    }
}
