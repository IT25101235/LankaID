package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.*;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.*;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.DeletionFormToken;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeletionAndReviewedApplicationsIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository users;
    @Autowired CitizenAccountRepository citizens;
    @Autowired StaffAccountRepository staff;
    @Autowired CitizenApplicationRepository applications;
    @Autowired ApplicationDocumentRepository documents;
    @Autowired SupportRequestRepository support;
    @Autowired CentreRepository centres;
    @Autowired WalkInSlotRepository slots;
    @Autowired AppointmentRepository appointments;
    @Autowired ApplicationReviewRepository reviews;

    @Test void draftDocumentsArePhysicallyDeletedForNewRenewalAndLostApplications() throws Exception {
        for (String kind : java.util.List.of("NEW", "RENEWAL", "LOST")) {
            var application = application("NIC", kind, "DRAFT", "Draft Citizen");
            var document = document(application, "PHOTOGRAPH");
            var session = citizenSession(application.getCitizenId());
            mvc.perform(post(documentDeleteUrl(application, document)).session(session).param("deleteToken", token(session)))
                    .andExpect(redirectedUrl("/citizen/" + ("NEW".equals(kind) ? "applications" : "renewals")
                            + "/nic/documents?applicationId=" + application.getApplicationId()));
            assertFalse(documents.existsById(document.getDocumentId()));
            assertTrue(applications.existsById(application.getApplicationId()));
        }
    }

    @Test void submittedDocumentsOtherOwnersAndMissingTokensAreProtected() throws Exception {
        var application = application("PASSPORT", "NEW", "SUBMITTED", "Protected Citizen");
        var document = document(application, "PHOTOGRAPH");
        var owner = citizenSession(application.getCitizenId());
        mvc.perform(post(documentDeleteUrl(application, document)).session(owner).param("deleteToken", token(owner)))
                .andExpect(flash().attribute("applicationError", containsString("unsubmitted")));
        assertTrue(documents.existsById(document.getDocumentId()));
        application.setCurrentStatus("DRAFT"); applications.saveAndFlush(application);
        var stranger = citizenSession(application.getCitizenId() + 100000);
        mvc.perform(post(documentDeleteUrl(application, document)).session(stranger).param("deleteToken", token(stranger)))
                .andExpect(flash().attributeExists("applicationError"));
        mvc.perform(post(documentDeleteUrl(application, document)).session(owner))
                .andExpect(flash().attribute("applicationError", containsString("Reload")));
        assertTrue(documents.existsById(document.getDocumentId()));
    }

    @Test void documentFromAnotherApplicationCannotBeDeleted() throws Exception {
        var first = application("NIC", "NEW", "DRAFT", "First Citizen");
        var second = application("NIC", "NEW", "DRAFT", "Second Citizen");
        var document = document(second, "PHOTOGRAPH");
        var session = citizenSession(first.getCitizenId());
        mvc.perform(post(documentDeleteUrl(first, document)).session(session).param("deleteToken", token(session)))
                .andExpect(flash().attributeExists("applicationError"));
        assertTrue(documents.existsById(document.getDocumentId()));
    }

    @Test void deletingRequiredUploadPreventsSubmissionAndPageAllowsReplacement() throws Exception {
        var application = application("NIC", "NEW", "DRAFT", "Reupload Citizen");
        var document = document(application, "PHOTOGRAPH");
        var session = citizenSession(application.getCitizenId());
        mvc.perform(get("/citizen/applications/nic/documents").session(session)
                .param("applicationId", application.getApplicationId().toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Delete document")));
        mvc.perform(post(documentDeleteUrl(application, document)).session(session).param("deleteToken", token(session)))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/citizen/applications/nic/submit").session(session)
                .param("applicationId", application.getApplicationId().toString()))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("documentError", containsString("PHOTOGRAPH")));
        assertEquals("DRAFT", application.getCurrentStatus());
    }

    @Test void unansweredContactCanBeDeletedThroughItsDetailPage() throws Exception {
        var request = request("CONTACT", "Question about my appointment");
        var session = citizenSession(request.getCitizenId());
        mvc.perform(get("/citizen/support/" + request.getRequestId()).session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Delete request")));
        mvc.perform(post("/citizen/support/" + request.getRequestId() + "/delete").session(session)
                .param("deleteToken", token(session))).andExpect(redirectedUrl("/citizen/support"));
        assertFalse(support.existsById(request.getRequestId()));
    }

    @Test void unansweredComplaintCanBeDeletedThroughItsDetailPage() throws Exception {
        var request = request("COMPLAINT", "Unanswered complaint");
        var session = citizenSession(request.getCitizenId());
        mvc.perform(get("/citizen/support/" + request.getRequestId()).session(session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Delete request")));
        mvc.perform(post("/citizen/support/" + request.getRequestId() + "/delete").session(session)
                .param("deleteToken", token(session))).andExpect(redirectedUrl("/citizen/support"))
                .andExpect(flash().attributeExists("supportMessage"));
        assertFalse(support.existsById(request.getRequestId()));
    }

    @Test void identityRequestsAndRespondedContactsAndComplaintsCannotBeDeleted() throws Exception {
        for (String type : java.util.List.of("IDENTITY", "RESPONDED_CONTACT", "RESPONDED_COMPLAINT")) {
            var request = request("RESPONDED_COMPLAINT".equals(type) ? "COMPLAINT" : "CONTACT",
                    "IDENTITY".equals(type) ? "Existing document verification" : "Protected request");
            if (type.startsWith("RESPONDED")) {
                request.setResponse("An officer has replied."); request.setRespondedAt(LocalDateTime.now());
                request.setRequestStatus("IN_PROGRESS"); support.saveAndFlush(request);
            }
            var session = citizenSession(request.getCitizenId());
            mvc.perform(post("/citizen/support/" + request.getRequestId() + "/delete").session(session)
                    .param("deleteToken", token(session))).andExpect(flash().attributeExists("supportError"));
            assertTrue(support.existsById(request.getRequestId()));
        }
    }

    @Test void contactDeletionRequiresItsOwnerAndSessionToken() throws Exception {
        var request = request("CONTACT", "Private question");
        var stranger = citizenSession(request.getCitizenId() + 100000);
        mvc.perform(post("/citizen/support/" + request.getRequestId() + "/delete").session(stranger)
                .param("deleteToken", token(stranger))).andExpect(flash().attributeExists("supportError"));
        mvc.perform(post("/citizen/support/" + request.getRequestId() + "/delete").session(citizenSession(request.getCitizenId())))
                .andExpect(flash().attribute("supportError", containsString("Reload")));
        assertTrue(support.existsById(request.getRequestId()));
    }

    @Test void csrDeletesUnusedFutureSlotAndItDisappearsFromAvailability() throws Exception {
        var slot = slot();
        var session = officerSession("CSR");
        mvc.perform(get("/officer/csr/walk-ins").session(session)
                .param("scheduleCentreId", slot.getCentreId().toString())
                .param("scheduleMonth", YearMonth.from(slot.getSlotDate()).toString()))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Delete unused slot")));
        mvc.perform(post(slotDeleteUrl(slot)).session(session).param("deleteToken", token(session)))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("walkInMessage"));
        assertFalse(slots.existsById(slot.getSlotId()));
    }

    @Test void slotsWithCancelledBookingHistoryOrPastDatesCannotBeDeleted() throws Exception {
        var booked = slot();
        var appointment = new Appointment();
        appointment.setAppointmentType("WALK_IN"); appointment.setCentreId(booked.getCentreId());
        appointment.setScheduledAt(booked.getSlotDate().atTime(booked.getSlotTime()));
        appointment.setAppointmentStatus("CANCELLED"); appointment.setReferenceNo("DELETE-TEST-" + unique());
        appointments.saveAndFlush(appointment);
        var session = officerSession("CSR");
        mvc.perform(post(slotDeleteUrl(booked)).session(session).param("deleteToken", token(session)))
                .andExpect(flash().attribute("walkInError", containsString("booking history")));
        assertTrue(slots.existsById(booked.getSlotId()));
        var past = slot(); past.setSlotDate(LocalDate.now().minusDays(1)); slots.saveAndFlush(past);
        mvc.perform(post(slotDeleteUrl(past)).session(session).param("deleteToken", token(session)))
                .andExpect(flash().attribute("walkInError", containsString("future")));
        assertTrue(slots.existsById(past.getSlotId()));
    }

    @Test void citizenCannotDeletePublishedSlots() throws Exception {
        var slot = slot(); var session = citizenSession(99999);
        mvc.perform(post(slotDeleteUrl(slot)).session(session).param("deleteToken", token(session)))
                .andExpect(redirectedUrl("/login"));
        assertTrue(slots.existsById(slot.getSlotId()));
    }

    @Test void reviewedPagesShowRealNamesDecisionsDatesAndCorrectService() throws Exception {
        String unique = unique();
        var nic = review("NIC", "RENEWAL", "APPROVED", "Nimali " + unique, LocalDate.now().minusDays(2));
        var passport = review("PASSPORT", "LOST", "REJECTED", "Pasindu " + unique, LocalDate.now().minusDays(1));
        for (String service : java.util.List.of("nic", "passport")) {
            var included = "nic".equals(service) ? nic : passport;
            var excluded = "nic".equals(service) ? passport : nic;
            mvc.perform(get(reviewUrl(service)).session(officerSession(service.toUpperCase() + "_OFFICER"))
                    .param("search", unique)).andExpect(status().isOk())
                    .andExpect(content().string(containsString(included.getApplicationNo())))
                    .andExpect(content().string(not(containsString(excluded.getApplicationNo()))))
                    .andExpect(content().string(not(containsString("Static demonstration data"))))
                    .andExpect(content().string(not(containsString("LK-2026-00183"))))
                    .andExpect(xpath("//tr[@data-review-row]").nodeCount(1));
        }
    }

    @Test void reviewSearchDecisionAndDateFiltersWorkTogetherForBothServices() throws Exception {
        for (String service : java.util.List.of("NIC", "PASSPORT")) {
            String name = "ReviewFilter " + unique();
            LocalDate date = LocalDate.now().minusDays(3);
            var approved = review(service, "NEW", "APPROVED", name, date);
            review(service, "LOST", "REJECTED", name, date);
            review(service, "RENEWAL", "APPROVED", name, date.plusDays(1));
            mvc.perform(get(reviewUrl(service.toLowerCase())).session(officerSession(service + "_OFFICER"))
                    .param("search", name.toLowerCase()).param("decision", "APPROVED").param("date", date.toString()))
                    .andExpect(status().isOk()).andExpect(xpath("//tr[@data-review-row]").nodeCount(1))
                    .andExpect(content().string(containsString(approved.getApplicationNo())))
                    .andExpect(content().string(containsString("Reset filters")));
            mvc.perform(get(reviewUrl(service.toLowerCase())).session(officerSession(service + "_OFFICER"))
                    .param("search", name).param("decision", "CORRECTION_REQUIRED"))
                    .andExpect(content().string(containsString("No reviewed applications match")));
        }
    }

    @Test void reviewHistoryPaginatesAndBlocksOtherOfficerServices() throws Exception {
        String name = "History " + unique();
        for (int i = 0; i < 16; i++) review("NIC", "NEW", "APPROVED", name, LocalDate.now());
        mvc.perform(get(reviewUrl("nic")).session(officerSession("NIC_OFFICER")).param("search", name))
                .andExpect(status().isOk()).andExpect(xpath("//tr[@data-review-row]").nodeCount(15))
                .andExpect(content().string(containsString("Page 1 of 2")));
        mvc.perform(get(reviewUrl("nic")).session(officerSession("NIC_OFFICER")).param("search", name).param("page", "1"))
                .andExpect(xpath("//tr[@data-review-row]").nodeCount(1));
        mvc.perform(get(reviewUrl("passport")).session(officerSession("NIC_OFFICER"))).andExpect(redirectedUrl("/login"));
        mvc.perform(get(reviewUrl("nic")).session(officerSession("CSR"))).andExpect(redirectedUrl("/login"));
        mvc.perform(get(reviewUrl("nic")).session(officerSession("DIRECTOR")).param("search", name))
                .andExpect(status().isOk());
    }

    @Test void invalidReviewFiltersShowErrorsWithoutReturningUnfilteredRows() throws Exception {
        for (String field : java.util.List.of("decision", "date")) {
            mvc.perform(get(reviewUrl("nic")).session(officerSession("NIC_OFFICER")).param(field, "invalid"))
                    .andExpect(status().isOk()).andExpect(model().attributeExists("filterError"))
                    .andExpect(xpath("//tr[@data-review-row]").nodeCount(0));
        }
    }

    private CitizenApplication review(String service, String kind, String decision, String name, LocalDate date) {
        var application = application(service, kind, decision, name);
        var officer = new StaffAccount(user().getUserId(), "EMP-DEL-" + unique());
        officer.setStaffRole(service + "_OFFICER"); staff.saveAndFlush(officer);
        var review = new ApplicationReview(); review.setApplicationId(application.getApplicationId());
        review.setStaffId(officer.getUserId()); review.setReviewSequence(1); review.setDecision(decision);
        review.setReviewedAt(date.atTime(14, 30)); reviews.saveAndFlush(review);
        return application;
    }
    private CitizenApplication application(String service, String kind, String status, String name) {
        var user = user(); var citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName(name); citizen.setFamilyName("Perera");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));
        citizens.saveAndFlush(citizen);
        var app = new CitizenApplication(); app.setCitizenId(user.getUserId());
        app.setApplicationNo("LK-" + service + "-DEL-" + unique()); app.setServiceType(service);
        app.setApplicationKind(kind); app.setCurrentStatus(status); app.setDeclarationAccepted("Y");
        return applications.saveAndFlush(app);
    }
    private ApplicationDocument document(CitizenApplication app, String type) {
        var doc = new ApplicationDocument(); doc.setApplicationId(app.getApplicationId());
        doc.setDocumentType(type); doc.setFilePath("test-delete/" + unique() + ".pdf");
        doc.setVerificationStatus("PENDING"); return documents.saveAndFlush(doc);
    }
    private SupportRequest request(String type, String subject) {
        var app = application("NIC", "NEW", "DRAFT", "Support Citizen");
        var request = new SupportRequest(); request.setCitizenId(app.getCitizenId());
        request.setRequestType(type); request.setSubject(subject); request.setMessage("Test request message for deletion.");
        request.setRequestStatus("OPEN"); return support.saveAndFlush(request);
    }
    private WalkInSlot slot() {
        var centre = new Centre(); centre.setCentreName("Delete Test " + unique());
        centre.setCity("Colombo"); centre.setDistrict("Colombo"); centre.setDailyCapacity(10); centres.saveAndFlush(centre);
        var slot = new WalkInSlot(); slot.setCentreId(centre.getCentreId());
        slot.setSlotDate(LocalDate.now().plusDays(5)); slot.setSlotTime(LocalTime.of(9, 0));
        slot.setSlotCapacity(1); slot.setIsAvailable("Y"); return slots.saveAndFlush(slot);
    }
    private UserAccount user() {
        var user = new UserAccount(); String id = unique(); user.setEmail("delete-" + id + "@example.com");
        user.setPhone("+947" + String.format("%08d", Integer.toUnsignedLong(id.hashCode()) % 100000000));
        user.setPasswordHash("test-only"); return users.saveAndFlush(user);
    }
    private MockHttpSession citizenSession(Integer id) {
        var session = new MockHttpSession(); session.setAttribute("loggedInUserId", id);
        session.setAttribute("loggedInUserRole", "CITIZEN"); return session;
    }
    private MockHttpSession officerSession(String role) {
        var session = new MockHttpSession(); session.setAttribute("loggedInOfficerId", 99999);
        session.setAttribute("loggedInOfficerRole", role); return session;
    }
    private String token(MockHttpSession session) { return new DeletionFormToken().token(session); }
    private String documentDeleteUrl(CitizenApplication app, ApplicationDocument doc) {
        return "/citizen/applications/" + app.getApplicationId() + "/documents/" + doc.getDocumentId() + "/delete";
    }
    private String slotDeleteUrl(WalkInSlot slot) { return "/officer/csr/walk-ins/schedule/slot/" + slot.getSlotId() + "/delete"; }
    private String reviewUrl(String service) { return "/officer/" + service + "/applications/processed"; }
    private String unique() { return UUID.randomUUID().toString().substring(0, 8); }
}
