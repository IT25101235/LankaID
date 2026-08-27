package lk.sliit.identityissuanceservice;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new UserController(userAccountService, adminAccountService)).build();
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
}
