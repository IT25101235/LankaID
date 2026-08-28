package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.DirectorController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.AppointmentController;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller.BiometricController;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.Mockito.mock;

class SystemAdminOfficerAccessTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VerificationQueueService verificationQueueService = mock(VerificationQueueService.class);
        VerificationReviewService verificationReviewService = mock(VerificationReviewService.class);

        mockMvc = standaloneSetup(
                new NicVerificationController(verificationQueueService, verificationReviewService),
                new PassportVerificationController(verificationQueueService, verificationReviewService),
                new BiometricController(),
                new AppointmentController(),
                new CitizenSupportController(),
                new DirectorController()
        ).build();
    }

    @Test
    void systemAdminSessionCannotOpenOtherOfficerPages() throws Exception {
        String[] otherOfficerRoutes = {
                "/officer/nic/applications",
                "/officer/nic/applications/processed",
                "/officer/nic/applications/NIC-2026-001",
                "/officer/nic/appointments",
                "/officer/nic/biometrics",
                "/officer/passport/applications",
                "/officer/passport/applications/processed",
                "/officer/passport/applications/PASS-2026-001",
                "/officer/passport/appointments",
                "/officer/passport/biometrics",
                "/officer/csr",
                "/officer/csr/complaints/CMP-2026-001",
                "/officer/csr/walk-ins",
                "/officer/csr/walk-ins/register",
                "/officer/director",
                "/officer/director/printing"
        };

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 50);
        adminSession.setAttribute("loggedInAdminRole", "SYSTEM_ADMIN");

        for (String route : otherOfficerRoutes) {
            mockMvc.perform(get(route).session(adminSession))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/officer/admin"));
        }
    }
}
