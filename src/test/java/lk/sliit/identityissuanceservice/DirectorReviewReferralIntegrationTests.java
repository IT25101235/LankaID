package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.DirectorReviewReferral;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.DirectorReviewReferralRepository;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class DirectorReviewReferralIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired CitizenApplicationRepository applicationRepository;
    @Autowired StaffAccountRepository staffRepository;
    @Autowired DirectorReviewReferralRepository referralRepository;

    @Test
    void pendingReferralLocksFinalDecisionUntilDirectorReturnsRecommendation() throws Exception {
        CitizenApplication application = createUnderReviewApplication();
        StaffAccount officer = createStaff("NIC_OFFICER", "NIC");
        StaffAccount director = createStaff("DIRECTOR", "DIR");
        MockHttpSession officerSession = officerSession(officer);
        MockHttpSession directorSession = officerSession(director);

        mockMvc.perform(post("/officer/nic/applications/{reference}/director-referral", application.getApplicationNo())
                        .param("category", "IDENTITY_CONFLICT")
                        .param("reason", "The birth record and submitted identity evidence show conflicting names.")
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()));

        DirectorReviewReferral referral = referralRepository
                .findByApplicationIdAndReferralStatus(application.getApplicationId(), "PENDING")
                .orElseThrow();
        assertEquals(officer.getUserId(), referral.getReferredByStaffId());

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()));
        assertEquals("UNDER_REVIEW", applicationRepository.findById(application.getApplicationId())
                .orElseThrow().getCurrentStatus());

        mockMvc.perform(get("/officer/director/referrals").session(directorSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(application.getApplicationNo())))
                .andExpect(content().string(containsString("IDENTITY CONFLICT")));

        mockMvc.perform(get("/officer/director/referrals/{id}", referral.getReferralId())
                        .session(directorSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Director recommendation requested")))
                .andExpect(content().string(containsString("conflicting names")));

        mockMvc.perform(post("/officer/director/referrals/{id}/respond", referral.getReferralId())
                        .param("recommendation", "REQUEST_CORRECTION")
                        .param("comment", "Ask the citizen for an amended birth record before making a final decision.")
                        .session(directorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/director/referrals"));

        DirectorReviewReferral responded = referralRepository.findById(referral.getReferralId()).orElseThrow();
        assertEquals("RESPONDED", responded.getReferralStatus());
        assertEquals("REQUEST_CORRECTION", responded.getRecommendation());
        assertEquals(director.getUserId(), responded.getDirectorId());
        assertNotNull(responded.getRespondedAt());

        mockMvc.perform(get("/officer/nic/applications/{reference}", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Director recommendation")))
                .andExpect(content().string(containsString("amended birth record")))
                .andExpect(content().string(containsString("Approve application")));

        mockMvc.perform(post("/officer/nic/applications/{reference}/approve", application.getApplicationNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"));
        assertEquals("APPROVED", applicationRepository.findById(application.getApplicationId())
                .orElseThrow().getCurrentStatus());
    }

    @Test
    void withdrawalIsARestrictedLogicalDeleteAndKeepsItsAuditRecord() throws Exception {
        CitizenApplication application = createUnderReviewApplication();
        StaffAccount referringOfficer = createStaff("NIC_OFFICER", "NIC");
        StaffAccount anotherOfficer = createStaff("NIC_OFFICER", "NIC");
        MockHttpSession ownerSession = officerSession(referringOfficer);

        createReferral(application, ownerSession);
        DirectorReviewReferral referral = referralRepository
                .findByApplicationIdAndReferralStatus(application.getApplicationId(), "PENDING")
                .orElseThrow();

        mockMvc.perform(post("/officer/nic/applications/{reference}/director-referral/{id}/delete",
                        application.getApplicationNo(), referral.getReferralId())
                        .param("withdrawalReason", "Another officer wants to remove this referral.")
                        .session(officerSession(anotherOfficer)))
                .andExpect(status().is3xxRedirection());
        assertEquals("PENDING", referralRepository.findById(referral.getReferralId())
                .orElseThrow().getReferralStatus());

        String withdrawalReason = "The conflicting entry was confirmed as a harmless data-entry error.";
        mockMvc.perform(post("/officer/nic/applications/{reference}/director-referral/{id}/delete",
                        application.getApplicationNo(), referral.getReferralId())
                        .param("withdrawalReason", withdrawalReason)
                        .session(ownerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications/" + application.getApplicationNo()));

        DirectorReviewReferral withdrawn = referralRepository.findById(referral.getReferralId()).orElseThrow();
        assertEquals("WITHDRAWN", withdrawn.getReferralStatus());
        assertEquals(referringOfficer.getUserId(), withdrawn.getWithdrawnByStaffId());
        assertEquals(withdrawalReason, withdrawn.getWithdrawalReason());
        assertNotNull(withdrawn.getWithdrawnAt());
        assertTrue(referralRepository.existsById(referral.getReferralId()));

        mockMvc.perform(get("/officer/director/referrals").session(officerSession(createStaff("DIRECTOR", "DIR"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(application.getApplicationNo()))));
    }

    private void createReferral(CitizenApplication application, MockHttpSession session) throws Exception {
        mockMvc.perform(post("/officer/nic/applications/{reference}/director-referral", application.getApplicationNo())
                        .param("category", "DOCUMENT_CONCERN")
                        .param("reason", "The supporting certificate contains an unexplained alteration near the seal.")
                        .session(session))
                .andExpect(status().is3xxRedirection());
    }

    private CitizenApplication createUnderReviewApplication() {
        String unique = unique();
        UserAccount user = new UserAccount();
        user.setEmail("referral-citizen-" + unique + "@example.com");
        user.setPhone("+947" + digits(unique));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setFamilyName("Perera");
        citizen.setGivenName("Nimal");
        citizen.setSurname("Perera");
        citizen.setGender("MALE");
        citizen.setCivilStatus("SINGLE");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));
        citizen.setBirthCertificateNo("BC-" + unique);
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

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-NIC-REF-" + unique);
        application.setCitizenId(user.getUserId());
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("UNDER_REVIEW");
        application.setDeclarationAccepted("Y");
        application.setSubmittedAt(LocalDateTime.now());
        return applicationRepository.saveAndFlush(application);
    }

    private StaffAccount createStaff(String role, String prefix) {
        String unique = unique();
        UserAccount user = new UserAccount();
        user.setEmail(role.toLowerCase() + "-" + unique + "@gov.lk");
        user.setPhone(prefix + "-" + unique);
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        StaffAccount staff = new StaffAccount();
        staff.setUserId(user.getUserId());
        staff.setEmployeeNo("EMP-" + prefix + "-" + unique);
        staff.setStaffRole(role);
        staff.setStaffStatus("ACTIVE");
        return staffRepository.saveAndFlush(staff);
    }

    private MockHttpSession officerSession(StaffAccount staff) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", staff.getUserId());
        session.setAttribute("loggedInOfficerRole", staff.getStaffRole());
        session.setAttribute("loggedInOfficerRoleLabel",
                "DIRECTOR".equals(staff.getStaffRole()) ? "Director" : "NIC Application Officer");
        session.setAttribute("loggedInOfficerEmployeeNo", staff.getEmployeeNo());
        session.setAttribute("loggedInOfficerEmail", staff.getEmployeeNo().toLowerCase() + "@gov.lk");
        return session;
    }

    private String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String digits(String value) {
        return String.format("%08d", Math.abs(value.hashCode() % 100_000_000));
    }
}
