package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.PassportApplicationRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class CitizenApplicationWorkflowIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userRepository;
    @Autowired private CitizenAccountRepository citizenRepository;
    @Autowired private GovernmentIdentityRecordRepository governmentRepository;
    @Autowired private CitizenApplicationRepository applicationRepository;
    @Autowired private NicApplicationRepository nicRepository;
    @Autowired private PassportApplicationRepository passportRepository;

    @Test
    void citizenWithoutGovernmentDocumentGetsNewApplicationForms() throws Exception {
        UserAccount user = createCitizen(false);
        MockHttpSession session = citizenSession(user);

        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New NIC")))
                .andExpect(content().string(containsString("New Passport")));

        mockMvc.perform(get("/citizen/applications/nic/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New NIC Application")));

        mockMvc.perform(get("/citizen/applications/nic/renewal").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"));
    }

    @Test
    void existingHolderGetsOnlyRenewalAndLostChoices() throws Exception {
        UserAccount user = createCitizen(true);
        MockHttpSession session = citizenSession(user);

        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Renew NIC")))
                .andExpect(content().string(containsString("Lost NIC")))
                .andExpect(content().string(containsString("Renew Passport")))
                .andExpect(content().string(containsString("Lost Passport")))
                .andExpect(content().string(not(containsString(">New NIC<"))));

        mockMvc.perform(get("/citizen/applications/passport/renewal").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Passport Renewal Application")));

        mockMvc.perform(get("/citizen/applications/passport/new").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"));
    }

    @Test
    void newNicFormCreatesApplicationAndNicRows() throws Exception {
        UserAccount user = createCitizen(false);
        MockHttpServletRequestBuilder request = validProfile(
                post("/citizen/applications/nic/new").session(citizenSession(user))
        );

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/citizen/applications/nic/documents?applicationId=*"));

        CitizenApplication application = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(user.getUserId()).get(0);
        assertEquals("NIC", application.getServiceType());
        assertEquals("NEW", application.getApplicationKind());
        assertTrue(nicRepository.existsById(application.getApplicationId()));
    }

    @Test
    void newPassportFormCreatesApplicationAndPassportRows() throws Exception {
        UserAccount user = createCitizen(false);
        MockHttpServletRequestBuilder request = validProfile(
                post("/citizen/applications/passport/new").session(citizenSession(user))
        ).param("parentNicNo", "197012345678")
                .param("serviceLevel", "NORMAL")
                .param("travelDocumentType", "ALL_COUNTRIES");

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/citizen/applications/passport/documents?applicationId=*"));

        CitizenApplication application = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(user.getUserId()).get(0);
        assertEquals("PASSPORT", application.getServiceType());
        assertEquals("NEW", application.getApplicationKind());
        assertTrue(passportRepository.existsById(application.getApplicationId()));
    }

    private UserAccount createCitizen(boolean createGovernmentRecord) {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = new UserAccount();
        user.setEmail("citizen-" + unique + "@example.com");
        user.setPhone("+9477" + Math.abs(unique.hashCode() % 10_000_000));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);
        citizenRepository.saveAndFlush(new CitizenAccount(user.getUserId()));

        if (createGovernmentRecord) {
            GovernmentIdentityRecord record = new GovernmentIdentityRecord();
            record.setNicNo("20" + Math.abs(unique.hashCode() % 1_000_000_000));
            record.setPassportNo("N" + Math.abs(unique.hashCode() % 10_000_000));
            record.setRegisteredEmail(user.getEmail());
            record.setRegisteredPhone(user.getPhone());
            record.setIdentityStatus("ACTIVE");
            governmentRepository.saveAndFlush(record);
        }
        return user;
    }

    private MockHttpSession citizenSession(UserAccount user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", user.getUserId());
        session.setAttribute("loggedInUserEmail", user.getEmail());
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }

    private MockHttpServletRequestBuilder validProfile(MockHttpServletRequestBuilder request) {
        return request
                .param("familyName", "Perera")
                .param("givenName", "Nimal")
                .param("surname", "Perera")
                .param("gender", "MALE")
                .param("civilStatus", "SINGLE")
                .param("dateOfBirth", "2000-01-01")
                .param("birthCertificateNo", "BC-100")
                .param("birthPlace", "Colombo")
                .param("birthDivision", "Colombo")
                .param("birthDistrict", "Colombo")
                .param("countryOfBirth", "Sri Lanka")
                .param("birthCity", "Colombo")
                .param("permanentHouseNo", "10")
                .param("permanentStreet", "Main Street")
                .param("permanentCity", "Colombo")
                .param("permanentPostalCode", "00100")
                .param("postalSameAsPermanent", "true")
                .param("declarationAccepted", "true");
    }
}
