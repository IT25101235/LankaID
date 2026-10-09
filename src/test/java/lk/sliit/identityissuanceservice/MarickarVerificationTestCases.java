package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.ApplicationDocument;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.NicApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.ApplicationDocumentRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.NicApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.NotificationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.UserNotificationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@TestPropertySource(properties = "app.upload.directory=target/test-marickar-uploads")
class MarickarVerificationTestCases {

    private static final Path UPLOAD_ROOT = Path.of("target/test-marickar-uploads")
            .toAbsolutePath().normalize();

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired StaffAccountRepository staffRepository;
    @Autowired CitizenApplicationRepository applicationRepository;
    @Autowired NicApplicationRepository nicApplicationRepository;
    @Autowired ApplicationDocumentRepository documentRepository;
    @Autowired ApplicationReviewRepository reviewRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserNotificationRepository userNotificationRepository;

    private final List<Path> createdFiles = new ArrayList<>();

    @BeforeEach
    void createUploadDirectory() throws Exception {
        Files.createDirectories(UPLOAD_ROOT);
    }

    @AfterEach
    void removeTestUploads() throws Exception {
        for (Path file : createdFiles) Files.deleteIfExists(file);
    }

    @Test
    @DisplayName("VER_TC_001 Officer Start Application Review")
    void VER_TC_001_officerStartApplicationReview() throws Exception {
        CitizenApplication application = application("SUBMITTED", "Start Review Citizen");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/review", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()));

        assertEquals("UNDER_REVIEW", refreshed(application).getCurrentStatus());
        assertNotification("APPLICATION_UNDER_REVIEW", application.getApplicationNo());

        mockMvc.perform(get("/officer/nic/applications/{reference}", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("decisionAllowed", true))
                .andExpect(content().string(containsString("Approve application")))
                .andExpect(content().string(containsString("Reject application")))
                .andExpect(content().string(containsString("Request selected corrections")));
    }

    @Test
    @DisplayName("VER_TC_002 Approve Application")
    void VER_TC_002_approveApplication() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Approval Citizen");
        ApplicationDocument document = document(application, "PHOTOGRAPH", "approval-photo.png");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andExpect(flash().attribute("verificationMessage", "Application approved successfully."));

        ApplicationDocument approvedDocument = documentRepository.findById(document.getDocumentId()).orElseThrow();
        ApplicationReview review = latestReview(application);
        assertAll(
                () -> assertEquals("APPROVED", refreshed(application).getCurrentStatus()),
                () -> assertEquals("VERIFIED", approvedDocument.getVerificationStatus()),
                () -> assertEquals(officer.getUserId(), approvedDocument.getVerifiedByStaffId()),
                () -> assertNotNull(approvedDocument.getVerifiedAt()),
                () -> assertEquals("APPROVED", review.getDecision()),
                () -> assertEquals(officer.getUserId(), review.getStaffId())
        );
        assertNotification("APPLICATION_APPROVED", application.getApplicationNo());

        var approvalNotification = notificationRepository.findAll().stream()
                .filter(notification -> "APPLICATION_APPROVED".equals(notification.getNotificationType())
                        && notification.getMessage().contains(application.getApplicationNo()))
                .findFirst().orElseThrow();
        assertTrue(userNotificationRepository.findById_NotificationIdAndId_UserId(
                approvalNotification.getNotificationId(), application.getCitizenId()).isPresent(),
                "The approval notification must be delivered to the applicant.");
    }

    @Test
    @DisplayName("VER_TC_003 Request Correction from Citizen")
    void VER_TC_003_requestCorrectionFromCitizen() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Correction Citizen");
        ApplicationDocument document = document(application, "PHOTOGRAPH", "blurry-photo.png");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/correction", application.getApplicationNo())
                        .param("correctForm", "true")
                        .param("documentIds", document.getDocumentId().toString())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andExpect(flash().attribute("verificationMessage", "Correction requested from the citizen."));

