package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.AdminController;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ServiceCentreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerSessionTests {

    private MockMvc mockMvc;
    private OfficerAccountService officerAccountService;
    private ServiceCentreService serviceCentreService;

    @BeforeEach
    void setUp() {
        officerAccountService = mock(OfficerAccountService.class);
        serviceCentreService = mock(ServiceCentreService.class);
        mockMvc = standaloneSetup(new AdminController(officerAccountService, serviceCentreService)).build();
    }

    @Test
    void adminDashboardRedirectsWithoutAdminSession() throws Exception {
        mockMvc.perform(get("/officer/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminDashboardOpensWithAdminSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        session.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(get("/officer/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("officers_frontends/admin_dashboard"))
                .andExpect(model().attribute("loggedInAdminEmail", "admin@gov.lk"));
    }

    @Test
    void adminLogoutInvalidatesSessionAndReturnsToOfficerLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("activeTab", "officer"));
    }

    @Test
    void adminCanCreateOfficerUsingRequiredDatabaseFields() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/officers")
                        .session(session)
                        .param("email", "nic.officer@gov.lk")
                        .param("phone", "+94771234567")
                        .param("role", "nic-officer")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin"))
                .andExpect(flash().attribute("officerCreatedMessage", "Officer account created successfully."));

        verify(officerAccountService).createOfficer(any(OfficerRegistrationDto.class));
    }

    @Test
    void adminCanEditOfficer() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/officers/90/edit")
                        .session(session)
                        .param("email", "updated.officer@gov.lk")
                        .param("phone", "+94771112233")
                        .param("role", "csr")
                        .param("newPassword", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin"))
                .andExpect(flash().attribute("officerActionMessage", "Officer account updated successfully."));

        verify(officerAccountService).updateOfficer(any(Integer.class), any(OfficerUpdateDto.class));
    }

    @Test
    void adminCanDisableOfficer() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        when(officerAccountService.toggleOfficerStatus(90)).thenReturn("SUSPENDED");

        mockMvc.perform(post("/officer/admin/officers/90/status").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin"))
                .andExpect(flash().attribute("officerActionMessage", "Officer account disabled successfully."));

        verify(officerAccountService).toggleOfficerStatus(90);
    }

    @Test
    void adminCanDeleteOfficer() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/officers/90/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin"))
                .andExpect(flash().attribute("officerActionMessage", "Officer account deleted successfully."));

        verify(officerAccountService).removeOfficer(90);
    }

    @Test
    void onlyLoggedInAdminCanAddServiceCentre() throws Exception {
        mockMvc.perform(post("/officer/admin/centres")
                        .param("centreName", "Kandy Service Centre")
                        .param("street", "Peradeniya Road")
                        .param("city", "Kandy")
                        .param("district", "Kandy")
                        .param("dailyCapacity", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/centres")
                        .session(session)
                        .param("centreName", "Kandy Service Centre")
                        .param("street", "Peradeniya Road")
                        .param("city", "Kandy")
                        .param("district", "Kandy")
                        .param("dailyCapacity", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=centres"))
                .andExpect(flash().attribute("centreMessage", "Service centre added successfully."));

        verify(serviceCentreService).addCentre(
                "Kandy Service Centre", "Peradeniya Road", "Kandy", "Kandy", 120);
    }

    @Test
    void invalidOfficerDetailsKeepModalOpenAndPreserveEmail() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        mockMvc.perform(post("/officer/admin/officers").session(session)
                        .param("email", "officer@gov.lk").param("phone", "123")
                        .param("role", "csr").param("password", "weak"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("openOfficerModal", true))
                .andExpect(model().attributeHasFieldErrors("officerRegistration", "phone", "password"))
                .andExpect(model().attribute("officerRegistration",
                        org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("officer@gov.lk"))));
        org.mockito.Mockito.verify(officerAccountService, org.mockito.Mockito.never()).createOfficer(any());
    }

    @Test
    void invalidOfficerEditKeepsSubmittedDetailsInTheModal() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        mockMvc.perform(post("/officer/admin/officers/12/edit").session(session)
                        .param("email", "officer@gov.lk").param("phone", "123")
                        .param("role", "csr").param("newPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("officers_frontends/admin_dashboard"))
                .andExpect(model().attribute("editOfficerId", 12))
                .andExpect(model().attributeHasFieldErrors("officerUpdate", "phone", "newPassword"))
                .andExpect(model().attribute("officerUpdate",
                        org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("officer@gov.lk"))));
    }
}
