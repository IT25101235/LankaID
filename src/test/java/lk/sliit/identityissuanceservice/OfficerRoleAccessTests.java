package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.DirectorController;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.DirectorDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.AppointmentController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller.CitizenSupportController;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller.NicVerificationController;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Controller.PassportVerificationController;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationQueueService;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service.VerificationReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OfficerRoleAccessTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VerificationQueueService verificationQueueService = mock(VerificationQueueService.class);
        VerificationReviewService verificationReviewService = mock(VerificationReviewService.class);
        AppointmentService appointmentService = mock(AppointmentService.class);
        AppointmentPassService appointmentPassService = mock(AppointmentPassService.class);
        BiometricService biometricService = mock(BiometricService.class);
        PrintingExportService printingExportService = mock(PrintingExportService.class);
        DirectorDashboardService directorDashboardService = mock(DirectorDashboardService.class);
        when(printingExportService.getReadyApplications()).thenReturn(java.util.List.of());
        when(printingExportService.getPrintedApplications()).thenReturn(java.util.List.of());
        when(verificationQueueService.getNicQueue()).thenReturn(java.util.List.of());
        when(verificationQueueService.getPassportQueue()).thenReturn(java.util.List.of());

        mockMvc = standaloneSetup(
                new NicVerificationController(verificationQueueService, verificationReviewService),
                new PassportVerificationController(verificationQueueService, verificationReviewService),
                new BiometricController(appointmentService, biometricService),
                new AppointmentController(appointmentService, appointmentPassService),
                new CitizenSupportController(),
                new DirectorController(printingExportService, directorDashboardService)
        ).build();
    }

    @Test
    void nicOfficerCanOpenNicDashboardButNotPassportDashboard() throws Exception {
        MockHttpSession session = officerSession(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(get("/officer/nic/applications").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loggedInOfficerRoleLabel", "NIC Application Officer"))
                .andExpect(model().attribute("loggedInOfficerEmployeeNo", "EMP-NIC-001"))
                .andExpect(model().attribute("loggedInOfficerEmail", "officer@gov.lk"));

        mockMvc.perform(get("/officer/passport/applications").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void passportOfficerCannotOpenCsrDashboard() throws Exception {
        MockHttpSession session = officerSession(OfficerAccountService.PASSPORT_OFFICER);

        mockMvc.perform(get("/officer/csr").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void directorCanOpenAllOfficerDashboards() throws Exception {
        MockHttpSession session = officerSession(OfficerAccountService.DIRECTOR);
        String[] routes = {
                "/officer/director",
                "/officer/nic/applications",
                "/officer/passport/applications",
                "/officer/csr"
        };

        for (String route : routes) {
            mockMvc.perform(get(route).session(session))
                    .andExpect(status().isOk());
        }
    }

    private MockHttpSession officerSession(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", 70);
        session.setAttribute("loggedInOfficerRole", role);
        session.setAttribute(
                "loggedInOfficerRoleLabel",
                OfficerAccountService.DIRECTOR.equals(role) ? "Director" : "NIC Application Officer"
        );
        session.setAttribute(
                "loggedInOfficerEmployeeNo",
                OfficerAccountService.DIRECTOR.equals(role) ? "EMP-DIR-001" : "EMP-NIC-001"
        );
        session.setAttribute("loggedInOfficerEmail", "officer@gov.lk");
        return session;
    }
}
