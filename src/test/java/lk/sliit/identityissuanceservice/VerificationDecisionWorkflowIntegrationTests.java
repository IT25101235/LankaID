package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@TestPropertySource(properties = "app.upload.directory=target/test-verification-uploads")
class VerificationDecisionWorkflowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired CitizenApplicationRepository applicationRepository;
    @Autowired NicApplicationRepository nicRepository;
    @Autowired ApplicationDocumentRepository documentRepository;
    @Autowired StaffAccountRepository staffRepository;

    @Test
    void officerReviewsRequestsCorrectionAndApprovesCitizenResubmission() throws Exception {
        UserAccount user = createCitizen();
        CitizenApplication application = createDraftNic(user.getUserId());
        MockHttpSession citizenSession = citizenSession(user);

        upload(application, citizenSession, "PHOTOGRAPH", "photo.png", "first-photo".getBytes());
        upload(application, citizenSession, "BIRTH_CERTIFICATE", "birth.pdf", "%PDF-birth".getBytes());

        mockMvc.perform(post("/citizen/applications/nic/submit")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"));
        assertEquals("SUBMITTED", applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus());

        MockHttpSession officerSession = officerSession(createNicOfficer());
        mockMvc.perform(post("/officer/nic/applications/{reference}/review", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()));

        mockMvc.perform(get("/officer/nic/applications/{reference}", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nimal Perera")))
                .andExpect(content().string(containsString("Open on screen")))
                .andExpect(content().string(containsString("Approve application")))
                .andExpect(content().string(containsString("Request selected corrections")));

        ApplicationDocument photograph = documentRepository
                .findByApplicationIdAndDocumentType(application.getApplicationId(), "PHOTOGRAPH")
                .orElseThrow();
        String oldFilePath = photograph.getFilePath();

        mockMvc.perform(get("/officer/verification/documents/{documentId}/view", photograph.getDocumentId())
                        .session(officerSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/png")))
                .andExpect(content().bytes("first-photo".getBytes()));

        mockMvc.perform(post("/officer/nic/applications/{reference}/correction", application.getApplicationNo())
                        .param("correctForm", "true")
                        .param("documentIds", photograph.getDocumentId().toString())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"));
        assertEquals("CORRECTION_REQUIRED", applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus());
        assertEquals("REJECTED", documentRepository.findById(photograph.getDocumentId()).orElseThrow().getVerificationStatus());

        mockMvc.perform(get("/citizen/dashboard").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Items to correct:")))
                .andExpect(content().string(containsString("Application form details")))
                .andExpect(content().string(containsString("Photograph")));

        mockMvc.perform(get("/citizen/applications/nic/correction")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Officer correction requested")))
                .andExpect(content().string(containsString("Items to correct:")))
                .andExpect(content().string(containsString("Application form details")))
                .andExpect(content().string(containsString("Photograph")))
                .andExpect(content().string(containsString("Nimal")));

        mockMvc.perform(get("/citizen/applications/nic/documents")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Items to correct:")))
                .andExpect(content().string(containsString("Application form details")))
                .andExpect(content().string(containsString("Photograph")));

        mockMvc.perform(validProfile(post("/citizen/applications/nic/new")
                        .param("applicationId", application.getApplicationId().toString())
                        .param("familyName", "Fernando")
                        .session(citizenSession)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/applications/nic/documents?applicationId=" + application.getApplicationId()));

        upload(application, citizenSession, "PHOTOGRAPH", "corrected-photo.png", "corrected-photo".getBytes());
        ApplicationDocument replacedPhoto = documentRepository.findById(photograph.getDocumentId()).orElseThrow();
        assertNotEquals(oldFilePath, replacedPhoto.getFilePath());
        assertEquals("PENDING", replacedPhoto.getVerificationStatus());

        mockMvc.perform(post("/citizen/applications/nic/submit")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/dashboard"));
        assertEquals("RESUBMITTED", applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus());

        mockMvc.perform(post("/officer/nic/applications/{reference}/review", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"));

        assertEquals("APPROVED", applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus());
        List<ApplicationDocument> verifiedDocuments = documentRepository
                .findAllByApplicationIdOrderByUploadedAtAsc(application.getApplicationId());
        assertEquals(2, verifiedDocuments.size());
        verifiedDocuments.forEach(document -> assertEquals("VERIFIED", document.getVerificationStatus()));
    }

    private void upload(
            CitizenApplication application,
            MockHttpSession session,
            String documentType,
            String fileName,
            byte[] bytes
    ) throws Exception {
        String contentType = fileName.endsWith(".pdf") ? "application/pdf" : "image/png";
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, bytes);
        mockMvc.perform(multipart("/citizen/applications/nic/documents")
                        .file(file)
                        .param("applicationId", application.getApplicationId().toString())
                        .param("documentType", documentType)
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/applications/nic/documents?applicationId=" + application.getApplicationId()));
    }

    private UserAccount createCitizen() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = new UserAccount();
        user.setEmail("decision-" + unique + "@example.com");
        user.setPhone("+947" + Math.abs(unique.hashCode() % 100_000_000));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setFamilyName("Perera");
        citizen.setGivenName("Nimal");
        citizen.setSurname("Perera");
        citizen.setGender("MALE");
        citizen.setCivilStatus("SINGLE");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));
        citizen.setBirthCertificateNo("BC-100");
        citizen.setBirthPlace("Colombo");
        citizen.setBirthDivision("Colombo");
        citizen.setBirthDistrict("Colombo");
        citizen.setCountryOfBirth("Sri Lanka");
        citizen.setBirthCity("Colombo");
        citizen.setPermanentHouseNo("10");
        citizen.setPermanentStreet("Main Street");
        citizen.setPermanentCity("Colombo");
        citizen.setPermanentPostalCode("00100");
        citizen.setPostalSameAsPermanent("Y");
        citizen.setDualCitizen("N");
        citizenRepository.saveAndFlush(citizen);
        return user;
    }

    private CitizenApplication createDraftNic(Integer citizenId) {
        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-NIC-TEST-" + UUID.randomUUID().toString().substring(0, 8));
        application.setCitizenId(citizenId);
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("DRAFT");
        application.setDeclarationAccepted("Y");
        application = applicationRepository.saveAndFlush(application);

        NicApplication details = new NicApplication();
        details.setApplicationId(application.getApplicationId());
        nicRepository.saveAndFlush(details);
        return application;
    }

    private MockHttpSession citizenSession(UserAccount user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", user.getUserId());
        session.setAttribute("loggedInUserEmail", user.getEmail());
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }

    private StaffAccount createNicOfficer() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = new UserAccount();
        user.setEmail("nic-officer-" + unique + "@gov.lk");
        user.setPhone("+946" + Math.abs(unique.hashCode() % 100_000_000));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        StaffAccount officer = new StaffAccount();
        officer.setUserId(user.getUserId());
        officer.setEmployeeNo("NIC-" + unique);
        officer.setStaffRole("NIC_OFFICER");
        officer.setStaffStatus("ACTIVE");
        return staffRepository.saveAndFlush(officer);
    }

    private MockHttpSession officerSession(StaffAccount officer) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", officer.getUserId());
        session.setAttribute("loggedInOfficerRole", "NIC_OFFICER");
        session.setAttribute("loggedInOfficerRoleLabel", "NIC Application Officer");
        session.setAttribute("loggedInOfficerEmployeeNo", officer.getEmployeeNo());
        session.setAttribute("loggedInOfficerEmail", "nic-officer@gov.lk");
        return session;
    }

    private MockHttpServletRequestBuilder validProfile(MockHttpServletRequestBuilder request) {
        return request
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
