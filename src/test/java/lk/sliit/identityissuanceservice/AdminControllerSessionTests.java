package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.AdminController;
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

class AdminControllerSessionTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AdminController()).build();
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
}
