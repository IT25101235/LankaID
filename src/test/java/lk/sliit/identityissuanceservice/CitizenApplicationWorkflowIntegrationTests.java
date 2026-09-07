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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@TestPropertySource(properties = "app.upload.directory=target/test-uploads")
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

        mockMvc.perform(get("/citizen/dashboard").session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(application.getApplicationNo())))
                .andExpect(content().string(not(containsString(">New NIC<"))))
                .andExpect(content().string(containsString(
                        "/citizen/applications/nic/documents?applicationId="
                                + application.getApplicationId()
                )));

        mockMvc.perform(get("/citizen/applications").session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(application.getApplicationNo())))
                .andExpect(content().string(containsString(
                        "/citizen/applications/nic/documents?applicationId="
                                + application.getApplicationId()
                )));

        mockMvc.perform(get("/citizen/applications/nic/documents")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "/citizen/applications/nic/review?applicationId=" + application.getApplicationId()
                )));

        MockMultipartFile photograph = new MockMultipartFile(
                "file",
                "photograph.jpg",
                "image/jpeg",
                "test-image".getBytes()
        );
        mockMvc.perform(multipart("/citizen/applications/nic/documents")
                        .file(photograph)
                        .param("applicationId", application.getApplicationId().toString())
                        .param("documentType", "PHOTOGRAPH")
                        .session(citizenSession(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/citizen/applications/nic/documents?applicationId="
                                + application.getApplicationId()
                ));

        mockMvc.perform(get("/citizen/applications/nic/documents")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PHOTOGRAPH")))
                .andExpect(content().string(containsString("Photograph (already uploaded)")))
                .andExpect(content().string(containsString(application.getApplicationNo())));

        MockMultipartFile duplicatePhotograph = new MockMultipartFile(
                "file",
                "another-photograph.jpg",
                "image/jpeg",
                "another-test-image".getBytes()
        );
        mockMvc.perform(multipart("/citizen/applications/nic/documents")
                        .file(duplicatePhotograph)
                        .param("applicationId", application.getApplicationId().toString())
                        .param("documentType", "PHOTOGRAPH")
                        .session(citizenSession(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "documentError",
                        "That document type has already been uploaded."
                ))
                .andExpect(redirectedUrl(
                        "/citizen/applications/nic/documents?applicationId="
                                + application.getApplicationId()
                ));
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

        mockMvc.perform(get("/citizen/applications/passport/edit")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editing saved draft")))
                .andExpect(content().string(containsString("Save changes and continue")));
    }

    @Test
    void unsubmittedDraftCanBeDeletedAndCitizenCanStartAgain() throws Exception {
        UserAccount user = createCitizen(false);
        MockHttpSession session = citizenSession(user);
        mockMvc.perform(validProfile(post("/citizen/applications/nic/new").session(session)))
                .andExpect(status().is3xxRedirection());

        CitizenApplication draft = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(user.getUserId()).get(0);
        assertTrue(nicRepository.existsById(draft.getApplicationId()));

        mockMvc.perform(post("/citizen/applications/{applicationId}/delete", draft.getApplicationId())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"))
                .andExpect(flash().attribute(
                        "applicationMessage",
                        "Draft " + draft.getApplicationNo()
                                + " was deleted. You can start a new application when needed."
                ));

        assertFalse(applicationRepository.existsById(draft.getApplicationId()));
        assertFalse(nicRepository.existsById(draft.getApplicationId()));
        mockMvc.perform(get("/citizen/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New NIC")))
                .andExpect(content().string(containsString("No applications yet")));
    }

    @Test
    void newNicDraftBioDataCanBeEditedBeforeSubmissionOnly() throws Exception {
        UserAccount user = createCitizen(false);
        MockHttpSession session = citizenSession(user);
        mockMvc.perform(validProfile(post("/citizen/applications/nic/new").session(session)))
                .andExpect(status().is3xxRedirection());

        CitizenApplication draft = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(user.getUserId()).get(0);
        String applicationId = draft.getApplicationId().toString();

        mockMvc.perform(get("/citizen/applications/nic/documents")
                        .param("applicationId", applicationId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit bio data")))
                .andExpect(content().string(containsString(
                        "/citizen/applications/nic/edit?applicationId=" + applicationId
                )));

        mockMvc.perform(get("/citizen/applications/nic/edit")
                        .param("applicationId", applicationId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editing saved draft")))
                .andExpect(content().string(containsString("Save changes and continue")));

        mockMvc.perform(validProfile(post("/citizen/applications/nic/new")
                        .param("applicationId", applicationId)
                        .param("occupation", "Teacher")
                        .session(session)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/citizen/applications/nic/documents?applicationId=" + applicationId
                ));
        assertEquals("Teacher", citizenRepository.findById(user.getUserId()).orElseThrow().getOccupation());

        draft.setCurrentStatus("SUBMITTED");
        applicationRepository.saveAndFlush(draft);
        mockMvc.perform(get("/citizen/applications/nic/edit")
                        .param("applicationId", applicationId)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"));
    }

    @Test
    void nicRenewalIsCreatedByTheRenewalWorkflow() throws Exception {
        UserAccount user = createCitizen(true);
        MockHttpServletRequestBuilder request = validProfile(
                post("/citizen/applications/nic/renewal").session(citizenSession(user))
        ).param("lastNicNo", governmentRepository
                .findByRegisteredEmailIgnoreCaseAndRegisteredPhone(user.getEmail(), user.getPhone())
                .orElseThrow()
                .getNicNo())
                .param("lastIssuedDate", "2020-01-01");

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/citizen/renewals/nic/documents?applicationId=*"));

        CitizenApplication application = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(user.getUserId()).get(0);
        assertEquals("NIC", application.getServiceType());
        assertEquals("RENEWAL", application.getApplicationKind());
        assertTrue(nicRepository.existsById(application.getApplicationId()));

        mockMvc.perform(get("/citizen/renewals/nic/documents")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "/citizen/renewals/nic/review?applicationId=" + application.getApplicationId()
                )));
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
