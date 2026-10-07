package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class OfficerApplicationTrackingIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired CitizenApplicationRepository applicationRepository;

    @Test
    void trackingPageAppliesDirectorAndApplicationOfficerRoleScopes() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String nicReference = createApplication("NIC", unique, "Nimali");
        String passportReference = createApplication("PASSPORT", unique, "Pasindu");

        mockMvc.perform(get("/officer/application-tracking").session(officerSession(OfficerAccountService.DIRECTOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<table")))
                .andExpect(content().string(containsString("Showing 15 applications at a time")))
                .andExpect(content().string(containsString(nicReference)))
                .andExpect(content().string(containsString(passportReference)))
                .andExpect(content().string(containsString("All NIC and Passport applications")));

        mockMvc.perform(get("/officer/application-tracking").session(officerSession(OfficerAccountService.NIC_OFFICER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(nicReference)))
                .andExpect(content().string(not(containsString(passportReference))))
                .andExpect(content().string(containsString("NIC applications")));

        mockMvc.perform(get("/officer/application-tracking").session(officerSession(OfficerAccountService.PASSPORT_OFFICER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(passportReference)))
                .andExpect(content().string(not(containsString(nicReference))))
                .andExpect(content().string(containsString("Passport applications")));

        mockMvc.perform(get("/officer/application-tracking").session(officerSession(OfficerAccountService.CSR)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/csr"));
    }

    @Test
    void trackingPageLoadsOnlyFifteenApplicationsAtATime() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        for (int index = 0; index < 16; index++) {
            createApplication("NIC", unique + "-" + index, "Citizen" + index);
        }

        mockMvc.perform(get("/officer/application-tracking")
                .param("search", unique)
                        .session(officerSession(OfficerAccountService.NIC_OFFICER)))
                .andExpect(status().isOk())
                .andExpect(xpath("//span[contains(@class,'font-bold')]").string("16"))
                .andExpect(content().string(containsString("Page 1 of 2")))
                .andExpect(content().string(containsString("Load next 15")));

        mockMvc.perform(get("/officer/application-tracking")
                        .param("search", unique)
                        .param("page", "1")
                        .session(officerSession(OfficerAccountService.NIC_OFFICER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Page 2 of 2")))
                .andExpect(content().string(containsString("Previous 15")));
    }

    private String createApplication(String serviceType, String unique, String givenName) {
        UserAccount user = new UserAccount();
        user.setEmail(serviceType.toLowerCase() + "-tracking-" + unique + "@example.com");
        user.setPhone("+947" + Math.abs((serviceType + unique).hashCode()));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName(givenName);
        citizen.setFamilyName("Perera");
        citizenRepository.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-" + serviceType + "-TRACK-" + unique);
        application.setCitizenId(user.getUserId());
        application.setServiceType(serviceType);
        application.setApplicationKind("NEW");
        application.setCurrentStatus("SUBMITTED");
        application.setDeclarationAccepted("Y");
        return applicationRepository.saveAndFlush(application).getApplicationNo();
    }

    private MockHttpSession officerSession(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", 99999);
        session.setAttribute("loggedInOfficerRole", role);
        session.setAttribute("loggedInOfficerRoleLabel", role);
        session.setAttribute("loggedInOfficerEmployeeNo", "EMP-TEST-001");
        session.setAttribute("loggedInOfficerEmail", "officer@gov.lk");
        return session;
    }
}
