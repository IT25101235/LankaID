package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricCollection;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Service: finishes the biometric step for an application. */
@Service
public class BiometricService {

    // OOP - Composition and Encapsulation: the services and repositories is a private final field handed
    // in through the constructor (dependency injection), so this class is loosely coupled and the object
    // is never created here.
    private final AppointmentService appointmentService;
    private final AppointmentRepository appointmentRepository;
    private final BiometricCollectionRepository biometricRepository;
    private final CitizenApplicationRepository applicationRepository;
    private final NotificationPublisher notificationPublisher;

    public BiometricService(
            AppointmentService appointmentService,
            AppointmentRepository appointmentRepository,
            BiometricCollectionRepository biometricRepository,
            CitizenApplicationRepository applicationRepository,
            NotificationPublisher notificationPublisher
    ) {
        this.appointmentService = appointmentService;
        this.appointmentRepository = appointmentRepository;
        this.biometricRepository = biometricRepository;
        this.applicationRepository = applicationRepository;
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * CREATE: save the collection, mark the appointment COMPLETED and the application ready for
     * printing. Needs a prior check-in.
     */
    @Transactional
    public void completeCollection(String reference, String serviceType, Integer staffId) {
        if (staffId == null) throw new IllegalArgumentException("Officer session was not found.");
        OfficerBiometricDetails details = appointmentService.findForOfficer(reference, serviceType);
        if (!details.checkedIn()) {
            throw new IllegalArgumentException("Check in the citizen before collecting biometrics.");
        }
        if (details.biometricCollected()) {
            throw new IllegalArgumentException("Biometrics have already been collected.");
        }
        var lockedAppointment = appointmentRepository.findByIdForChange(details.appointment().getAppointmentId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment was not found."));
        if (!"CHECKED_IN".equals(lockedAppointment.getAppointmentStatus())
                || biometricRepository.existsById(details.application().getApplicationId())) {
            throw new IllegalArgumentException("Check in the citizen before collecting biometrics.");
        }

        BiometricCollection collection = new BiometricCollection();
        collection.setApplicationId(details.application().getApplicationId());
        collection.setStaffId(staffId);
        collection.setCollectedAt(LocalDateTime.now());
        collection.setCollectionStatus("COLLECTED");
        biometricRepository.save(collection);

        details.appointment().setAppointmentStatus("COMPLETED");
        appointmentRepository.save(details.appointment());
        details.application().setCurrentStatus("BIOMETRIC_COLLECTED");
        applicationRepository.save(details.application());

        notificationPublisher.sendToCitizen(
                details.application().getCitizenId(),
                staffId,
                "BIOMETRIC_COMPLETED",
                "Biometrics for " + details.application().getServiceType() + " application "
                        + details.application().getApplicationNo()
                        + " were collected successfully. Your application is ready for printing."
        );
    }
}
