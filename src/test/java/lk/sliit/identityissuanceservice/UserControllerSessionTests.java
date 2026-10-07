package lk.sliit.identityissuanceservice;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.SystemMaintenance;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.UserController;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerSessionTests {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private OfficerAccountService officerAccountService;

    @Mock
    private SystemMaintenanceService maintenanceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(
                userAccountService,
                adminAccountService,
                officerAccountService,
                maintenanceService
        )).build();
    }

    @Test
    void successfulLoginCreatesCitizenSession() throws Exception {
        UserAccount account = new UserAccount();
        account.setUserId(25);
        account.setEmail("citizen@example.com");

        when(userAccountService.authenticateCitizen(
                "citizen@example.com",
                "password123"
        )).thenReturn(Optional.of(account));

        HttpSession session = mockMvc.perform(post("/login")
                        .param("email", "citizen@example.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session);
        assertEquals(25, session.getAttribute("loggedInUserId"));
        assertEquals("citizen@example.com", session.getAttribute("loggedInUserEmail"));
        assertEquals(1800, session.getMaxInactiveInterval());
    }

    @Test
    void maintenanceModeBlocksCitizenLogin() throws Exception {
        when(maintenanceService.isMaintenanceActive()).thenReturn(true);
        SystemMaintenance settings = new SystemMaintenance();
        settings.setMaintenanceActive(true);
        settings.setMaintenanceMessage("Scheduled maintenance");
        when(maintenanceService.getSettings()).thenReturn(settings);

        mockMvc.perform(post("/login")
                        .param("email", "citizen@example.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("login_portal"))
                .andExpect(model().attributeHasErrors("loginRequest"))
                .andExpect(model().attribute("maintenanceActive", true));
    }

    @Test
    void dashboardRedirectsWhenCitizenIsNotLoggedIn() throws Exception {
        mockMvc.perform(get("/citizen/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void dashboardOpensWhenCitizenIsLoggedIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", 25);
        session.setAttribute("loggedInUserEmail", "citizen@example.com");

        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/citizen_dashboard"))
                .andExpect(model().attribute("loggedInUserEmail", "citizen@example.com"));
    }

    @Test
    void successfulAdminLoginCreatesAdminSession() throws Exception {
        UserAccount admin = new UserAccount();
        admin.setUserId(50);
        admin.setEmail("admin@gov.lk");

        when(adminAccountService.authenticateAdmin("admin@gov.lk", "admin12345"))
                .thenReturn(Optional.of(admin));

        HttpSession session = mockMvc.perform(post("/login/officer")
                        .param("role", "admin")
                        .param("email", "admin@gov.lk")
                        .param("password", "admin12345"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session);
        assertEquals(50, session.getAttribute("loggedInAdminId"));
        assertEquals("admin@gov.lk", session.getAttribute("loggedInAdminEmail"));
        assertEquals("SYSTEM_ADMIN", session.getAttribute("loggedInAdminRole"));
    }

    @Test
    void adminEmailCannotLoginAsAnotherOfficerRole() throws Exception {
        when(adminAccountService.isSystemAdminEmail("admin@gov.lk")).thenReturn(true);

        mockMvc.perform(post("/login/officer")
                        .param("role", "nic-officer")
                        .param("email", "admin@gov.lk")
                        .param("password", "admin12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("login_portal"))
                .andExpect(model().attributeHasErrors("officerLoginRequest"));
    }

    @Test
    void officerLoginCreatesRoleSessionAndOpensOwnDashboard() throws Exception {
        OfficerDetailsDto officer = new OfficerDetailsDto(
                70,
                "EMP-NIC-001",
                "nic.officer@gov.lk",
                "+94771234567",
                OfficerAccountService.NIC_OFFICER,
                "NIC Application Officer",
                "nic-officer",
                "ACTIVE"
        );

        when(officerAccountService.dashboardForRoleSelection("nic-officer"))
                .thenReturn("/officer/nic/applications");
        when(officerAccountService.authenticateOfficer(
                "nic.officer@gov.lk",
                "password123",
                "nic-officer"
        )).thenReturn(Optional.of(officer));

        HttpSession session = mockMvc.perform(post("/login/officer")
                        .param("role", "nic-officer")
                        .param("email", "nic.officer@gov.lk")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session);
        assertEquals(70, session.getAttribute("loggedInOfficerId"));
        assertEquals(OfficerAccountService.NIC_OFFICER, session.getAttribute("loggedInOfficerRole"));
        assertEquals("NIC Application Officer", session.getAttribute("loggedInOfficerRoleLabel"));
        assertEquals("EMP-NIC-001", session.getAttribute("loggedInOfficerEmployeeNo"));
        assertEquals("nic.officer@gov.lk", session.getAttribute("loggedInOfficerEmail"));
    }

    @Test
    void directorCanChooseCsrDashboardAtLogin() throws Exception {
        OfficerDetailsDto director = new OfficerDetailsDto(
                71,
                "EMP-DIR-001",
                "director@gov.lk",
                "+94771234568",
                OfficerAccountService.DIRECTOR,
                "Director",
                "director",
                "ACTIVE"
        );

        when(officerAccountService.dashboardForRoleSelection("csr"))
                .thenReturn("/officer/csr");
        when(officerAccountService.authenticateOfficer(
                "director@gov.lk",
                "password123",
                "csr"
        )).thenReturn(Optional.of(director));

        HttpSession session = mockMvc.perform(post("/login/officer")
                        .param("role", "csr")
                        .param("email", "director@gov.lk")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/csr"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session);
        assertEquals(OfficerAccountService.DIRECTOR, session.getAttribute("loggedInOfficerRole"));
        assertEquals("Director", session.getAttribute("loggedInOfficerRoleLabel"));
        assertEquals("EMP-DIR-001", session.getAttribute("loggedInOfficerEmployeeNo"));
        assertEquals("director@gov.lk", session.getAttribute("loggedInOfficerEmail"));
    }
}
