package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Controller.NotificationController;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.CitizenNotificationDto;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OfficerNotificationControllerTests {

    private NotificationService service;
    private MockMvc mockMvc;
    private MockHttpSession officerSession;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        mockMvc = standaloneSetup(new NotificationController(service)).build();
        officerSession = new MockHttpSession();
        officerSession.setAttribute("loggedInOfficerId", 70);
        officerSession.setAttribute("loggedInOfficerRole", "CSR");
    }

    @Test
    void officerInboxListsAdministratorMessages() throws Exception {
        CitizenNotificationDto message = new CitizenNotificationDto(
                8, "ADMIN_GENERAL", "Shift update\n\nPlease review it.", LocalDateTime.now(), false);
        when(service.getCitizenNotifications(70)).thenReturn(List.of(message));

        mockMvc.perform(get("/officer/notifications").session(officerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("officers_frontends/officer_notifications"))
                .andExpect(model().attribute("notifications", List.of(message)))
                .andExpect(model().attribute("unreadCount", 1L));
    }

    @Test
    void officerCanMarkMessageRead() throws Exception {
        mockMvc.perform(post("/officer/notifications/8/read").session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/notifications"));

        verify(service).markAsRead(70, 8);
    }

    @Test
    void unreadCountSupportsNavigationBadge() throws Exception {
        when(service.getUnreadCount(70)).thenReturn(6L);

        mockMvc.perform(get("/officer/notifications/unread-count").session(officerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(6));
    }
}
