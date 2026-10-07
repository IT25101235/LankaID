package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MaintenanceUiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemMaintenanceService maintenanceService;

    @AfterEach
    void restoreSystem() {
        maintenanceService.updateMaintenance(false, SystemMaintenanceService.DEFAULT_MESSAGE);
    }

    @Test
    void maintenanceWarningAppearsAndOperationalRoutesAreBlocked() throws Exception {
        maintenanceService.updateMaintenance(true, "Scheduled maintenance test");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("LankaID is under maintenance")))
                .andExpect(content().string(containsString("Scheduled maintenance test")));

        mockMvc.perform(get("/citizen/dashboard")
                        .sessionAttr("loggedInUserId", 25))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?maintenance=true"));

        mockMvc.perform(get("/officer/admin")
                        .sessionAttr("loggedInAdminId", 50)
                        .sessionAttr("loggedInAdminEmail", "admin@gov.lk"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maintenance Mode")));
    }
}
