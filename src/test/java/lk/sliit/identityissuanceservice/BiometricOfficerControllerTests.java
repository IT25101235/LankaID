package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.AppointmentController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricFormToken;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricScheduleController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricScheduleService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Officer biometric desk, schedule page and citizen booking controllers: role rules, form tokens and
 * the friendly message shown when the database rejects a save. The services are mocked, so no database
 * is needed.
 */
class BiometricOfficerControllerTests {

    private static final String TOKEN = "test-token";
    private static final String RELOAD = "Reload the page and try again.";

    private AppointmentService appointmentService;
    private BiometricService biometricService;
    private BiometricScheduleService scheduleService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        appointmentService = mock(AppointmentService.class);
        biometricService = mock(BiometricService.class);
        scheduleService = mock(BiometricScheduleService.class);
        mvc = standaloneSetup(
                new BiometricController(appointmentService, biometricService),
                new BiometricScheduleController(scheduleService),
                new AppointmentController(appointmentService, mock(AppointmentPassService.class))
        ).build();
    }

    // ---- Reschedule a missed appointment ----

    @Test
    void aSlotIdThatIsNotANumberGetsTheFriendlyMessageInsteadOfAnErrorPage() throws Exception {
        mvc.perform(withToken(post("/officer/{service}/biometrics/{reference}/reschedule-missed", "nic", "BIO-1")
                                .param("slotId", "abc"), OfficerAccountService.NIC_OFFICER))
                .andExpect(redirectedUrl("/officer/nic/biometrics?reference=BIO-1"))
                .andExpect(flash().attribute("biometricError", "Select an available date and time."));
        verify(appointmentService, never()).rescheduleMissed(any(), any(), any(), any());
    }

    @Test
    void rescheduleMissedNeedsTheFormToken() throws Exception {
        mvc.perform(post("/officer/nic/biometrics/BIO-1/reschedule-missed")
                        .session(officer(OfficerAccountService.NIC_OFFICER)).param("formToken", "forged"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("biometricError", RELOAD));
        mvc.perform(post("/officer/nic/biometrics/BIO-1/reschedule-missed")
                        .session(officer(OfficerAccountService.NIC_OFFICER)))
                .andExpect(flash().attribute("biometricError", RELOAD));
        verify(appointmentService, never()).rescheduleMissed(any(), any(), any(), any());
    }

    @Test
    void rescheduleMissedMovesTheAppointmentAndEncodesTheReference() throws Exception {
        Appointment moved = new Appointment();
        moved.setScheduledAt(LocalDateTime.of(2030, 1, 15, 9, 0));
        when(appointmentService.rescheduleMissed("BIO 2026", "NIC", 70, 42))
                .thenReturn(new OfficerBiometricDetails(moved, null, null, null, "Citizen", false, false));

        mvc.perform(withToken(post("/officer/{service}/biometrics/{reference}/reschedule-missed", "nic", "BIO 2026").param("slotId", "42"),
                        OfficerAccountService.NIC_OFFICER))
                .andExpect(redirectedUrl("/officer/nic/biometrics?reference=BIO+2026"))
                .andExpect(flash().attribute("biometricMessage", containsString("moved to")));
        verify(appointmentService).rescheduleMissed("BIO 2026", "NIC", 70, 42);
    }

    @Test
    void checkInAndCompleteStillRejectABadToken() throws Exception {
        for (String action : new String[]{"check-in", "complete"}) {
            mvc.perform(post("/officer/nic/biometrics/BIO-1/" + action)
                            .session(officer(OfficerAccountService.NIC_OFFICER)).param("formToken", "forged"))
                    .andExpect(flash().attribute("biometricError", RELOAD));
        }
        verify(appointmentService, never()).checkIn(any(), any(), any(), any());
        verify(biometricService, never()).completeCollection(any(), any(), any());
    }

    @Test
    void checkInPassesTheMethodAndOfficerToTheService() throws Exception {
        mvc.perform(withToken(post("/officer/nic/biometrics/BIO-1/check-in").param("checkInMethod", "QR"),
                        OfficerAccountService.NIC_OFFICER))
                .andExpect(redirectedUrl("/officer/nic/biometrics?reference=BIO-1"))
                .andExpect(flash().attribute("biometricMessage", "Citizen checked in successfully."));
        verify(appointmentService).checkIn("BIO-1", "NIC", 70, "QR");
    }

    @Test
    void anOfficerOfTheOtherServiceIsSentToLogin() throws Exception {
        for (String action : new String[]{"check-in", "reschedule-missed", "complete"}) {
            mvc.perform(withToken(post("/officer/nic/biometrics/BIO-1/" + action),
                            OfficerAccountService.PASSPORT_OFFICER))
                    .andExpect(redirectedUrl("/login"));
        }
        verify(appointmentService, never()).checkIn(any(), any(), any(), any());
        verify(appointmentService, never()).rescheduleMissed(any(), any(), any(), any());
        verify(biometricService, never()).completeCollection(any(), any(), any());
    }

    @Test
    void aDatabaseConflictShowsAFriendlyMessageOnTheBiometricDesk() throws Exception {
        when(appointmentService.checkIn(any(), any(), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));
        when(appointmentService.rescheduleMissed(any(), any(), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));
        doThrow(new OptimisticLockingFailureException("conflict"))
                .when(biometricService).completeCollection(any(), any(), any());

        for (String action : new String[]{"check-in", "reschedule-missed", "complete"}) {
            mvc.perform(withToken(post("/officer/nic/biometrics/BIO-1/" + action),
                            OfficerAccountService.NIC_OFFICER))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("biometricError", containsString("could not be saved")));
        }
    }

    // ---- Schedule page ----

    @Test
    void publishCloseAndSlotNeedTheFormToken() throws Exception {
        MockHttpSession session = officer(OfficerAccountService.NIC_OFFICER);
        mvc.perform(publish(session).param("formToken", "forged"))
                .andExpect(flash().attribute("scheduleError", RELOAD));
        mvc.perform(close(session).param("formToken", "forged"))
                .andExpect(flash().attribute("scheduleError", RELOAD));
        mvc.perform(slot(session).param("formToken", "forged"))
                .andExpect(flash().attribute("scheduleError", RELOAD));

        verify(scheduleService, never()).publishDateRange(any(), any(), any(), any(), any(), any(), any(), any());
        verify(scheduleService, never()).closeDateRange(any(), any(), any(), any());
        verify(scheduleService, never()).updateSlot(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void anOfficerCannotManageTheOtherServicesSchedule() throws Exception {
        MockHttpSession passportOfficer = officer(OfficerAccountService.PASSPORT_OFFICER);
        mvc.perform(publish(passportOfficer).param("formToken", TOKEN)).andExpect(redirectedUrl("/login"));
        mvc.perform(close(passportOfficer).param("formToken", TOKEN)).andExpect(redirectedUrl("/login"));
        mvc.perform(slot(passportOfficer).param("formToken", TOKEN)).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/officer/nic/appointments/schedule").session(passportOfficer))
                .andExpect(redirectedUrl("/login"));
        verify(scheduleService, never()).publishDateRange(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void theDirectorCanPublishForEitherService() throws Exception {
        when(scheduleService.publishDateRange(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        mvc.perform(publish(officer(OfficerAccountService.DIRECTOR)).param("formToken", TOKEN))
                .andExpect(flash().attribute("scheduleMessage", containsString("published for NIC")));
    }

    @Test
    void anUnknownServiceIsNotFound() throws Exception {
        mvc.perform(get("/officer/other/appointments/schedule").session(officer(OfficerAccountService.DIRECTOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aDatabaseConflictShowsAFriendlyMessageOnTheSchedulePage() throws Exception {
        when(scheduleService.publishDateRange(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));
        when(scheduleService.closeDateRange(any(), any(), any(), any()))
                .thenThrow(new OptimisticLockingFailureException("conflict"));
        doThrow(new OptimisticLockingFailureException("conflict"))
                .when(scheduleService).updateSlot(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());

        MockHttpSession session = officer(OfficerAccountService.NIC_OFFICER);
        mvc.perform(publish(session).param("formToken", TOKEN))
                .andExpect(flash().attribute("scheduleError", containsString("could not be saved")));
        mvc.perform(close(session).param("formToken", TOKEN))
                .andExpect(flash().attribute("scheduleError", containsString("could not be closed")));
        mvc.perform(slot(session).param("formToken", TOKEN))
                .andExpect(flash().attribute("scheduleError", containsString("could not be saved")));
    }

    // ---- Citizen booking ----

    @Test
    void aDatabaseConflictWhileBookingSendsTheCitizenBackToTheBookingPage() throws Exception {
        when(appointmentService.bookAppointment(5, 9, 3)).thenThrow(new OptimisticLockingFailureException("conflict"));
        MockHttpSession citizen = new MockHttpSession();
        citizen.setAttribute("loggedInUserId", 5);
        citizen.setAttribute("loggedInUserRole", "CITIZEN");
        citizen.setAttribute(BiometricFormToken.KEY, TOKEN);

        mvc.perform(post("/citizen/appointments/biometric").session(citizen)
                        .param("applicationId", "9").param("slotId", "3").param("formToken", TOKEN))
                .andExpect(redirectedUrl("/citizen/appointments/biometric?applicationId=9"))
                .andExpect(flash().attribute("appointmentError", containsString("could not be saved")));
    }

    // ---- helpers ----

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String role) {
        return request.session(officer(role)).param("formToken", TOKEN);
    }

    private MockHttpServletRequestBuilder publish(MockHttpSession session) {
        return post("/officer/nic/appointments/schedule/publish").session(session)
                .param("centreId", "1").param("startDate", "2030-01-01").param("endDate", "2030-01-02")
                .param("openingTime", "09:00").param("closingTime", "16:30").param("capacity", "1");
    }

    private MockHttpServletRequestBuilder close(MockHttpSession session) {
        return post("/officer/nic/appointments/schedule/close").session(session)
                .param("centreId", "1").param("startDate", "2030-01-01").param("endDate", "2030-01-02");
    }

    private MockHttpServletRequestBuilder slot(MockHttpSession session) {
        return post("/officer/nic/appointments/schedule/slot").session(session)
                .param("slotId", "4").param("capacity", "2").param("centreId", "1").param("month", "2030-01");
    }

    private MockHttpSession officer(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", 70);
        session.setAttribute("loggedInOfficerRole", role);
        session.setAttribute(BiometricFormToken.KEY, TOKEN);
        return session;
    }
}
