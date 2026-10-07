package lk.sliit.identityissuanceservice;

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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class VerificationQueueIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CitizenAccountRepository citizenAccountRepository;

    @Autowired
    private CitizenApplicationRepository applicationRepository;

    @Test
    void eachOfficerSeesOnlySubmittedAndOngoingApplicationsForTheirService() throws Exception {
        UserAccount user = createCitizen();
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CitizenApplication submittedNic = createApplication(
                user.getUserId(), "LK-NIC-QUEUE-" + suffix, "NIC", "NEW", "SUBMITTED"
        );
        CitizenApplication ongoingNic = createApplication(
                user.getUserId(), "LK-NIC-ONGOING-" + suffix, "NIC", "RENEWAL", "UNDER_REVIEW"
        );
        CitizenApplication draftNic = createApplication(
                user.getUserId(), "LK-NIC-DRAFT-" + suffix, "NIC", "NEW", "DRAFT"
        );
        CitizenApplication submittedPassport = createApplication(
                user.getUserId(), "LK-PASS-QUEUE-" + suffix, "PASSPORT", "NEW", "SUBMITTED"
        );

        mockMvc.perform(get("/officer/nic/applications").session(officerSession("NIC_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(submittedNic.getApplicationNo())))
                .andExpect(content().string(containsString(ongoingNic.getApplicationNo())))
                .andExpect(content().string(containsString("Nimal Perera")))
                .andExpect(content().string(containsString("Under review")))
                .andExpect(content().string(not(containsString(draftNic.getApplicationNo()))))
                .andExpect(content().string(not(containsString(submittedPassport.getApplicationNo()))));

        mockMvc.perform(get("/officer/passport/applications").session(officerSession("PASSPORT_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(submittedPassport.getApplicationNo())))
                .andExpect(content().string(not(containsString(submittedNic.getApplicationNo()))));
    }

    private UserAccount createCitizen() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        UserAccount user = new UserAccount();
        user.setEmail("queue-" + unique + "@example.com");
        user.setPhone("+947" + Math.abs(unique.hashCode() % 100_000_000));
        user.setPasswordHash("test-hash");
        user = userAccountRepository.saveAndFlush(user);

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Nimal");
        citizen.setFamilyName("Perera");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 4, 12, 0, 0));
        citizenAccountRepository.saveAndFlush(citizen);
        return user;
    }

    private CitizenApplication createApplication(
            Integer citizenId,
            String applicationNo,
            String serviceType,
            String applicationKind,
            String status
    ) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo(applicationNo);
        application.setCitizenId(citizenId);
        application.setServiceType(serviceType);
        application.setApplicationKind(applicationKind);
        application.setCurrentStatus(status);
        application.setSubmittedAt("DRAFT".equals(status) ? null : LocalDateTime.now());
        application.setDeclarationAccepted("DRAFT".equals(status) ? "N" : "Y");
        return applicationRepository.saveAndFlush(application);
    }

    private MockHttpSession officerSession(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", 70);
        session.setAttribute("loggedInOfficerRole", role);
        session.setAttribute("loggedInOfficerRoleLabel", role.equals("NIC_OFFICER")
                ? "NIC Application Officer"
                : "Passport Application Officer");
        session.setAttribute("loggedInOfficerEmployeeNo", role.equals("NIC_OFFICER")
                ? "EMP-NIC-001"
                : "EMP-PASS-001");
        session.setAttribute("loggedInOfficerEmail", "officer@gov.lk");
        return session;
    }
}
