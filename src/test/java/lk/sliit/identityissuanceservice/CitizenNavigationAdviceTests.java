package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
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
        when(notifications.getUnreadCount(25)).thenReturn(3L);
        CitizenNavigationAdvice advice = new CitizenNavigationAdvice(notifications);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        ConcurrentModel model = new ConcurrentModel();

        advice.exposeCitizenNavigation(session, model);

        assertEquals(3L, model.getAttribute("unreadNotificationCount"));
    }

    @Test
    void ignoresSessionsThatAreNotCitizens() {
        NotificationService notifications = mock(NotificationService.class);
        CitizenNavigationAdvice advice = new CitizenNavigationAdvice(notifications);
        ConcurrentModel model = new ConcurrentModel();

        advice.exposeCitizenNavigation(new MockHttpSession(), model);

        assertFalse(model.containsAttribute("unreadNotificationCount"));
        verifyNoInteractions(notifications);
    }
}
