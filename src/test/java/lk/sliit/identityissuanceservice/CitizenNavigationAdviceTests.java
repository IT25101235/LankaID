package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.CitizenNavigationAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CitizenNavigationAdviceTests {

    @Test
    void exposesUnreadCountForSignedInCitizen() {
        NotificationService notifications = mock(NotificationService.class);
        CitizenSupportService support = mock(CitizenSupportService.class);
        when(notifications.getUnreadCount(25)).thenReturn(3L);
        when(support.getCitizenOpenCount(25)).thenReturn(2L);
        when(support.getCitizenRespondedCount(25)).thenReturn(1L);
        CitizenNavigationAdvice advice = new CitizenNavigationAdvice(notifications, support);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        ConcurrentModel model = new ConcurrentModel();

        advice.exposeCitizenNavigation(session, model);

        assertEquals(3L, model.getAttribute("unreadNotificationCount"));
        assertEquals(3L, model.getAttribute("activeSupportRequestCount"));
    }

    @Test
    void ignoresSessionsThatAreNotCitizens() {
        NotificationService notifications = mock(NotificationService.class);
        CitizenSupportService support = mock(CitizenSupportService.class);
        CitizenNavigationAdvice advice = new CitizenNavigationAdvice(notifications, support);
        ConcurrentModel model = new ConcurrentModel();

        advice.exposeCitizenNavigation(new MockHttpSession(), model);

        assertFalse(model.containsAttribute("unreadNotificationCount"));
        verifyNoInteractions(notifications);
        verifyNoInteractions(support);
    }
}
