package lk.sliit.identityissuanceservice;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.UserController;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.OfficerSessionIdentityAdvice;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.EmailVerificationChallenge;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.EmailVerificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
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
    private EmailVerificationService emailVerificationService;

    @Mock
    private AdminAccountService adminAccountService;

    @Mock
    private OfficerAccountService officerAccountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(
                userAccountService,
                emailVerificationService,
                adminAccountService,
                officerAccountService
        )).build();
    }

    @Test
    void successfulLoginCreatesCitizenSession() throws Exception {
        UserAccount account = new UserAccount();
        account.setUserId(25);
        account.setEmail("citizen@example.com");

        when(userAccountService.authenticateCitizen(
                "citizen@example.com",
                "Password123!"
        )).thenReturn(Optional.of(account));

        HttpSession session = mockMvc.perform(post("/login")
                        .param("email", "citizen@example.com")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(session);
        assertEquals(25, session.getAttribute("loggedInUserId"));
        assertEquals("citizen@example.com", session.getAttribute("loggedInUserEmail"));
        assertEquals("CITIZEN", session.getAttribute("loggedInUserRole"));
        assertEquals("/citizen/dashboard", session.getAttribute("loggedInDashboardPath"));
        assertEquals(1800, session.getMaxInactiveInterval());
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
        assertEquals("admin@gov.lk", session.getAttribute("loggedInOfficerEmail"));
        assertEquals("System Administrator", session.getAttribute("loggedInOfficerRoleLabel"));
        assertNotNull(session.getAttribute("loggedInOfficerLoginTime"));
        assertEquals("/officer/admin", session.getAttribute("loggedInDashboardPath"));
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
                "Password123!",
                "nic-officer"
        )).thenReturn(Optional.of(officer));

        HttpSession session = mockMvc.perform(post("/login/officer")
                        .param("role", "nic-officer")
                        .param("email", "nic.officer@gov.lk")
                        .param("password", "Password123!"))
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
        assertNotNull(session.getAttribute("loggedInOfficerLoginTime"));
        assertEquals("/officer/nic/applications", session.getAttribute("loggedInDashboardPath"));
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
                "Password123!",
                "csr"
        )).thenReturn(Optional.of(director));

        HttpSession session = mockMvc.perform(post("/login/officer")
                        .param("role", "csr")
                        .param("email", "director@gov.lk")
                        .param("password", "Password123!"))
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
        assertNotNull(session.getAttribute("loggedInOfficerLoginTime"));
        assertEquals("/officer/csr", session.getAttribute("loggedInDashboardPath"));
    }

    @Test
    void landingPageShowsTheSavedDashboardForAnAuthenticatedSession() throws Exception {
        MockHttpSession citizenSession = new MockHttpSession();
        citizenSession.setAttribute("loggedInUserId", 25);

        mockMvc.perform(get("/").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("landingSignedIn", true))
                .andExpect(model().attribute("landingDashboardPath", "/citizen/dashboard"));

        MockHttpSession directorSession = new MockHttpSession();
        directorSession.setAttribute("loggedInOfficerId", 71);
        directorSession.setAttribute("loggedInOfficerRole", OfficerAccountService.DIRECTOR);
        directorSession.setAttribute("loggedInDashboardPath", "/officer/passport/applications");

        mockMvc.perform(get("/").session(directorSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("landingSignedIn", true))
                .andExpect(model().attribute("landingDashboardPath", "/officer/passport/applications"));
    }

    @Test
    void registrationRequiresEmailCodeBeforeCreatingAccount() throws Exception {
        PreparedCitizenRegistration registration = new PreparedCitizenRegistration(
                "citizen@example.com",
                "+94771234567",
                "bcrypt-hash"
        );
        EmailVerificationChallenge challenge = new EmailVerificationChallenge(
                "code-hash",
                Instant.now().plusSeconds(600),
                Instant.now().plusSeconds(60)
        );
        when(userAccountService.prepareCitizenRegistration(
                "citizen@example.com",
                "0771234567",
                "Password123!"
        )).thenReturn(registration);
        when(emailVerificationService.sendVerificationCode("citizen@example.com"))
                .thenReturn(challenge);

        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/register")
                        .param("email", "citizen@example.com")
                        .param("phone", "0771234567")
                        .param("password", "Password123!")
                        .param("confirmPassword", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/verify"))
                .andReturn().getRequest().getSession(false);

        when(emailVerificationService.isValid(challenge, "123456")).thenReturn(true);
        mockMvc.perform(post("/register/verify")
                        .session(session)
                        .param("code", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userAccountService).registerVerifiedCitizen(registration);
    }

    @Test
    void officerIdentityAdviceExposesTheSharedSidebarDetails() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerEmail", "director@gov.lk");
        session.setAttribute("loggedInOfficerRoleLabel", "Director");
        session.setAttribute("loggedInOfficerLoginTime", "01 Sep 2026, 10:15");
        ExtendedModelMap model = new ExtendedModelMap();

        new OfficerSessionIdentityAdvice().exposeOfficerIdentity(session, model);

        assertEquals("director@gov.lk", model.get("loggedInOfficerEmail"));
        assertEquals("Director", model.get("loggedInOfficerRoleLabel"));
        assertEquals("01 Sep 2026, 10:15", model.get("loggedInOfficerLoginTime"));
    }

    @Test
    void invalidRegistrationKeepsEmailAndReportsPhoneAndPasswordErrors() throws Exception {
        mockMvc.perform(post("/register")
                        .param("email", "citizen@example.com")
                        .param("phone", "77123")
                        .param("password", "weak").param("confirmPassword", "weak"))
                .andExpect(status().isOk())
                .andExpect(view().name("citizen_frontends/citizen_register"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeHasFieldErrors("registrationRequest", "phone", "password"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attribute("registrationRequest", org.hamcrest.Matchers.hasProperty("email", org.hamcrest.Matchers.is("citizen@example.com"))));
        org.mockito.Mockito.verify(userAccountService, org.mockito.Mockito.never())
                .prepareCitizenRegistration(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
