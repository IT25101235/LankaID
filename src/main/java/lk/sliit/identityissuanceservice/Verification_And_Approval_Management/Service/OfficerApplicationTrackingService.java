package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricCollection;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Repository.DocumentDeliveryRepository;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.OfficerApplicationTrackingItem;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Model.ApplicationReview;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates an application's complete lifecycle for officer tracking.
 * Data from verification, payment, appointment, biometric and delivery modules is combined into
 * one immutable DTO without changing any workflow state.
 */
@Service
public class OfficerApplicationTrackingService {

    public static final int PAGE_SIZE = 15;

    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final UserAccountRepository userRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final BiometricCollectionRepository biometricRepository;
    private final DocumentDeliveryRepository deliveryRepository;

    public OfficerApplicationTrackingService(
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            ApplicationReviewRepository reviewRepository,
            AppointmentRepository appointmentRepository,
            PaymentRepository paymentRepository,
            BiometricCollectionRepository biometricRepository,
            DocumentDeliveryRepository deliveryRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.biometricRepository = biometricRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional(readOnly = true)
    public Page<OfficerApplicationTrackingItem> getTrackingPage(
            String officerRole,
            String search,
            int pageNumber
    ) {
        // Convert the logged-in role into a database scope before searching or paging.
        String serviceScope = serviceScope(officerRole);
        String applicationReference = clean(search);
        PageRequest pageRequest = PageRequest.of(Math.max(pageNumber, 0), PAGE_SIZE);

        Page<CitizenApplication> applications;
        if (serviceScope == null && applicationReference.isBlank()) {
            applications = applicationRepository.findAllByOrderByUpdatedAtDesc(pageRequest);
        } else if (serviceScope == null) {
            applications = applicationRepository
                    .findAllByApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
                            applicationReference,
                            pageRequest
                    );
        } else if (applicationReference.isBlank()) {
            applications = applicationRepository
                    .findAllByServiceTypeOrderByUpdatedAtDesc(serviceScope, pageRequest);
        } else {
            applications = applicationRepository
                    .findAllByServiceTypeAndApplicationNoContainingIgnoreCaseOrderByUpdatedAtDesc(
                            serviceScope,
                            applicationReference,
                            pageRequest
                    );
        }

        return applications.map(this::buildItem);
    }

    private OfficerApplicationTrackingItem buildItem(CitizenApplication application) {
        // Each optional stage may not exist yet, so missing downstream records are represented by null.
        CitizenAccount citizen = citizenRepository.findById(application.getCitizenId()).orElse(null);
        UserAccount user = userRepository.findById(application.getCitizenId()).orElse(null);
        ApplicationReview review = reviewRepository
                .findTopByApplicationIdOrderByReviewSequenceDesc(application.getApplicationId())
                .orElse(null);
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(application.getApplicationId(), "BIOMETRIC")
                .orElse(null);
        Payment payment = appointment == null ? null : paymentRepository
                .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                        appointment.getAppointmentId(), "PAID"
                )
                .orElse(null);
        BiometricCollection biometric = biometricRepository
                .findById(application.getApplicationId())
                .orElse(null);
        DocumentDelivery delivery = deliveryRepository
                .findByApplicationId(application.getApplicationId())
                .orElse(null);

        return new OfficerApplicationTrackingItem(
                application,
                citizenName(citizen, application.getCitizenId()),
                user == null ? "Account unavailable" : user.getEmail(),
                review,
                appointment,
                payment,
                biometric,
                delivery
        );
    }

    private String serviceScope(String officerRole) {
        // Director has no service filter; each application officer receives a fixed service filter.
        return switch (officerRole) {
            case OfficerAccountService.DIRECTOR -> null;
            case OfficerAccountService.NIC_OFFICER -> "NIC";
            case OfficerAccountService.PASSPORT_OFFICER -> "PASSPORT";
            default -> throw new IllegalArgumentException("This officer role cannot view application tracking.");
        };
    }

    private String citizenName(CitizenAccount citizen, Integer citizenId) {
        if (citizen == null) return "Citizen #" + citizenId;
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        return clean(citizen.getSurname()).isBlank() ? "Citizen #" + citizenId : citizen.getSurname();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
