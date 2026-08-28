package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenEligibilityDto;
import lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Controller.DocumentRenewalReplacementController;
import lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Dto.NicRenewalReplacementForm;
import lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Service.DocumentRenewalReplacementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class DocumentRenewalReplacementControllerTests {

    @Mock private DocumentRenewalReplacementService renewalService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new DocumentRenewalReplacementController(renewalService)).build();
    }

    @Test
    void renewalPageRequiresCitizenSession() throws Exception {
        mockMvc.perform(get("/citizen/applications/nic/renewal"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void existingNicHolderCanOpenRenewalForm() throws Exception {
        when(renewalService.prepareNicForm(25, "renewal"))
                .thenReturn(new NicRenewalReplacementForm());
        when(renewalService.getEligibility(25))
                .thenReturn(new CitizenEligibilityDto(true, false, false, "200012345678", null));

        mockMvc.perform(get("/citizen/applications/nic/renewal").session(citizenSession()))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/nic_application_form"))
                .andExpect(model().attribute("applicationKind", "RENEWAL"))
                .andExpect(model().attribute("existingDocumentNo", "200012345678"));
    }

    @Test
    void renewalUrlIsBlockedWhenNoExistingNicIsFound() throws Exception {
        when(renewalService.prepareNicForm(25, "renewal"))
                .thenThrow(new IllegalArgumentException("No existing identity document was found."));

        mockMvc.perform(get("/citizen/applications/nic/renewal").session(citizenSession()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andExpect(flash().attribute("applicationError", "No existing identity document was found."));
    }

    private MockHttpSession citizenSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }
}
