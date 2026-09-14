package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller.CitizenApplicationController;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewNicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class CitizenApplicationControllerTests {

    @Mock
    private CitizenApplicationService applicationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentDeliveryService deliveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(
                new CitizenApplicationController(applicationService, notificationService, deliveryService)
        ).build();
    }

    @Test
    void dashboardRequiresCitizenSession() throws Exception {
        mockMvc.perform(get("/citizen/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dashboardUsesRealEligibilityAndApplicationData() throws Exception {
        MockHttpSession session = citizenSession();
        CitizenApplication application = new CitizenApplication();
        application.setApplicationId(42);
        application.setApplicationNo("NIC-2026-000042");
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("UNDER_REVIEW");
        application.setCreatedAt(LocalDateTime.of(2026, 8, 29, 10, 30));

        when(applicationService.getEligibility(25))
                .thenReturn(new CitizenEligibilityDto(true, false, false, "200012345678", null));
        when(applicationService.getCitizenApplications(25)).thenReturn(List.of(application));
        when(deliveryService.getCitizenDeliveryMap(25)).thenReturn(Map.of());

        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/citizen_dashboard"))
                .andExpect(model().attribute("hasNic", true))
                .andExpect(model().attribute("hasPassport", false))
                .andExpect(model().attribute("nicNumber", "200012345678"))
                .andExpect(model().attribute("applications", List.of(application)));
    }

    @Test
    void eligibleCitizenCanOpenNewNicForm() throws Exception {
        MockHttpSession session = citizenSession();
        when(applicationService.prepareNewNicForm(25)).thenReturn(new NewNicApplicationForm());

        mockMvc.perform(get("/citizen/applications/nic/new").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/nic_application_form"))
                .andExpect(model().attribute("applicationKind", "NEW"));
    }

    @Test
    void directNewNicUrlIsBlockedWhenCitizenAlreadyHasNic() throws Exception {
        MockHttpSession session = citizenSession();
        when(applicationService.prepareNewNicForm(25))
                .thenThrow(new IllegalArgumentException("You already have this identity document."));

        mockMvc.perform(get("/citizen/applications/nic/new").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andExpect(flash().attribute("applicationError", "You already have this identity document."));
    }

    @Test
    void citizenCanDeleteDraftFromDashboard() throws Exception {
        CitizenApplication draft = new CitizenApplication();
        draft.setApplicationId(42);
        draft.setApplicationNo("LK-NIC-2026-000042");
        when(applicationService.deleteDraftApplication(25, 42)).thenReturn(draft);

        mockMvc.perform(post("/citizen/applications/42/delete").session(citizenSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andExpect(flash().attribute(
                        "applicationMessage",
                        "Draft LK-NIC-2026-000042 was deleted. You can start a new application when needed."
                ));
    }

    @Test
    void deleteDraftEndpointRequiresCitizenSession() throws Exception {
        mockMvc.perform(post("/citizen/applications/42/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private MockHttpSession citizenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserEmail", "citizen@example.com");
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }
}
