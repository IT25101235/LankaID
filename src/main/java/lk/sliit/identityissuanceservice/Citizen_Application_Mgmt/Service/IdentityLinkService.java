package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.SupportRequest;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.SupportRequestRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SriLankanMobileNumber;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class IdentityLinkService {
    public static final String REQUEST_SUBJECT = "Existing document verification";
    private static final String PURPOSE = "IDENTITY_LINK";
    private static final String SESSION_KEY = "pendingIdentityLink";
    private static final Set<String> ACTIVE_APPLICATIONS = Set.of("DRAFT", "SUBMITTED", "UNDER_REVIEW",
            "CORRECTION_REQUIRED", "RESUBMITTED", "APPROVED", "PAYMENT_PENDING", "APPOINTMENT_BOOKED",
            "BIOMETRIC_COLLECTED", "PROCESSING");

    private final CitizenAccountRepository citizens;
    private final UserAccountRepository users;
    private final GovernmentIdentityRecordRepository records;
    private final SupportRequestRepository requests;
    private final StaffAccountRepository staff;
    private final CitizenApplicationRepository applications;
    private final SmsOtpService otp;
    private final NotificationPublisher notificationPublisher;

    public IdentityLinkService(CitizenAccountRepository citizens, UserAccountRepository users,
            GovernmentIdentityRecordRepository records, SupportRequestRepository requests,
            StaffAccountRepository staff, CitizenApplicationRepository applications,
            SmsOtpService otp, NotificationPublisher notificationPublisher) {
        this.citizens = citizens;
        this.users = users;
        this.records = records;
        this.requests = requests;
        this.staff = staff;
        this.applications = applications;
        this.otp = otp;
        this.notificationPublisher = notificationPublisher;
    }

    // Save the claim even if sending the SMS later fails. The citizen can then ask an officer for help.
    @Transactional
    public SupportRequest requestVerification(Integer citizenId, String type, String number, String explanation) {
        requireVerifiedEmail(citizenId);
        CitizenAccount citizen = lockedCitizen(citizenId);
        if (citizen.getGovernmentRecordId() != null) {
            throw new IllegalArgumentException("Your account already has a linked government record. Contact support for corrections.");
        }
        String documentType = clean(type).toUpperCase(Locale.ROOT);
        if (!Set.of("NIC", "PASSPORT").contains(documentType)) {
            throw new IllegalArgumentException("Select NIC or passport.");
        }
        String documentNumber = clean(number).toUpperCase(Locale.ROOT);
        if (documentNumber.length() > 20 || (!documentNumber.isEmpty() && !documentNumber.matches("[A-Z0-9]+"))) {
            throw new IllegalArgumentException("Enter a document number using letters and numbers only (up to 20 characters).");
        }
        String reason = clean(explanation);
        if (reason.length() < 5 || reason.length() > 1500) {
            throw new IllegalArgumentException("Explain your request in 5 to 1500 characters.");
        }
        SupportRequest request = latestRequest(citizenId);
        if (request == null || isClosed(request)) {
            request = new SupportRequest();
            request.setCitizenId(citizenId);
            request.setRequestType("CONTACT");
            request.setSubject(REQUEST_SUBJECT);
            request.setRequestStatus("OPEN");
        }
        request.setMessage("Document: " + documentType + "\nNumber: "
                + (documentNumber.isEmpty() ? "Unknown" : documentNumber) + "\n" + reason);
        return requests.saveAndFlush(request);
    }

    @Transactional(readOnly = true)
    public SupportRequest latestRequest(Integer citizenId) {
        return requests.findFirstByCitizenIdAndRequestTypeAndSubjectOrderByRequestIdDesc(
                citizenId, "CONTACT", REQUEST_SUBJECT).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isLinked(Integer citizenId) {
        return citizens.findById(citizenId).orElseThrow().getGovernmentRecordId() != null;
    }

    @Transactional(readOnly = true)
    public GovernmentIdentityRecord reviewRecord(Integer officerId, Integer recordId) {
        requireOfficer(officerId);
        return records.findById(recordId).orElseThrow(() -> new IllegalArgumentException("Government record was not found."));
    }

    @Transactional(readOnly = true)
    public void sendCode(Integer citizenId, String type, String number, HttpSession session) {
        requireVerifiedEmail(citizenId);
        synchronized (session) {
            // Never leave the previous document's challenge usable when the selection changes.
            session.removeAttribute(SESSION_KEY);
            SupportRequest request = latestRequest(citizenId);
            if (request == null || isClosed(request)) {
                throw new IllegalArgumentException("Submit an identity verification request first.");
            }
            GovernmentIdentityRecord record = findDocument(type, number);
            requireAvailableRecord(record, citizenId);
            String phone;
            try {
                phone = SriLankanMobileNumber.normalize(record.getRegisteredPhone());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("This record has no usable mobile number. Request officer verification.");
            }
            otp.send(phone, PURPOSE, session);
            session.setAttribute(SESSION_KEY, new PendingLink(citizenId, record.getGovernmentRecordId(),
                    request.getRequestId(), phone));
        }
    }

    @Transactional
    public void verifyCode(Integer citizenId, String code, HttpSession session) {
        requireVerifiedEmail(citizenId);
        synchronized (session) {
            PendingLink pending = pending(citizenId, session);
            if (pending == null) throw new IllegalArgumentException("Request a new verification code.");
            CitizenAccount citizen = lockedCitizen(citizenId);
            GovernmentIdentityRecord record = lockedRecord(pending.recordId());
            requireAvailableRecord(record, citizenId);
            if (!pending.phone().equals(SriLankanMobileNumber.normalize(record.getRegisteredPhone()))) {
                throw new IllegalArgumentException("The registered contact changed. Request a new code.");
            }
            SupportRequest request = openRequest(pending.requestId());
            if (!citizenId.equals(request.getCitizenId())) throw new IllegalArgumentException("Request does not belong to this account.");
            otp.verify(code, PURPOSE, session);
            if (!otp.isVerified(pending.phone(), PURPOSE, session)) throw new IllegalArgumentException("Verify your phone first.");
            link(citizen, record);
            finishRequest(request, null, "Verified by SMS OTP sent to the government-registered phone. Record ID: " + record.getGovernmentRecordId());
            otp.clear(PURPOSE, session);
            session.removeAttribute(SESSION_KEY);
        }
    }

    @Transactional
    public void review(Integer requestId, Integer officerId, boolean approve, Integer recordId, String notes) {
        requireOfficer(officerId);
        String reason = clean(notes);
        if (reason.length() < 15 || reason.length() > 1500) {
            throw new IllegalArgumentException("Record the evidence checked and your decision in 15 to 1500 characters.");
        }
        SupportRequest initial = openRequest(requestId);
        CitizenAccount citizen = lockedCitizen(initial.getCitizenId());
        SupportRequest request = openRequest(requestId);
        if (approve) {
            requireVerifiedEmail(citizen.getUserId());
            if (recordId == null) throw new IllegalArgumentException("Enter the verified government record ID.");
            GovernmentIdentityRecord record = lockedRecord(recordId);
            requireAvailableRecord(record, citizen.getUserId());
            link(citizen, record);
        }
        finishRequest(request, officerId, (approve ? "APPROVED. Government record ID: " + recordId : "NOT APPROVED") + "\n" + reason);
    }

    public String maskedPhone(Integer citizenId, HttpSession session) {
        PendingLink pending = pending(citizenId, session);
        return pending == null ? null : "+94 ** *** " + pending.phone().substring(pending.phone().length() - 4);
    }

    public void cancelCode(HttpSession session) {
        otp.clear(PURPOSE, session);
        session.removeAttribute(SESSION_KEY);
    }

    private GovernmentIdentityRecord findDocument(String type, String number) {
        String value = clean(number).toUpperCase(Locale.ROOT);
        if (value.isEmpty() || value.length() > 20) throw new IllegalArgumentException("Enter your document number or request officer verification.");
        List<GovernmentIdentityRecord> matches = switch (clean(type).toUpperCase(Locale.ROOT)) {
            case "NIC" -> records.findAllByNicNoIgnoreCase(value);
            case "PASSPORT" -> records.findAllByPassportNoIgnoreCase(value);
            default -> throw new IllegalArgumentException("Select NIC or passport.");
        };
        if (matches.size() != 1) throw new IllegalArgumentException("Automatic verification is unavailable for these details. Request officer verification.");
        return matches.get(0);
    }

    private void requireAvailableRecord(GovernmentIdentityRecord record, Integer citizenId) {
        if ("CANCELLED".equalsIgnoreCase(record.getIdentityStatus())
                || (clean(record.getNicNo()).isEmpty() && clean(record.getPassportNo()).isEmpty())
                || citizens.existsByGovernmentRecordIdAndUserIdNot(record.getGovernmentRecordId(), citizenId)) {
            throw new IllegalArgumentException("This record cannot be linked automatically. Contact Citizen Support.");
        }
    }

    private void link(CitizenAccount citizen, GovernmentIdentityRecord record) {
        if (citizen.getGovernmentRecordId() != null) {
            throw new IllegalArgumentException("This account already has a government record. Contact support for corrections.");
        }
        for (String service : List.of("NIC", "PASSPORT")) {
            if (applications.existsByCitizenIdAndServiceTypeAndCurrentStatusIn(citizen.getUserId(), service, ACTIVE_APPLICATIONS)) {
                throw new IllegalArgumentException("You have an active application. Delete an unsubmitted draft or ask support to resolve the application before linking.");
            }
        }
        citizen.setGovernmentRecordId(record.getGovernmentRecordId());
        citizens.saveAndFlush(citizen);
        // Do not overwrite the government contact with the citizen's current login contact.
    }

    private void finishRequest(SupportRequest request, Integer officerId, String response) {
        request.setHandledByStaffId(officerId);
        request.setResponse(response);
        request.setRespondedAt(LocalDateTime.now());
        request.setRequestStatus("RESOLVED");
        requests.save(request);
        notificationPublisher.sendToCitizen(request.getCitizenId(), officerId, "IDENTITY_VERIFICATION",
                "Your identity verification request has been updated. View Citizen Support for the decision.");
    }

    private SupportRequest openRequest(Integer id) {
        SupportRequest request = requests.findById(id).orElseThrow(() -> new IllegalArgumentException("Request was not found."));
        if (!REQUEST_SUBJECT.equals(request.getSubject()) || !"CONTACT".equals(request.getRequestType())
                || isClosed(request)) {
            throw new IllegalArgumentException("This identity request is already closed or is not valid.");
        }
        return request;
    }

    private void requireVerifiedEmail(Integer citizenId) {
        UserAccount user = users.findById(citizenId).orElseThrow(() -> new IllegalArgumentException("Sign in again."));
        if (!"Y".equals(user.getEmailVerified()) || !"ACTIVE".equals(user.getAccountStatus())) {
            throw new IllegalArgumentException("An active account with a verified email is required. Complete email verification and sign in again.");
        }
    }

    private void requireOfficer(Integer officerId) {
        StaffAccount officer = staff.findById(officerId)
                .orElseThrow(() -> new IllegalArgumentException("Officer account was not found."));
        UserAccount officerUser = users.findById(officerId).orElseThrow();
        if (!"ACTIVE".equals(officer.getStaffStatus()) || !"ACTIVE".equals(officerUser.getAccountStatus())
                || !Set.of("CSR", "DIRECTOR").contains(officer.getStaffRole())) {
            throw new IllegalArgumentException("Only an active CSR officer or director can verify identity.");
        }
    }

    private CitizenAccount lockedCitizen(Integer id) {
        return citizens.findByIdForIdentityLink(id).orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    private GovernmentIdentityRecord lockedRecord(Integer id) {
        return records.findByIdForIdentityLink(id).orElseThrow(() -> new IllegalArgumentException("Government record was not found."));
    }

    private PendingLink pending(Integer citizenId, HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof PendingLink pending && Objects.equals(pending.citizenId(), citizenId) ? pending : null;
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }

    private boolean isClosed(SupportRequest request) {
        return "RESOLVED".equals(request.getRequestStatus()) || "CLOSED".equals(request.getRequestStatus());
    }

    private record PendingLink(Integer citizenId, Integer recordId, Integer requestId, String phone) implements Serializable { }
}