        ApplicationReview review = latestReview(application);
        assertAll(
                () -> assertEquals("CORRECTION_REQUIRED", refreshed(application).getCurrentStatus()),
                () -> assertEquals("REJECTED", documentRepository.findById(document.getDocumentId())
                        .orElseThrow().getVerificationStatus()),
                () -> assertEquals("CORRECTION_REQUIRED", review.getDecision()),
                () -> assertTrue(review.getCorrectionMessage().contains("Application form details")),
                () -> assertTrue(review.getCorrectionMessage().contains("Photograph"))
        );
        assertNotification("CORRECTION_REQUIRED", application.getApplicationNo());
    }

    @Test
    @DisplayName("VER_TC_004 Reject Fraudulent Application")
    void VER_TC_004_rejectFraudulentApplication() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Rejected Citizen");
        ApplicationDocument document = document(application, "BIRTH_CERTIFICATE", "birth.pdf");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/reject", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andExpect(flash().attribute("verificationMessage", "Application rejected."));

        assertAll(
                () -> assertEquals("REJECTED", refreshed(application).getCurrentStatus()),
                () -> assertEquals("REJECTED", documentRepository.findById(document.getDocumentId())
                        .orElseThrow().getVerificationStatus()),
                () -> assertEquals("REJECTED", latestReview(application).getDecision())
        );
        assertNotification("APPLICATION_REJECTED", application.getApplicationNo());
    }

    @Test
    @DisplayName("VER_TC_005 View Uploaded Document Scan")
    void VER_TC_005_viewUploadedDocumentScan() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Document Citizen");
        ApplicationDocument document = document(application, "PHOTOGRAPH", "identity-photo.png");
        byte[] bytes = "test-image-bytes".getBytes();
        Path file = UPLOAD_ROOT.resolve(document.getFilePath());
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
        createdFiles.add(file);
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(get("/officer/verification/documents/{documentId}/view", document.getDocumentId())
                        .session(officerSession(officer)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/png")))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    @DisplayName("VER_TC_006 Concurrent Review Lock / Application State Guard")
    void VER_TC_006_concurrentReviewStateGuard() throws Exception {
        CitizenApplication application = application("APPROVED", "Concurrent Citizen");
        StaffAccount secondOfficer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession(secondOfficer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()))
                .andExpect(flash().attribute("decisionError",
                        "Open the submitted application for review before making a decision."));

        assertEquals("APPROVED", refreshed(application).getCurrentStatus());
        assertTrue(reviewRepository.findTopByApplicationIdOrderByReviewSequenceDesc(
                application.getApplicationId()).isEmpty());
    }

    @Test
    @DisplayName("VER_TC_007 Role-Based Cross-Queue Restriction")
    void VER_TC_007_roleBasedCrossQueueRestriction() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Cross Queue Citizen");
        StaffAccount passportOfficer = officer(OfficerAccountService.PASSPORT_OFFICER);
        MockHttpSession session = officerSession(passportOfficer);

        mockMvc.perform(get("/officer/nic/applications").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertEquals("UNDER_REVIEW", refreshed(application).getCurrentStatus());
        assertTrue(reviewRepository.findTopByApplicationIdOrderByReviewSequenceDesc(
                application.getApplicationId()).isEmpty());
    }

    @Test
    @DisplayName("VER_TC_008 Approval Without UNDER_REVIEW Is Blocked")
    void VER_TC_008_approvalWithoutReviewIsBlocked() throws Exception {
        CitizenApplication application = application("SUBMITTED", "Unclaimed Citizen");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()))
                .andExpect(flash().attribute("decisionError",
                        "Open the submitted application for review before making a decision."));

        assertEquals("SUBMITTED", refreshed(application).getCurrentStatus());
    }

    @Test
    @DisplayName("VER_TC_009 Draft Application Cannot Be Reviewed")
    void VER_TC_009_draftApplicationCannotBeReviewed() throws Exception {
        CitizenApplication application = application("DRAFT", "Draft Citizen");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/review", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andExpect(flash().attribute("verificationError",
                        "This application is not available for verification."));

        assertEquals("DRAFT", refreshed(application).getCurrentStatus());
    }

    @Test
    @DisplayName("VER_TC_010 Empty Correction Request Is Blocked")
    void VER_TC_010_emptyCorrectionRequestIsBlocked() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Empty Correction Citizen");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/correction", application.getApplicationNo())
                        .param("correctForm", "false")
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()))
                .andExpect(flash().attribute("decisionError",
                        "Select the application form or at least one document."));

        assertEquals("UNDER_REVIEW", refreshed(application).getCurrentStatus());
        assertTrue(reviewRepository.findTopByApplicationIdOrderByReviewSequenceDesc(
                application.getApplicationId()).isEmpty());
    }

    @Test
    @DisplayName("VER_TC_011 Verification Audit Trail Logging")
    void VER_TC_011_verificationAuditTrailLogging() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Audit Citizen");
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession(officer)))
                .andExpect(status().is3xxRedirection());

        ApplicationReview review = latestReview(application);
        assertAll(
                () -> assertEquals(application.getApplicationId(), review.getApplicationId()),
                () -> assertEquals(officer.getUserId(), review.getStaffId()),
                () -> assertEquals("APPROVED", review.getDecision()),
                () -> assertEquals(1, review.getReviewSequence()),
                () -> assertNotNull(review.getReviewedAt())
        );
    }

    @Test
    @DisplayName("VER_TC_012 Unauthorized Document Preview Returns Forbidden")
    void VER_TC_012_unauthorizedDocumentPreviewReturnsForbidden() throws Exception {
        CitizenApplication application = application("UNDER_REVIEW", "Private Document Citizen");
        ApplicationDocument document = document(application, "PHOTOGRAPH", "private-photo.png");
        StaffAccount csr = officer(OfficerAccountService.CSR);
        StaffAccount passportOfficer = officer(OfficerAccountService.PASSPORT_OFFICER);
        StaffAccount nicOfficer = officer(OfficerAccountService.NIC_OFFICER);

        MvcResult csrResult = mockMvc.perform(
                get("/officer/verification/documents/{documentId}/view", document.getDocumentId())
                        .session(officerSession(csr)))
                .andReturn();
        MvcResult anonymousResult = mockMvc.perform(
                get("/officer/verification/documents/{documentId}/view", document.getDocumentId()))
                .andReturn();
        MvcResult wrongServiceResult = mockMvc.perform(
                get("/officer/verification/documents/{documentId}/view", document.getDocumentId())
                        .session(officerSession(passportOfficer)))
                .andReturn();
        MvcResult missingDocumentResult = mockMvc.perform(
                get("/officer/verification/documents/{documentId}/view", Integer.MAX_VALUE)
                        .session(officerSession(nicOfficer)))
                .andReturn();

        assertAll(
                () -> assertEquals(403, csrResult.getResponse().getStatus(),
                        "A CSR officer must receive HTTP 403, not a not-found response"),
                () -> assertEquals(403, anonymousResult.getResponse().getStatus(),
                        "An unauthenticated request must receive HTTP 403"),
                () -> assertEquals(403, wrongServiceResult.getResponse().getStatus(),
                        "An officer for another service must receive HTTP 403"),
                () -> assertEquals(404, missingDocumentResult.getResponse().getStatus(),
                        "A missing document must still receive HTTP 404"),
                () -> assertEquals(0, csrResult.getResponse().getContentAsByteArray().length,
                        "No document bytes may be streamed to a CSR officer"),
                () -> assertEquals(0, wrongServiceResult.getResponse().getContentAsByteArray().length,
                        "No document bytes may be streamed to an officer for another service")
        );
    }

    @Test
    @DisplayName("VER_TC_013 Filter Processed Applications by Decision and Date")
    void VER_TC_013_filterProcessedApplicationsByDecisionAndDate() throws Exception {
        String uniqueName = "MarickarFilter" + token();
        CitizenApplication approvedToday = reviewedApplication(
                "APPROVED", uniqueName, LocalDate.now(), OfficerAccountService.NIC_OFFICER);
        reviewedApplication("REJECTED", uniqueName, LocalDate.now(), OfficerAccountService.NIC_OFFICER);
        reviewedApplication("APPROVED", uniqueName, LocalDate.now().minusDays(1),
                OfficerAccountService.NIC_OFFICER);
        StaffAccount officer = officer(OfficerAccountService.NIC_OFFICER);

        mockMvc.perform(get("/officer/nic/applications/processed")
                        .param("search", uniqueName)
                        .param("decision", "APPROVED")
                        .param("date", LocalDate.now().toString())
                        .param("page", "0")
                        .session(officerSession(officer)))
                .andExpect(status().isOk())
                .andExpect(xpath("//tr[@data-review-row]").nodeCount(1))
                .andExpect(xpath("//tr[@data-review-row][contains(., 'Rejected')]").nodeCount(0))
                .andExpect(content().string(containsString(approvedToday.getApplicationNo())))
                .andExpect(xpath("//tr[@data-review-row]//span[normalize-space()='Approved']").nodeCount(1));
    }

    private CitizenApplication reviewedApplication(
            String decision,
            String citizenName,
            LocalDate reviewedOn,
            String officerRole
    ) {
        CitizenApplication application = application(decision, citizenName);
        StaffAccount officer = officer(officerRole);
        ApplicationReview review = new ApplicationReview();
        review.setApplicationId(application.getApplicationId());
        review.setStaffId(officer.getUserId());
        review.setReviewSequence(1);
        review.setDecision(decision);
        review.setReviewedAt(reviewedOn.atTime(10, 30));
        reviewRepository.saveAndFlush(review);
        return application;
    }

    private CitizenApplication application(String status, String citizenName) {
        UserAccount user = user("citizen");
        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName(citizenName);
        citizen.setFamilyName("Perera");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));
        citizen.setPermanentHouseNo("10");
        citizen.setPermanentStreet("Main Street");
        citizen.setPermanentCity("Colombo");
        citizen.setPermanentPostalCode("00100");
        citizenRepository.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("NIC-TC-" + token());
        application.setCitizenId(user.getUserId());
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus(status);
        application.setDeclarationAccepted("DRAFT".equals(status) ? "N" : "Y");
        if (!"DRAFT".equals(status)) application.setSubmittedAt(LocalDateTime.now());
        application = applicationRepository.saveAndFlush(application);

        NicApplication nic = new NicApplication();
        nic.setApplicationId(application.getApplicationId());
        nicApplicationRepository.saveAndFlush(nic);
        return application;
    }

    private ApplicationDocument document(CitizenApplication application, String type, String fileName) {
        ApplicationDocument document = new ApplicationDocument();
        document.setApplicationId(application.getApplicationId());
        document.setDocumentType(type);
        document.setFilePath("marickar/" + token() + "-" + fileName);
        document.setVerificationStatus("PENDING");
        return documentRepository.saveAndFlush(document);
    }

    private StaffAccount officer(String role) {
        UserAccount user = user("officer");
        StaffAccount officer = new StaffAccount();
        officer.setUserId(user.getUserId());
        officer.setEmployeeNo("EMP-" + token());
        officer.setStaffRole(role);
        officer.setStaffStatus("ACTIVE");
        return staffRepository.saveAndFlush(officer);
    }

    private UserAccount user(String prefix) {
        String id = UUID.randomUUID().toString().replace("-", "");
        UserAccount user = new UserAccount();
        user.setEmail(prefix + "-" + id.substring(0, 12) + "@example.com");
        user.setPhone("+947" + String.format("%08d", Integer.toUnsignedLong(id.hashCode()) % 100_000_000));
        user.setPasswordHash("test-only");
        return userRepository.saveAndFlush(user);
    }

    private MockHttpSession officerSession(StaffAccount officer) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", officer.getUserId());
        session.setAttribute("loggedInOfficerRole", officer.getStaffRole());
        session.setAttribute("loggedInOfficerRoleLabel", officer.getStaffRole());
        session.setAttribute("loggedInOfficerEmployeeNo", officer.getEmployeeNo());
        session.setAttribute("loggedInOfficerEmail", "officer@example.com");
        return session;
    }

    private CitizenApplication refreshed(CitizenApplication application) {
        return applicationRepository.findById(application.getApplicationId()).orElseThrow();
    }

    private ApplicationReview latestReview(CitizenApplication application) {
        return reviewRepository.findTopByApplicationIdOrderByReviewSequenceDesc(
                application.getApplicationId()).orElseThrow();
    }

    private void assertNotification(String type, String reference) {
        assertTrue(notificationRepository.findAll().stream().anyMatch(notification ->
                        type.equals(notification.getNotificationType())
                                && notification.getMessage().contains(reference)),
                "Expected citizen notification " + type + " for " + reference);
    }

    private String token() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
