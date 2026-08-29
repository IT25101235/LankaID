package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.SupportRequest;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.SupportRequestRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class CitizenSupportWorkflowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired StaffAccountRepository staffRepository;
    @Autowired SupportRequestRepository supportRepository;
    @Autowired CitizenSupportService supportService;

    @Test
    void citizenSubmitsRequestAndCsrRespondsAndResolvesIt() throws Exception {
        String unique = unique();
        Integer citizenId = createCitizen(unique);
        Integer csrId = createCsr(unique);

        mockMvc.perform(post("/citizen/support")
                        .session(citizenSession(citizenId))
                        .param("requestType", "CONTACT")
                        .param("subject", "Question about my appointment")
                        .param("message", "Please explain which documents I should bring to the appointment."))
                .andExpect(status().is3xxRedirection());

        SupportRequest request = supportRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(
                        citizenId,
                        org.springframework.data.domain.PageRequest.of(0, 15)
                )
                .getContent().get(0);
        assertEquals("CONTACT", request.getRequestType());
        assertEquals("OPEN", request.getRequestStatus());
        String reference = supportService.reference(request.getRequestId());

        mockMvc.perform(post("/officer/csr/support/{reference}/respond", reference)
                        .session(officerSession(csrId))
                        .param("response", "Bring the appointment pass and all original supporting documents."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/csr/support/" + reference));

        request = supportRepository.findById(request.getRequestId()).orElseThrow();
        assertEquals("IN_PROGRESS", request.getRequestStatus());
        assertEquals(csrId, request.getHandledByStaffId());
        assertNotNull(request.getRespondedAt());

        mockMvc.perform(get("/citizen/support/{reference}", reference)
                        .session(citizenSession(citizenId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Bring the appointment pass and all original supporting documents."
                )));

        mockMvc.perform(post("/officer/csr/support/{reference}/resolve", reference)
                        .session(officerSession(csrId)))
                .andExpect(status().is3xxRedirection());

        assertEquals(
                "RESOLVED",
                supportRepository.findById(request.getRequestId()).orElseThrow().getRequestStatus()
        );
    }

    @Test
    void csrCanFilterContactUsAndComplaintRequests() throws Exception {
        String unique = unique();
        Integer citizenId = createCitizen(unique);
        Integer csrId = createCsr(unique);
        createRequest(citizenId, "CONTACT", "Contact subject " + unique);
        createRequest(citizenId, "COMPLAINT", "Complaint subject " + unique);

        mockMvc.perform(get("/officer/csr/support")
                        .param("type", "CONTACT")
                        .session(officerSession(csrId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contact subject " + unique)))
                .andExpect(content().string(not(containsString("Complaint subject " + unique))));

        mockMvc.perform(get("/officer/csr/support")
                        .param("type", "COMPLAINT")
                        .session(officerSession(csrId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Complaint subject " + unique)))
                .andExpect(content().string(not(containsString("Contact subject " + unique))));
    }

    @Test
    void csrQueueLoadsFifteenRequestsPerPage() throws Exception {
        String unique = unique();
        Integer citizenId = createCitizen(unique);
        Integer csrId = createCsr(unique);
        for (int index = 0; index < 16; index++) {
            createRequest(citizenId, "COMPLAINT", "Paged complaint " + unique + "-" + index);
        }

        mockMvc.perform(get("/officer/csr/support")
                        .param("type", "COMPLAINT")
                        .session(officerSession(csrId)))
                .andExpect(status().isOk())
                .andExpect(xpath("//tbody/tr").nodeCount(15))
                .andExpect(content().string(containsString("Load next 15")));

        mockMvc.perform(get("/officer/csr/support")
                        .param("type", "COMPLAINT")
                        .param("page", "1")
                        .session(officerSession(csrId)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Previous 15")));
    }

    private Integer createCitizen(String unique) {
        UserAccount user = new UserAccount();
        user.setEmail("support-citizen-" + unique + "@example.com");
        user.setPhone(phone(unique, 1));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Support");
        citizen.setFamilyName("Citizen");
        citizenRepository.saveAndFlush(citizen);
        return user.getUserId();
    }

    private Integer createCsr(String unique) {
        UserAccount user = new UserAccount();
        user.setEmail("support-csr-" + unique + "@gov.lk");
        user.setPhone(phone(unique, 2));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);

        StaffAccount staff = new StaffAccount(user.getUserId(), "EMP-SUP-" + unique);
        staff.setStaffRole(OfficerAccountService.CSR);
        staffRepository.saveAndFlush(staff);
        return user.getUserId();
    }

    private void createRequest(Integer citizenId, String requestType, String subject) {
        SupportRequest request = new SupportRequest();
        request.setCitizenId(citizenId);
        request.setRequestType(requestType);
        request.setSubject(subject);
        request.setMessage("This is a support request message for integration testing.");
        request.setRequestStatus("OPEN");
        supportRepository.saveAndFlush(request);
    }

    private MockHttpSession citizenSession(Integer citizenId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", citizenId);
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }

    private MockHttpSession officerSession(Integer csrId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", csrId);
        session.setAttribute("loggedInOfficerRole", OfficerAccountService.CSR);
        session.setAttribute("loggedInOfficerRoleLabel", "Citizen Service Representative");
        session.setAttribute("loggedInOfficerEmployeeNo", "EMP-CSR-TEST");
        session.setAttribute("loggedInOfficerEmail", "csr@gov.lk");
        return session;
    }

    private String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String phone(String unique, int suffix) {
        long digits = Integer.toUnsignedLong((unique + suffix).hashCode()) % 100_000_000L;
        return "+947" + String.format("%08d", digits);
    }
}
