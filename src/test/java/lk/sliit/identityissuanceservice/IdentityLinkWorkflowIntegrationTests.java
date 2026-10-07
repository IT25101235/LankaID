package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.IdentityLinkService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Sms.SmsSender;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentityLinkWorkflowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired CitizenAccountRepository citizens;
    @Autowired GovernmentIdentityRecordRepository records;
    @Autowired CitizenApplicationRepository applications;
    @Autowired StaffAccountRepository staff;
    @Autowired IdentityLinkService service;
    @Autowired CitizenApplicationService eligibility;
    @Autowired UserAccountService registration;
    @Autowired CitizenSupportService support;
    @MockitoBean SmsSender sms;

    @Test
    void verifiedRegistrationSelectsTheCorrectServicesForEachRegistryCase() throws Exception {
        for (String documents : java.util.List.of("NIC", "PASSPORT", "BOTH")) {
            GovernmentIdentityRecord record = record();
            if ("NIC".equals(documents)) record.setPassportNo(null);
            if ("PASSPORT".equals(documents)) record.setNicNo(null);
            records.saveAndFlush(record);
            var prepared = registration.prepareCitizenRegistration(
                    record.getRegisteredEmail().toUpperCase(), "0" + record.getRegisteredPhone().substring(3), "Demo-password1");
            UserAccount user = registration.registerVerifiedCitizen(prepared);
            assertEquals(record.getGovernmentRecordId(), citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
            String html = mvc.perform(get("/citizen/dashboard").session(session(user)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            boolean hasNic = record.getNicNo() != null;
            boolean hasPassport = record.getPassportNo() != null;
            assertEquals(hasNic, html.contains(">Renew NIC<"));
            assertEquals(hasNic, html.contains(">Lost NIC<"));
            assertEquals(!hasNic, html.contains(">New NIC<"));
            assertEquals(hasPassport, html.contains(">Renew Passport<"));
            assertEquals(hasPassport, html.contains(">Lost Passport<"));
            assertEquals(!hasPassport, html.contains(">New Passport<"));
            assertFalse(html.contains("Verify record or request support"));
            if (hasNic) assertThrows(IllegalArgumentException.class, () -> eligibility.prepareNewNicForm(user.getUserId()));
            if (hasPassport) assertThrows(IllegalArgumentException.class, () -> eligibility.prepareNewPassportForm(user.getUserId()));
        }
        verifyNoInteractions(sms);
    }

    @Test
    void registrationWithNoRegistryMatchStillOffersNewApplications() throws Exception {
        String email = "new-" + UUID.randomUUID() + "@example.com";
        UserAccount user = registration.registerVerifiedCitizen(registration.prepareCitizenRegistration(email, phone(), "Demo-password1"));
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
        mvc.perform(get("/citizen/dashboard").session(session(user))).andExpect(status().isOk())
                .andExpect(content().string(containsString(">New NIC<")))
                .andExpect(content().string(containsString(">New Passport<")))
                .andExpect(content().string(not(containsString(">Renew NIC<"))));
    }

    @Test
    void partialOrConflictingRegistryContactsBlockNewApplications() throws Exception {
        UserAccount user = citizen();
        GovernmentIdentityRecord first = record();
        first.setRegisteredEmail(user.getEmail());
        records.saveAndFlush(first);
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        GovernmentIdentityRecord second = record();
        second.setRegisteredPhone(user.getPhone());
        records.saveAndFlush(second);
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
        mvc.perform(get("/citizen/dashboard").session(session(user))).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">New NIC<"))))
                .andExpect(content().string(not(containsString(">New Passport<"))))
                .andExpect(content().string(containsString("Verify record or request support")));
    }

    @Test
    void automaticMatchDoesNotBypassCancelledOrAlreadyClaimedRecords() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        record.setRegisteredEmail(user.getEmail());
        record.setRegisteredPhone(user.getPhone());
        record.setIdentityStatus("CANCELLED");
        records.saveAndFlush(record);
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        record.setIdentityStatus("ACTIVE");
        records.saveAndFlush(record);
        UserAccount owner = citizen();
        CitizenAccount linkedOwner = citizens.findById(owner.getUserId()).orElseThrow();
        linkedOwner.setGovernmentRecordId(record.getGovernmentRecordId());
        citizens.saveAndFlush(linkedOwner);
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
    }

    @Test
    void existingHolderLinksUsingGovernmentPhoneAndImmediatelyGetsRenewalForms() throws Exception {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        MockHttpSession session = session(user);
        mvc.perform(get("/citizen/identity").session(session)).andExpect(status().isOk());
        mvc.perform(post("/citizen/identity/request").session(session)
                        .param("identityToken", token(session)).param("type", "PASSPORT")
                        .param("number", record.getPassportNo()).param("explanation", "I have an existing passport to renew.")
                        .param("action", "OTP"))
                .andExpect(redirectedUrl("/citizen/identity"));
        String code = sentCode(record.getRegisteredPhone());
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        mvc.perform(get("/citizen/identity").session(session)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(record.getRegisteredPhone()))));
        mvc.perform(post("/citizen/identity/verify").session(session)
                        .param("identityToken", token(session)).param("code", code))
                .andExpect(redirectedUrl("/citizen/dashboard"));
        assertEquals(record.getGovernmentRecordId(), citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
        assertTrue(eligibility.getEligibility(user.getUserId()).isHasNic());
        assertTrue(eligibility.getEligibility(user.getUserId()).isHasPassport());
        assertEquals("RESOLVED", service.latestRequest(user.getUserId()).getRequestStatus());
        assertNotNull(service.latestRequest(user.getUserId()).getRespondedAt());
        mvc.perform(get("/citizen/applications/passport/renewal").session(session)).andExpect(status().isOk());
        mvc.perform(get("/citizen/applications/nic/replacement").session(session)).andExpect(status().isOk());
        mvc.perform(get("/citizen/applications/nic/new").session(session)).andExpect(redirectedUrl("/citizen/dashboard"));
        mvc.perform(post("/citizen/identity/verify").session(session)
                .param("identityToken", token(session)).param("code", code)).andExpect(redirectedUrl("/citizen/identity"));
    }

    @Test
    void matchingEmailAndPhoneAutomaticallyLinkAnIdentityAfterEmailVerification() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        record.setRegisteredEmail(user.getEmail());
        record.setRegisteredPhone(user.getPhone());
        records.saveAndFlush(record);
        var result = eligibility.getEligibility(user.getUserId());
        assertTrue(result.isHasNic());
        assertFalse(result.isIdentityConflict());
        assertEquals(record.getGovernmentRecordId(), citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
    }

    @Test
    void wrongCodeAndDifferentSessionCannotLinkAndUnverifiedEmailCannotRequest() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        MockHttpSession session = session(user);
        requestCode(user, record, session);
        String code = sentCode(record.getRegisteredPhone());
        String wrong = "000000".equals(code) ? "111111" : "000000";
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user.getUserId(), wrong, session));
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user.getUserId(), code, new MockHttpSession()));
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
        UserAccount unverified = citizen();
        unverified.setEmailVerified("N");
        users.saveAndFlush(unverified);
        assertThrows(IllegalArgumentException.class, () -> service.requestVerification(unverified.getUserId(), "NIC", record.getNicNo(), "Please check my identity."));
    }

    @Test
    void failedSmsLeavesAnOfficerRequestAndBlocksNewApplications() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        doThrow(new IllegalArgumentException("SMS unavailable")).when(sms).send(anyString(), anyString(), anyInt());
        service.requestVerification(user.getUserId(), "NIC", record.getNicNo(), "My existing identity needs verification.");
        assertThrows(IllegalArgumentException.class, () -> service.sendCode(user.getUserId(), "NIC", record.getNicNo(), session(user)));
        assertNotNull(service.latestRequest(user.getUserId()));
        assertThrows(IllegalArgumentException.class, () -> eligibility.prepareNewNicForm(user.getUserId()));
    }

    @Test
    void officerCanVerifyAnOutdatedContactAndNicOnlyHolderCanApplyForFirstPassport() throws Exception {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        record.setPassportNo(null);
        records.saveAndFlush(record);
        var request = service.requestVerification(user.getUserId(), "NIC", record.getNicNo(), "I cannot access the old registered phone.");
        UserAccount officer = officer("CSR");
        MockHttpSession officerSession = new MockHttpSession();
        officerSession.setAttribute("loggedInOfficerId", officer.getUserId());
        officerSession.setAttribute("loggedInOfficerRole", "CSR");
        mvc.perform(get("/officer/csr/support/" + request.getRequestId()).session(officerSession))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Review identity and record decision")));
        mvc.perform(get("/officer/csr/identity/" + request.getRequestId()).session(officerSession)
                        .param("recordId", record.getGovernmentRecordId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString(record.getNicNo())));
        mvc.perform(post("/officer/csr/identity/" + request.getRequestId()).session(officerSession)
                        .param("identityToken", token(officerSession)).param("decision", "APPROVE")
                        .param("recordId", record.getGovernmentRecordId().toString()).param("evidenceChecked", "true")
                        .param("notes", "Original identity evidence checked in person against the government record."))
                .andExpect(redirectedUrl("/officer/csr/support/" + request.getRequestId()));
        assertEquals(officer.getUserId(), service.latestRequest(user.getUserId()).getHandledByStaffId());
        assertTrue(eligibility.getEligibility(user.getUserId()).isHasNic());
        assertFalse(eligibility.getEligibility(user.getUserId()).isHasPassport());
        mvc.perform(get("/citizen/applications/passport/new").session(session(user))).andExpect(status().isOk());
        assertNotEquals(user.getPhone(), record.getRegisteredPhone());
    }

    @Test
    void recordCannotBeClaimedByTwoAccountsAndWrongOfficerCannotApprove() {
        UserAccount first = citizen();
        UserAccount second = citizen();
        GovernmentIdentityRecord record = record();
        var request = service.requestVerification(first.getUserId(), "NIC", record.getNicNo(), "Please verify my old document.");
        UserAccount wrongOfficer = officer("SYSTEM_ADMIN");
        assertThrows(IllegalArgumentException.class, () -> service.review(request.getRequestId(), wrongOfficer.getUserId(), true,
                record.getGovernmentRecordId(), "Evidence reviewed in person at the service centre."));
        UserAccount csr = officer("CSR");
        service.review(request.getRequestId(), csr.getUserId(), true, record.getGovernmentRecordId(), "Evidence reviewed in person at the service centre.");
        var secondRequest = service.requestVerification(second.getUserId(), "NIC", record.getNicNo(), "Please verify this document.");
        assertThrows(IllegalArgumentException.class, () -> service.review(secondRequest.getRequestId(), csr.getUserId(), true,
                record.getGovernmentRecordId(), "Evidence reviewed in person at the service centre."));
        assertNull(citizens.findById(second.getUserId()).orElseThrow().getGovernmentRecordId());
    }

    @Test
    void missingRecordAndRejectedRequestStayBlockedAndCanBeResubmitted() {
        UserAccount user = citizen();
        var request = service.requestVerification(user.getUserId(), "NIC", "", "I lost my NIC and do not remember its number.");
        assertThrows(IllegalArgumentException.class, () -> service.sendCode(user.getUserId(), "NIC", "", session(user)));
        UserAccount csr = officer("CSR");
        assertThrows(IllegalArgumentException.class, () -> support.resolve(request.getRequestId().toString(), csr.getUserId()));
        service.review(request.getRequestId(), csr.getUserId(), false, null, "Please attend the service centre with supporting evidence before approval.");
        assertTrue(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        var next = service.requestVerification(user.getUserId(), "NIC", "", "I now have the supporting evidence for review.");
        assertNotEquals(request.getRequestId(), next.getRequestId());
    }

    @Test
    void firstTimeApplicantStillWorksAndIdentityPostsRequireLoginAndFormToken() throws Exception {
        UserAccount user = citizen();
        MockHttpSession session = session(user);
        assertFalse(eligibility.getEligibility(user.getUserId()).isIdentityConflict());
        mvc.perform(get("/citizen/applications/nic/new").session(session)).andExpect(status().isOk());
        mvc.perform(get("/citizen/identity")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/officer/csr/identity/1").session(session)).andExpect(redirectedUrl("/login"));
        mvc.perform(post("/citizen/identity/verify").session(session).param("code", "123456")
                .param("identityToken", "forged")).andExpect(status().isForbidden());
    }

    @Test
    void cancelledRecordCannotBeLinkedButExpiredDocumentCanBeRenewed() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        record.setIdentityStatus("CANCELLED");
        records.saveAndFlush(record);
        service.requestVerification(user.getUserId(), "NIC", record.getNicNo(), "Please verify my old document.");
        assertThrows(IllegalArgumentException.class, () -> service.sendCode(user.getUserId(), "NIC", record.getNicNo(), session(user)));
        verifyNoInteractions(sms);
        record.setIdentityStatus("EXPIRED");
        records.saveAndFlush(record);
        UserAccount csr = officer("CSR");
        service.review(service.latestRequest(user.getUserId()).getRequestId(), csr.getUserId(), true,
                record.getGovernmentRecordId(), "Expired passport checked against the registry in person.");
        assertTrue(eligibility.getEligibility(user.getUserId()).isHasPassport());
    }

    @Test
    void changedGovernmentPhoneInvalidatesPendingOtp() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        MockHttpSession session = session(user);
        requestCode(user, record, session);
        String code = sentCode(record.getRegisteredPhone());
        record.setRegisteredPhone(phone());
        records.saveAndFlush(record);
        assertThrows(IllegalArgumentException.class, () -> service.verifyCode(user.getUserId(), code, session));
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
    }

    @Test
    void activeDraftBlocksLinkingAndPendingVerificationBlocksItsSubmission() {
        UserAccount user = citizen();
        GovernmentIdentityRecord record = record();
        CitizenApplication draft = new CitizenApplication();
        draft.setCitizenId(user.getUserId());
        draft.setApplicationNo("ID-" + UUID.randomUUID().toString().substring(0, 15));
        draft.setServiceType("NIC");
        draft.setApplicationKind("NEW");
        draft.setCurrentStatus("DRAFT");
        draft.setDeclarationAccepted("Y");
        applications.saveAndFlush(draft);
        var request = service.requestVerification(user.getUserId(), "NIC", record.getNicNo(), "I already own a NIC and created the wrong draft.");
        UserAccount csr = officer("CSR");
        assertThrows(IllegalArgumentException.class, () -> service.review(request.getRequestId(), csr.getUserId(), true,
                record.getGovernmentRecordId(), "Evidence was checked in person against the record."));
        assertThrows(IllegalArgumentException.class, () -> eligibility.submitNewApplication(user.getUserId(), draft.getApplicationId()));
        assertNull(citizens.findById(user.getUserId()).orElseThrow().getGovernmentRecordId());
    }

    private void requestCode(UserAccount user, GovernmentIdentityRecord record, MockHttpSession session) {
        service.requestVerification(user.getUserId(), "NIC", record.getNicNo(), "I have an existing document to link.");
        service.sendCode(user.getUserId(), "NIC", record.getNicNo(), session);
    }

    private String sentCode(String phone) {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(sms).send(eq(phone), message.capture(), anyInt());
        return message.getValue().substring(message.getValue().length() - 6);
    }

    private UserAccount citizen() {
        UserAccount user = user();
        citizens.saveAndFlush(new CitizenAccount(user.getUserId()));
        return user;
    }

    private UserAccount officer(String role) {
        UserAccount user = user();
        StaffAccount account = new StaffAccount(user.getUserId(), "ID-" + UUID.randomUUID().toString().substring(0, 8));
        account.setStaffRole(role);
        staff.saveAndFlush(account);
        return user;
    }

    private UserAccount user() {
        UserAccount user = new UserAccount();
        user.setEmail("identity-" + UUID.randomUUID() + "@example.com");
        user.setPhone(phone());
        user.setPasswordHash("test-only");
        user.setEmailVerified("Y");
        return users.saveAndFlush(user);
    }

    private GovernmentIdentityRecord record() {
        GovernmentIdentityRecord record = new GovernmentIdentityRecord();
        String digits = String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
        record.setNicNo("1990" + digits);
        record.setPassportNo("N" + digits);
        record.setRegisteredEmail("registry-" + UUID.randomUUID() + "@example.com");
        record.setRegisteredPhone(phone());
        record.setIdentityStatus("ACTIVE");
        return records.saveAndFlush(record);
    }

    private String phone() { return "+947" + String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000)); }

    private MockHttpSession session(UserAccount user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", user.getUserId());
        session.setAttribute("loggedInUserRole", "CITIZEN");
        session.setAttribute("loggedInUserEmail", user.getEmail());
        return session;
    }

    private String token(MockHttpSession session) { return (String) session.getAttribute("identityFormToken"); }
}
