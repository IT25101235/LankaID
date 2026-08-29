package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.SupportRequestForm;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.SupportRequestView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model.SupportRequest;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.SupportRequestRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Set;

@Service
public class CitizenSupportService {

    public static final int PAGE_SIZE = 15;
    public static final String CONTACT = "CONTACT";
    public static final String COMPLAINT = "COMPLAINT";
    public static final String OPEN = "OPEN";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String RESOLVED = "RESOLVED";

    private static final Set<String> REQUEST_TYPES = Set.of(CONTACT, COMPLAINT);
    private static final Set<String> REQUEST_STATUSES = Set.of(OPEN, IN_PROGRESS, RESOLVED);

    private final SupportRequestRepository supportRepository;
    private final CitizenAccountRepository citizenRepository;
    private final UserAccountRepository userRepository;
    private final StaffAccountRepository staffRepository;
    private final NotificationService notificationService;

    public CitizenSupportService(
            SupportRequestRepository supportRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            StaffAccountRepository staffRepository,
            NotificationService notificationService
    ) {
        this.supportRepository = supportRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.staffRepository = staffRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SupportRequestView createRequest(Integer citizenId, SupportRequestForm form) {
        findUser(citizenId);
        String requestType = clean(form.getRequestType()).toUpperCase();
        if (!REQUEST_TYPES.contains(requestType)) {
            throw new IllegalArgumentException("Select Contact Us or Complaint.");
        }

        SupportRequest request = new SupportRequest();
        request.setCitizenId(citizenId);
        request.setRequestType(requestType);
        request.setSubject(clean(form.getSubject()));
        request.setMessage(clean(form.getMessage()));
        request.setRequestStatus(OPEN);
        request = supportRepository.save(request);

        notificationService.sendToCitizen(
                citizenId,
                null,
                "SUPPORT_REQUEST_CREATED",
                typeLabel(requestType) + " request " + reference(request.getRequestId())
                        + " was submitted successfully. A CSR officer will review it."
        );
        return buildView(request);
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestView> getCitizenRequests(Integer citizenId, int pageNumber) {
        return supportRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(
                        citizenId,
                        PageRequest.of(Math.max(pageNumber, 0), PAGE_SIZE)
                )
                .map(this::buildView);
    }

    @Transactional(readOnly = true)
    public SupportRequestView getCitizenRequest(Integer citizenId, String reference) {
        Integer requestId = requestId(reference);
        SupportRequest request = supportRepository
                .findByRequestIdAndCitizenId(requestId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Support request was not found."));
        return buildView(request);
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestView> getCsrRequests(
            String requestType,
            String requestStatus,
            int pageNumber
    ) {
        String type = filterValue(requestType, REQUEST_TYPES);
        String status = filterValue(requestStatus, REQUEST_STATUSES);
        PageRequest pageRequest = PageRequest.of(Math.max(pageNumber, 0), PAGE_SIZE);

        Page<SupportRequest> requests;
        if (type.isBlank() && status.isBlank()) {
            requests = supportRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        } else if (status.isBlank()) {
            requests = supportRepository.findAllByRequestTypeOrderByCreatedAtDesc(type, pageRequest);
        } else if (type.isBlank()) {
            requests = supportRepository.findAllByRequestStatusOrderByCreatedAtDesc(status, pageRequest);
        } else {
            requests = supportRepository
                    .findAllByRequestTypeAndRequestStatusOrderByCreatedAtDesc(type, status, pageRequest);
        }
        return requests.map(this::buildView);
    }

    @Transactional(readOnly = true)
    public SupportRequestView getCsrRequest(String reference) {
        return buildView(findRequest(reference));
    }

    @Transactional(readOnly = true)
    public List<SupportRequestView> getRecentRequests() {
        return supportRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 3))
                .map(this::buildView)
                .getContent();
    }

    @Transactional
    public SupportRequestView respond(String reference, Integer staffId, String response) {
        SupportRequest request = findRequest(reference);
        String cleanResponse = clean(response);
        if (cleanResponse.length() < 5 || cleanResponse.length() > 2000) {
            throw new IllegalArgumentException("Response must contain between 5 and 2000 characters.");
        }
        if (RESOLVED.equals(request.getRequestStatus())) {
            throw new IllegalArgumentException("This request is already resolved.");
        }

        request.setHandledByStaffId(staffId);
        request.setResponse(cleanResponse);
        request.setRespondedAt(LocalDateTime.now());
        request.setRequestStatus(IN_PROGRESS);
        request = supportRepository.save(request);

        notificationService.sendToCitizen(
                request.getCitizenId(),
                staffId,
                "CSR_RESPONSE",
                "A CSR officer responded to " + reference(request.getRequestId())
                        + ": " + shortMessage(cleanResponse)
        );
        return buildView(request);
    }

    @Transactional
    public SupportRequestView resolve(String reference, Integer staffId) {
        SupportRequest request = findRequest(reference);
        if (request.getResponse() == null || request.getResponse().isBlank()) {
            throw new IllegalArgumentException("Send a response before resolving this request.");
        }
        request.setHandledByStaffId(staffId);
        request.setRequestStatus(RESOLVED);
        if (request.getRespondedAt() == null) request.setRespondedAt(LocalDateTime.now());
        request = supportRepository.save(request);

        notificationService.sendToCitizen(
                request.getCitizenId(),
                staffId,
                "SUPPORT_REQUEST_RESOLVED",
                typeLabel(request.getRequestType()) + " request " + reference(request.getRequestId())
                        + " has been marked as resolved."
        );
        return buildView(request);
    }

    @Transactional(readOnly = true)
    public long getOpenCount() {
        return supportRepository.countByRequestStatus(OPEN);
    }

    @Transactional(readOnly = true)
    public long getRespondedCount() {
        return supportRepository.countByRequestStatus(IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public long getResolvedCount() {
        return supportRepository.countByRequestStatus(RESOLVED);
    }

    @Transactional(readOnly = true)
    public long getCitizenOpenCount(Integer citizenId) {
        return supportRepository.countByCitizenIdAndRequestStatus(citizenId, OPEN);
    }

    @Transactional(readOnly = true)
    public long getCitizenRespondedCount(Integer citizenId) {
        return supportRepository.countByCitizenIdAndRequestStatus(citizenId, IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public long getCitizenResolvedCount(Integer citizenId) {
        return supportRepository.countByCitizenIdAndRequestStatus(citizenId, RESOLVED);
    }

    public String reference(Integer requestId) {
        return "SUP-" + Year.now().getValue() + "-" + String.format("%06d", requestId);
    }

    public String typeLabel(String requestType) {
        return COMPLAINT.equals(requestType) ? "Complaint" : "Contact Us";
    }

    private SupportRequest findRequest(String reference) {
        return supportRepository.findById(requestId(reference))
                .orElseThrow(() -> new IllegalArgumentException("Support request was not found."));
    }

    private Integer requestId(String reference) {
        String value = clean(reference);
        if (value.isBlank()) throw new IllegalArgumentException("Support request was not found.");
        String idPart = value.contains("-") ? value.substring(value.lastIndexOf('-') + 1) : value;
        try {
            return Integer.valueOf(idPart);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Support request was not found.");
        }
    }

    private SupportRequestView buildView(SupportRequest request) {
        UserAccount user = findUser(request.getCitizenId());
        CitizenAccount citizen = citizenRepository.findById(request.getCitizenId()).orElse(null);
        String employeeNo = request.getHandledByStaffId() == null ? "Not assigned"
                : staffRepository.findById(request.getHandledByStaffId())
                .map(StaffAccount::getEmployeeNo)
                .orElse("Officer unavailable");
        return new SupportRequestView(
                request,
                reference(request.getRequestId()),
                citizenName(citizen, request.getCitizenId()),
                user.getEmail(),
                user.getPhone(),
                employeeNo
        );
    }

    private UserAccount findUser(Integer citizenId) {
        return userRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
    }

    private String citizenName(CitizenAccount citizen, Integer citizenId) {
        if (citizen == null) return "Citizen #" + citizenId;
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        return clean(citizen.getSurname()).isBlank() ? "Citizen #" + citizenId : citizen.getSurname();
    }

    private String filterValue(String value, Set<String> allowedValues) {
        String result = clean(value).toUpperCase();
        return allowedValues.contains(result) ? result : "";
    }

    private String shortMessage(String message) {
        return message.length() <= 160 ? message : message.substring(0, 157) + "...";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
