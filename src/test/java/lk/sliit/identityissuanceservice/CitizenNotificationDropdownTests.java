package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Controller.NotificationController;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.CitizenNotificationDto;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** The notification bell in the citizen navigation loads its short list from this endpoint. */
class CitizenNotificationDropdownTests {

    private NotificationService service;
    private MockMvc mockMvc;
    private MockHttpSession citizenSession;

    @BeforeEach
    void setUp() {
        service = mock(NotificationService.class);
        mockMvc = standaloneSetup(new NotificationController(service)).build();
        citizenSession = new MockHttpSession();
        citizenSession.setAttribute("loggedInUserId", 15);
        citizenSession.setAttribute("loggedInUserRole", "CITIZEN");
    }

    @Test
    void signedOutVisitorsAreRejected() throws Exception {
        mockMvc.perform(get("/citizen/notifications/recent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void officersCannotUseTheCitizenList() throws Exception {
        MockHttpSession officer = new MockHttpSession();
        officer.setAttribute("loggedInOfficerId", 70);

        mockMvc.perform(get("/citizen/notifications/recent").session(officer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dropdownGetsOnlyTheFiveNewestWithTheFullUnreadCount() throws Exception {
        List<CitizenNotificationDto> all = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            all.add(new CitizenNotificationDto(
                    i, "APPOINTMENT_CONFIRMED", "Message " + i, LocalDateTime.of(2026, 10, 10, 9, 0).minusHours(i), i > 2));
        }
        when(service.getCitizenNotifications(15)).thenReturn(all);

        mockMvc.perform(get("/citizen/notifications/recent").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.notifications.length()").value(5))
                .andExpect(jsonPath("$.notifications[0].notificationId").value(1))
                .andExpect(jsonPath("$.notifications[0].type").value("APPOINTMENT_CONFIRMED"))
                .andExpect(jsonPath("$.notifications[0].message").value("Message 1"))
                .andExpect(jsonPath("$.notifications[0].read").value(false))
                .andExpect(jsonPath("$.notifications[4].notificationId").value(5))
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    void emptyInboxGivesAnEmptyList() throws Exception {
        when(service.getCitizenNotifications(15)).thenReturn(List.of());

        mockMvc.perform(get("/citizen/notifications/recent").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifications.length()").value(0))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }
}
