package lk.sliit.identityissuanceservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

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
                "/", "/login", "/register",
                "/citizen/dashboard?profile=new",
                "/citizen/dashboard?profile=nic-only",
                "/citizen/dashboard?profile=existing",
                "/citizen/applications",
                "/citizen/applications/nic/new",
                "/citizen/applications/nic/renewal",
                "/citizen/applications/nic/replacement",
                "/citizen/applications/passport/new",
                "/citizen/applications/passport/renewal",
                "/citizen/applications/passport/replacement",
                "/citizen/applications/nic/documents",
                "/citizen/applications/passport/documents",
                "/citizen/applications/nic/review",
                "/citizen/applications/passport/review",
                "/citizen/applications/nic/LK-NIC-2026-00097/correct",
                "/citizen/applications/passport/LK-PASS-2026-00041/correct",
                "/citizen/payment",
                "/citizen/notifications",
                "/citizen/appointments/biometric",
                "/citizen/appointments/walk-in",
                "/citizen/support",
                "/citizen/support/new",
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
            mockMvc.perform(get(route)).andExpect(status().isOk());
        }
    }
}
