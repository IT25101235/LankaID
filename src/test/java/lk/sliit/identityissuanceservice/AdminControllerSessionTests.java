package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.AdminController;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountDashboardDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountRowDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.DirectorDashboardStats;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.ApplicationFeeSettings;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.SystemMaintenance;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.CitizenAccountDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.DirectorDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ServiceCentreService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.PublicAnnouncementService;
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

import java.util.List;
import java.util.Optional;

class AdminControllerSessionTests {

    private MockMvc mockMvc;
    private OfficerAccountService officerAccountService;
    private ServiceCentreService serviceCentreService;
    private CitizenAccountDashboardService citizenAccountDashboardService;
    private DirectorDashboardService directorDashboardService;
    private DirectorDashboardStats overviewStats;
    private AdminAccountService adminAccountService;
    private SystemMaintenanceService maintenanceService;
    private ApplicationFeeSettingsService feeSettingsService;
    private PublicAnnouncementService publicAnnouncementService;

    @BeforeEach
    void setUp() {
        officerAccountService = mock(OfficerAccountService.class);
        serviceCentreService = mock(ServiceCentreService.class);
        citizenAccountDashboardService = mock(CitizenAccountDashboardService.class);
        directorDashboardService = mock(DirectorDashboardService.class);
        adminAccountService = mock(AdminAccountService.class);
        maintenanceService = mock(SystemMaintenanceService.class);
        feeSettingsService = mock(ApplicationFeeSettingsService.class);
        publicAnnouncementService = mock(PublicAnnouncementService.class);

        SystemMaintenance settings = new SystemMaintenance();
        settings.setMaintenanceId(1);
        settings.setMaintenanceActive(false);
        settings.setMaintenanceMessage(SystemMaintenanceService.DEFAULT_MESSAGE);
        when(maintenanceService.getSettings()).thenReturn(settings);

        ApplicationFeeSettings feeSettings = new ApplicationFeeSettings();
        feeSettings.setSettingsId(1);
        feeSettings.setNicFee(ApplicationFeeSettingsService.DEFAULT_NIC_FEE);
        feeSettings.setPassportFee(ApplicationFeeSettingsService.DEFAULT_PASSPORT_FEE);
        when(feeSettingsService.getSettings()).thenReturn(feeSettings);

        overviewStats = new DirectorDashboardStats(
                12, 5, 2, 1, 7, 5, 3, 2, 1, 1,
                List.of(), List.of(), List.of(), List.of(), List.of());
        when(citizenAccountDashboardService.getDashboard())
                .thenReturn(new CitizenAccountDashboardDto(List.of(), 0, 0, 0));
        when(directorDashboardService.getStats()).thenReturn(overviewStats);
        mockMvc = standaloneSetup(new AdminController(
                officerAccountService, serviceCentreService, citizenAccountDashboardService,
                directorDashboardService, adminAccountService, maintenanceService,
                feeSettingsService, publicAnnouncementService)).build();
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
        CitizenAccountRowDto citizen = new CitizenAccountRowDto(
                7, "CTZ-000007", "Nimali Perera", "nimali@example.com", 2, 1,
                "ACTIVE", "Active", true, true);
        when(citizenAccountDashboardService.getDashboard())
                .thenReturn(new CitizenAccountDashboardDto(List.of(citizen), 1, 1, 0));

        mockMvc.perform(get("/officer/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("officers_frontends/admin_dashboard"))
                .andExpect(model().attribute("loggedInAdminEmail", "admin@gov.lk"))
                .andExpect(model().attribute("citizens", List.of(citizen)))
                .andExpect(model().attribute("citizenAccountCount", 1L))
                .andExpect(model().attribute("registeredThisMonthCount", 1L))
                .andExpect(model().attribute("suspendedCitizenCount", 0L))
                .andExpect(model().attribute("activeOfficerCount", 0L))
                .andExpect(model().attribute("serviceCentreCount", 0))
                .andExpect(model().attribute("adminOverview", overviewStats));
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
                        .param("role", "nic-officer")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=users"))
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
                        .param("role", "csr")
                        .param("newPassword", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=users"))
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
                .andExpect(redirectedUrl("/officer/admin?section=users"))
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
    void adminPasswordIsRequiredToChangeApplicationFees() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        session.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(post("/officer/admin/settings")
                        .session(session)
                        .param("nicFee", "2500.00")
                        .param("passportFee", "12000.00")
                        .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=settings"))
                .andExpect(flash().attribute(
                        "settingsError", "The administrator password is incorrect."));

        org.mockito.Mockito.verify(feeSettingsService, org.mockito.Mockito.never())
                .updateFees(any(), any());
    }

    @Test
    void authenticatedAdminCanChangeApplicationFees() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        session.setAttribute("loggedInAdminEmail", "admin@gov.lk");
        UserAccount admin = new UserAccount();
        admin.setUserId(50);
        when(adminAccountService.authenticateAdmin("admin@gov.lk", "admin12345"))
                .thenReturn(Optional.of(admin));

        mockMvc.perform(post("/officer/admin/settings")
                        .session(session)
                        .param("nicFee", "2500.00")
                        .param("passportFee", "12000.00")
                        .param("password", "admin12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=settings"))
                .andExpect(flash().attribute(
                        "settingsMessage", "Application service fees updated successfully."));

        verify(feeSettingsService).updateFees("2500.00", "12000.00");
    }

    @Test
    void adminCanDeleteOfficer() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/officers/90/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=users"))
                .andExpect(flash().attribute("officerActionMessage", "Officer account deleted successfully."));

        verify(officerAccountService).removeOfficer(90);
    }

    @Test
    void adminCanDeleteCitizenLoginWhileServicePreservesIdentityHistory() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);

        mockMvc.perform(post("/officer/admin/citizens/44/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=citizens"))
                .andExpect(flash().attribute("citizenActionMessage",
                        "Citizen login deleted. Government identity records and application history were preserved."));

        verify(citizenAccountDashboardService).removeCitizenAccount(44);
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
    void adminCanEditChangeStatusAndDeleteAServiceCentre() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        when(serviceCentreService.toggleCentreStatus(8)).thenReturn("INACTIVE");

        mockMvc.perform(post("/officer/admin/centres/8/edit")
                        .session(session)
                        .param("centreName", "Kandy Main Centre")
                        .param("street", "Peradeniya Road")
                        .param("city", "Kandy")
                        .param("district", "Kandy")
                        .param("dailyCapacity", "150"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=centres"))
                .andExpect(flash().attribute("centreMessage", "Service centre updated successfully."));

        mockMvc.perform(post("/officer/admin/centres/8/status").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=centres"))
                .andExpect(flash().attribute("centreMessage", "Service centre deactivated successfully."));

        mockMvc.perform(post("/officer/admin/centres/8/delete").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=centres"))
                .andExpect(flash().attribute("centreMessage", "Service centre deleted successfully."));

        verify(serviceCentreService).updateCentre(
                8, "Kandy Main Centre", "Peradeniya Road", "Kandy", "Kandy", 150);
        verify(serviceCentreService).toggleCentreStatus(8);
        verify(serviceCentreService).removeCentre(8);
    }

    @Test
    void invalidOfficerDetailsKeepModalOpenAndPreserveEmail() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        mockMvc.perform(post("/officer/admin/officers").session(session)
                        .param("email", "officer@example.com")
                        .param("role", "csr").param("password", "weak"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("openOfficerModal", true))
                .andExpect(model().attributeHasFieldErrors("officerRegistration", "email", "password"))
                .andExpect(model().attribute("officerRegistration",
                        org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("officer@example.com"))));
        org.mockito.Mockito.verify(officerAccountService, org.mockito.Mockito.never()).createOfficer(any());
    }

    @Test
    void invalidOfficerEditKeepsSubmittedDetailsInTheModal() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInAdminId", 50);
        mockMvc.perform(post("/officer/admin/officers/12/edit").session(session)
                        .param("email", "officer@example.com")
                        .param("role", "csr").param("newPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("officers_frontends/admin_dashboard"))
                .andExpect(model().attribute("editOfficerId", 12))
                .andExpect(model().attributeHasFieldErrors("officerUpdate", "email", "newPassword"))
                .andExpect(model().attribute("officerUpdate",
                        org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("officer@example.com"))));
    }
}
