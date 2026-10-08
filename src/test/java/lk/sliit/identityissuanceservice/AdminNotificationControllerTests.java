package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.EmailDispatchResult;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.AdminNotificationService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.PublicAnnouncementService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Controller.AdminNotificationController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdminNotificationControllerTests {

    private AdminNotificationService service;
    private MockMvc mockMvc;
    private PublicAnnouncementService publicAnnouncementService;

    @BeforeEach
    void setUp() {
        service = mock(AdminNotificationService.class);
        publicAnnouncementService = mock(PublicAnnouncementService.class);
        mockMvc = standaloneSetup(new AdminNotificationController(service, publicAnnouncementService)).build();
    }

    @Test
    void inAppDispatchRequiresAdministratorSession() throws Exception {
        mockMvc.perform(post("/officer/admin/notifications/in-app"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void sendsInAppDispatchThroughService() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        when(service.sendInApp(any(), eq(50))).thenReturn(4);

        mockMvc.perform(post("/officer/admin/notifications/in-app")
                        .session(session)
                        .param("recipientType", "OFFICER")
                        .param("audience", "FILTERED")
                        .param("accountStatus", "ACTIVE")
                        .param("officerRole", "CSR")
                        .param("title", "Shift update")
                        .param("message", "Please review the updated counter schedule."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=notifications"))
                .andExpect(flash().attribute("notificationMessage", "In-app notification delivered to 4 recipient(s)."));

        verify(service).sendInApp(any(), eq(50));
    }

    @Test
    void emailDispatchReportsGatewayFailuresWithoutMixingInAppDelivery() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        when(service.sendEmail(any())).thenReturn(new EmailDispatchResult(3, 1));

        mockMvc.perform(post("/officer/admin/notifications/email")
                        .session(session)
                        .param("recipientType", "CITIZEN")
                        .param("audience", "ALL")
                        .param("title", "Service update")
                        .param("message", "The service is available."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=notifications"))
                .andExpect(flash().attribute("emailNotificationMessage",
                        "Official email sent to 3 recipient(s). 1 delivery attempt(s) failed."));
    }
}
