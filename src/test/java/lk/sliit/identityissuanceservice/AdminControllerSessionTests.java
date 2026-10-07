package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.AdminController;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.SystemMaintenance;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
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

import java.util.Optional;

class AdminControllerSessionTests {

    private MockMvc mockMvc;
    private OfficerAccountService officerAccountService;
    private AdminAccountService adminAccountService;
    private SystemMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        officerAccountService = mock(OfficerAccountService.class);
        adminAccountService = mock(AdminAccountService.class);
        maintenanceService = mock(SystemMaintenanceService.class);
        SystemMaintenance settings = new SystemMaintenance();
        settings.setMaintenanceId(1);
        settings.setMaintenanceActive(false);
        settings.setMaintenanceMessage(SystemMaintenanceService.DEFAULT_MESSAGE);
        when(maintenanceService.getSettings()).thenReturn(settings);
        mockMvc = standaloneSetup(new AdminController(
                officerAccountService,
                adminAccountService,
                maintenanceService
        )).build();
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
                        .param("password", "password123"))
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
    void adminPasswordIsRequiredToEnableMaintenanceMode() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        session.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        UserAccount admin = new UserAccount();
        admin.setUserId(50);
        when(adminAccountService.authenticateAdmin("admin@gov.lk", "admin12345"))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(post("/officer/admin/maintenance")
                        .session(session)
                        .param("enable", "true")
                        .param("message", "Scheduled maintenance")
                        .param("password", "admin12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=maintenance"))
                .andExpect(flash().attribute(
                        "maintenanceMessage",
                        "Maintenance mode activated. All non-admin operations are paused."
                ));

        verify(maintenanceService).updateMaintenance(true, "Scheduled maintenance");
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
}
