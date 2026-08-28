package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Controller.CitizenApplicationController;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new CitizenApplicationController(applicationService)).build();
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
        when(applicationService.getEligibility(25))
                .thenReturn(new CitizenEligibilityDto(true, false, false, "200012345678", null));
        when(applicationService.getCitizenApplications(25)).thenReturn(List.of());

        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/citizen_dashboard"))
                .andExpect(model().attribute("hasNic", true))
                .andExpect(model().attribute("hasPassport", false))
                .andExpect(model().attribute("nicNumber", "200012345678"));
    }

    @Test
    void eligibleCitizenCanOpenNewNicForm() throws Exception {
        MockHttpSession session = citizenSession();
        when(applicationService.prepareNicForm(25, "new")).thenReturn(new NicApplicationForm());
        when(applicationService.getEligibility(25))
                .thenReturn(new CitizenEligibilityDto(false, false, false, null, null));

        mockMvc.perform(get("/citizen/applications/nic/new").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/nic_application_form"))
                .andExpect(model().attribute("applicationKind", "NEW"));
    }

    @Test
    void directNewNicUrlIsBlockedWhenCitizenAlreadyHasNic() throws Exception {
        MockHttpSession session = citizenSession();
        when(applicationService.prepareNicForm(25, "new"))
                .thenThrow(new IllegalArgumentException("You already have this identity document."));

        mockMvc.perform(get("/citizen/applications/nic/new").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andExpect(flash().attribute("applicationError", "You already have this identity document."));
    }

    private MockHttpSession citizenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserEmail", "citizen@example.com");
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }
}
