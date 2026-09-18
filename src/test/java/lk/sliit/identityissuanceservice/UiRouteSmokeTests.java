package lk.sliit.identityissuanceservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UiRouteSmokeTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allUiPagesOpenSuccessfully() throws Exception {
        // Keep this list in sync with ROUTES.md when a new demonstration page is added.
        String[] routes = {
                "/", "/login", "/register", "/forgot-password", "/walk-in", "/walk-in/recover",
                "/citizen/payment",
                "/citizen/notifications",
                "/citizen/appointments/biometric",
                "/citizen/appointments/walk-in",
                "/citizen/support",
                "/citizen/support/new",
                "/citizen/support/contact-us",
                "/citizen/support/complaint",
                "/citizen/support/CMP-2026-0102",
                "/officer/nic/applications",
                "/officer/nic/applications/NIC-2023-8891",
                "/officer/nic/applications/processed",
                "/officer/nic/appointments",
                "/officer/nic/biometrics",
                "/officer/passport/applications",
                "/officer/passport/applications/PP-2023-8891",
                "/officer/passport/applications/processed",
                "/officer/passport/appointments",
                "/officer/passport/biometrics",
                "/officer/csr",
                "/officer/csr/support",
                "/officer/csr/complaints/CMP-2026-0102",
                "/officer/csr/walk-ins",
                "/officer/csr/walk-ins/register",
                "/officer/director",
                "/officer/director/printing",
                "/officer/admin",
                "/officer/admin/officers",
                "/officer/admin/citizens",
                "/officer/admin/notifications"
        };

        for (String route : routes) {
            MockHttpServletRequestBuilder request = get(route);

            if (route.startsWith("/citizen/")) {
                request.sessionAttr("loggedInUserId", 1);
                request.sessionAttr("loggedInUserRole", "CITIZEN");
            }
            if (route.startsWith("/officer/admin")) {
                request.sessionAttr("loggedInAdminId", 1);
            } else if (route.startsWith("/officer/nic")) {
                request.sessionAttr("loggedInOfficerRole", "NIC_OFFICER");
            } else if (route.startsWith("/officer/passport")) {
                request.sessionAttr("loggedInOfficerRole", "PASSPORT_OFFICER");
            } else if (route.startsWith("/officer/csr")) {
                request.sessionAttr("loggedInOfficerRole", "CSR");
            } else if (route.startsWith("/officer/director")) {
                request.sessionAttr("loggedInOfficerRole", "DIRECTOR");
            }

            if (route.equals("/officer/nic/applications/NIC-2023-8891")
                    || route.equals("/officer/passport/applications/PP-2023-8891")) {
                mockMvc.perform(request).andExpect(status().is3xxRedirection());
            } else if (route.equals("/officer/admin/citizens")
                    || route.equals("/officer/admin/notifications")) {
                mockMvc.perform(request).andExpect(status().is3xxRedirection());
            } else {
                mockMvc.perform(request).andExpect(status().isOk());
            }
        }
    }
}
