package lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Dto.CitizenDeliveryDetails;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Dto.CsrCollectionDetails;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Document_Delivery_Mgmt.Repository.DocumentDeliveryRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentDeliveryService {

    public static final String HOME_DELIVERY = "HOME_DELIVERY";
    public static final String CENTRE_COLLECTION = "CENTRE_COLLECTION";
    public static final String PREPARING = "PREPARING";
    public static final String DISPATCHED = "DISPATCHED";
    public static final String READY_FOR_COLLECTION = "READY_FOR_COLLECTION";
    public static final String DELIVERED = "DELIVERED";
    public static final String COLLECTED = "COLLECTED";

    public static final BigDecimal NIC_HOME_DELIVERY_FEE = new BigDecimal("500.00");
    public static final BigDecimal PASSPORT_HOME_DELIVERY_FEE = new BigDecimal("2000.00");

    private final DocumentDeliveryRepository deliveryRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final CitizenAccountRepository citizenRepository;
    private final UserAccountRepository userRepository;
    private final CentreRepository centreRepository;
    private final GovernmentIdentityRecordRepository governmentRepository;
    private final NotificationService notificationService;

    public DocumentDeliveryService(
            DocumentDeliveryRepository deliveryRepository,
            CitizenApplicationRepository applicationRepository,
            CitizenAccountRepository citizenRepository,
            UserAccountRepository userRepository,
            CentreRepository centreRepository,
            GovernmentIdentityRecordRepository governmentRepository,
            NotificationService notificationService
    ) {
        this.deliveryRepository = deliveryRepository;
        this.applicationRepository = applicationRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.centreRepository = centreRepository;
        this.governmentRepository = governmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<Centre> getAvailableCentres() {
        return centreRepository.findAllByOrderByCentreNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<DocumentDelivery> findByApplicationId(Integer applicationId) {
        return deliveryRepository.findByApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public String getHomeAddress(Integer citizenId) {
        CitizenAccount citizen = findCitizen(citizenId);
        return buildAddress(citizen);
    }

    public BigDecimal homeDeliveryFee(String serviceType) {
        return "NIC".equals(serviceType) ? NIC_HOME_DELIVERY_FEE : PASSPORT_HOME_DELIVERY_FEE;
    }

    public BigDecimal deliveryFee(String serviceType, String deliveryMethod) {
        return HOME_DELIVERY.equals(deliveryMethod) ? homeDeliveryFee(serviceType) : BigDecimal.ZERO;
    }

    @Transactional
    public DocumentDelivery chooseDelivery(
            Integer citizenId,
            Integer applicationId,
            String deliveryMethod,
            Integer centreId
    ) {
        CitizenApplication application = applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        validateMethod(deliveryMethod);

        DocumentDelivery delivery = deliveryRepository.findByApplicationId(applicationId)
                .orElseGet(DocumentDelivery::new);
        if (delivery.getDeliveryId() != null && !PREPARING.equals(delivery.getDeliveryStatus())) {
            throw new IllegalArgumentException("The delivery method cannot be changed after printing has started.");
        }

        delivery.setApplicationId(applicationId);
        delivery.setDeliveryMethod(deliveryMethod);
        delivery.setDeliveryStatus(PREPARING);
        delivery.setCompletedAt(null);
        if (delivery.getTrackingReference() == null) {
            delivery.setTrackingReference(generateTrackingReference());
        }

        if (HOME_DELIVERY.equals(deliveryMethod)) {
            String address = buildAddress(findCitizen(citizenId));
            if (address.isBlank()) {
                throw new IllegalArgumentException(
                        "Your permanent address is incomplete. Update the application address before selecting home delivery."
                );
            }
            delivery.setDeliveryAddress(address);
            delivery.setCentreId(null);
        } else {
            if (centreId == null) {
                throw new IllegalArgumentException("Select a service centre for document collection.");
            }
            Centre centre = centreRepository.findById(centreId)
                    .orElseThrow(() -> new IllegalArgumentException("The selected service centre was not found."));
            delivery.setCentreId(centre.getCentreId());
            delivery.setDeliveryAddress(null);
        }

        DocumentDelivery saved = deliveryRepository.save(delivery);
        notificationService.sendToCitizen(
                citizenId,
                null,
                "DELIVERY_SELECTED",
                deliverySelectionMessage(application, saved)
        );
        return saved;
    }

    @Transactional
    public void updateAfterPrinting(CitizenApplication application, Integer directorId) {
        DocumentDelivery delivery = deliveryRepository.findByApplicationId(application.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application " + application.getApplicationNo()
                                + " has no delivery choice. Ask the citizen to select one from the payment page."
                ));

        String message;
        String type;
        if (HOME_DELIVERY.equals(delivery.getDeliveryMethod())) {
            delivery.setDeliveryStatus(DISPATCHED);
            type = "DOCUMENT_DISPATCHED";
            message = application.getServiceType() + " document " + application.getApplicationNo()
                    + " is now being delivered to your registered address. Tracking reference: "
                    + delivery.getTrackingReference() + ".";
        } else {
            Centre centre = centreRepository.findById(delivery.getCentreId())
                    .orElseThrow(() -> new IllegalArgumentException("The selected collection centre was not found."));
            delivery.setDeliveryStatus(READY_FOR_COLLECTION);
            type = "READY_FOR_COLLECTION";
            message = application.getServiceType() + " document " + application.getApplicationNo()
                    + " is ready for collection at " + centre.getCentreName()
                    + ". Present reference " + delivery.getTrackingReference() + " to the CSR officer.";
        }
        deliveryRepository.save(delivery);
        notificationService.sendToCitizen(application.getCitizenId(), directorId, type, message);
    }

    @Transactional(readOnly = true)
    public Map<Integer, CitizenDeliveryDetails> getCitizenDeliveryMap(Integer citizenId) {
        List<Integer> applicationIds = applicationRepository
                .findAllByCitizenIdOrderByCreatedAtDesc(citizenId)
                .stream()
                .map(CitizenApplication::getApplicationId)
                .toList();
        Map<Integer, CitizenDeliveryDetails> result = new LinkedHashMap<>();
        if (applicationIds.isEmpty()) return result;

        for (DocumentDelivery delivery : deliveryRepository.findAllByApplicationIdIn(applicationIds)) {
            result.put(delivery.getApplicationId(), buildCitizenDetails(delivery));
        }
        return result;
    }

    @Transactional
    public DocumentDelivery confirmHomeDelivery(Integer citizenId, Integer applicationId) {
        CitizenApplication application = applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        DocumentDelivery delivery = deliveryRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Delivery details were not found."));

        if (!HOME_DELIVERY.equals(delivery.getDeliveryMethod())) {
            throw new IllegalArgumentException("Only a home delivery can be confirmed by the citizen.");
        }
        if (DELIVERED.equals(delivery.getDeliveryStatus())) return delivery;
        if (!DISPATCHED.equals(delivery.getDeliveryStatus())) {
            throw new IllegalArgumentException("This document is not currently out for home delivery.");
        }

        delivery.setDeliveryStatus(DELIVERED);
        delivery.setCompletedAt(LocalDateTime.now());
        DocumentDelivery saved = deliveryRepository.save(delivery);
        notificationService.sendToCitizen(
                citizenId,
                null,
                "DOCUMENT_DELIVERED",
                "You confirmed delivery of your " + application.getServiceType()
                        + " document for application " + application.getApplicationNo()
                        + ". Delivery reference: " + delivery.getTrackingReference() + "."
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<CsrCollectionDetails> getReadyCollections() {
        return deliveryRepository
                .findAllByDeliveryMethodAndDeliveryStatusOrderByCreatedAtAsc(
                        CENTRE_COLLECTION,
                        READY_FOR_COLLECTION
                )
                .stream()
                .map(this::buildCsrDetails)
                .toList();
    }

    @Transactional(readOnly = true)
    public CsrCollectionDetails findCollection(String trackingReference) {
        if (trackingReference == null || trackingReference.isBlank()) {
            throw new IllegalArgumentException("Enter the citizen's collection reference.");
        }
        DocumentDelivery delivery = deliveryRepository
                .findByTrackingReferenceIgnoreCase(trackingReference.trim())
                .orElseThrow(() -> new IllegalArgumentException("No document was found for that reference."));
        if (!CENTRE_COLLECTION.equals(delivery.getDeliveryMethod())) {
            throw new IllegalArgumentException("This reference belongs to a home delivery, not centre collection.");
        }
        return buildCsrDetails(delivery);
    }

    @Transactional
    public CsrCollectionDetails handOver(String trackingReference, Integer csrOfficerId) {
        CsrCollectionDetails details = findCollection(trackingReference);
        DocumentDelivery delivery = details.delivery();
        if (COLLECTED.equals(delivery.getDeliveryStatus())) return details;
        if (!READY_FOR_COLLECTION.equals(delivery.getDeliveryStatus())) {
            throw new IllegalArgumentException("This document is not ready to be handed over.");
        }

        delivery.setDeliveryStatus(COLLECTED);
        delivery.setCompletedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        CitizenApplication application = findApplication(delivery.getApplicationId());
        notificationService.sendToCitizen(
                application.getCitizenId(),
                csrOfficerId,
                "DOCUMENT_COLLECTED",
                application.getServiceType() + " document " + application.getApplicationNo()
                        + " was handed over successfully. Collection reference: "
                        + delivery.getTrackingReference() + "."
        );
        return buildCsrDetails(delivery);
    }

    private CitizenDeliveryDetails buildCitizenDetails(DocumentDelivery delivery) {
        if (delivery.getCentreId() == null) {
            return new CitizenDeliveryDetails(delivery, null, null);
        }
        Centre centre = centreRepository.findById(delivery.getCentreId()).orElse(null);
        return new CitizenDeliveryDetails(
                delivery,
                centre == null ? "Service centre" : centre.getCentreName(),
                centre == null ? "" : centreAddress(centre)
        );
    }

    private CsrCollectionDetails buildCsrDetails(DocumentDelivery delivery) {
        CitizenApplication application = findApplication(delivery.getApplicationId());
        CitizenAccount citizen = findCitizen(application.getCitizenId());
        UserAccount user = userRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
        GovernmentIdentityRecord identity = citizen.getGovernmentRecordId() == null
                ? null : governmentRepository.findById(citizen.getGovernmentRecordId()).orElse(null);
        Centre centre = delivery.getCentreId() == null
                ? null : centreRepository.findById(delivery.getCentreId()).orElse(null);
        String documentNumber = identity == null ? "Not generated"
                : "NIC".equals(application.getServiceType()) ? identity.getNicNo() : identity.getPassportNo();

        return new CsrCollectionDetails(
                delivery,
                application.getApplicationNo(),
                application.getServiceType(),
                application.getApplicationKind(),
                citizenName(citizen),
                user.getEmail(),
                user.getPhone(),
                documentNumber,
                centre == null ? "Service centre" : centre.getCentreName(),
                centre == null ? "" : centreAddress(centre)
        );
    }

    private String deliverySelectionMessage(CitizenApplication application, DocumentDelivery delivery) {
        if (HOME_DELIVERY.equals(delivery.getDeliveryMethod())) {
            return "Home delivery was selected for " + application.getServiceType()
                    + " application " + application.getApplicationNo() + ". Delivery reference: "
                    + delivery.getTrackingReference() + ".";
        }
        Centre centre = centreRepository.findById(delivery.getCentreId()).orElseThrow();
        return "Collection at " + centre.getCentreName() + " was selected for "
                + application.getServiceType() + " application " + application.getApplicationNo()
                + ". Collection reference: " + delivery.getTrackingReference() + ".";
    }

    private void validateMethod(String deliveryMethod) {
        if (!HOME_DELIVERY.equals(deliveryMethod) && !CENTRE_COLLECTION.equals(deliveryMethod)) {
            throw new IllegalArgumentException("Select home delivery or service-centre collection.");
        }
    }

    private CitizenApplication findApplication(Integer applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
    }

    private CitizenAccount findCitizen(Integer citizenId) {
        return citizenRepository.findById(citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Citizen details were not found."));
    }

    private String buildAddress(CitizenAccount citizen) {
        return String.join(", ", List.of(
                clean(citizen.getPermanentHouseNo()),
                clean(citizen.getPermanentStreet()),
                clean(citizen.getPermanentCity()),
                clean(citizen.getPermanentPostalCode())
        ).stream().filter(value -> !value.isBlank()).toList());
    }

    private String centreAddress(Centre centre) {
        return String.join(", ", List.of(
                clean(centre.getStreet()),
                clean(centre.getCity()),
                clean(centre.getDistrict())
        ).stream().filter(value -> !value.isBlank()).toList());
    }

    private String citizenName(CitizenAccount citizen) {
        String name = (clean(citizen.getGivenName()) + " " + clean(citizen.getFamilyName())).trim();
        if (!name.isBlank()) return name;
        return clean(citizen.getSurname()).isBlank() ? "Citizen #" + citizen.getUserId() : citizen.getSurname();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String generateTrackingReference() {
        String reference;
        do {
            reference = "DEL-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (deliveryRepository.existsByTrackingReference(reference));
        return reference;
    }
}
