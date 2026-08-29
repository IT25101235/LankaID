package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Controller.DirectorController;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.DirectorDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DirectorPrintingControllerTests {

    private PrintingExportService printingService;
    private MockMvc mockMvc;
    private MockHttpSession directorSession;

    @BeforeEach
    void setUp() {
        printingService = mock(PrintingExportService.class);
        DirectorDashboardService dashboardService = mock(DirectorDashboardService.class);
        mockMvc = standaloneSetup(new DirectorController(printingService, dashboardService)).build();

        directorSession = new MockHttpSession();
        directorSession.setAttribute("loggedInOfficerId", 70);
        directorSession.setAttribute("loggedInOfficerRole", OfficerAccountService.DIRECTOR);
    }

    @Test
    void selectedApplicationsAreProcessedAndRedirectBackToPrintingPage() throws Exception {
        mockMvc.perform(post("/officer/director/printing/process")
                        .session(directorSession)
                        .param("applicationIds", "17", "18"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/director/printing"))
                .andExpect(flash().attribute(
                        "printingMessage",
                        "2 document(s) printed and added to the downloadable register."
                ));

        verify(printingService).printAndCreateCsv(List.of(17, 18), 70);
    }

    @Test
    void unexpectedDatabaseFailureShowsMessageInsteadOfWhitelabelPage() throws Exception {
        doThrow(new IllegalStateException("database error"))
                .when(printingService).printAndCreateCsv(List.of(17), 70);

        mockMvc.perform(post("/officer/director/printing/process")
                        .session(directorSession)
                        .param("applicationIds", "17"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/director/printing"))
                .andExpect(flash().attribute(
                        "printingError",
                        "Printing could not be completed. No records were changed. "
                                + "Please check the citizen details and try again."
                ));
    }
}
