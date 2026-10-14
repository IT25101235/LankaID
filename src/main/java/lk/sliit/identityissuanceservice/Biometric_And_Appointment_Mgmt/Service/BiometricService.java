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

/**
 * Service: finishes the biometric step for an application.
 * <p>
 * Design Pattern - Service Layer and Observer: the rules live here and the citizen is notified through
 * NotificationPublisher.<br>
 * Exception handling: a broken rule throws IllegalArgumentException, and @Transactional saves the
 * collection, the appointment and the application together or not at all.
 */
@Service
public class BiometricService {

    // OOP - Composition and Encapsulation: the services and repositories are private final fields handed
    // in through the constructor (Design Pattern - Dependency Injection), so this class is loosely coupled
    // and the objects are never created here.
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
        // Input validation: the citizen must be checked in first, and biometrics are collected only once.
        if (!details.checkedIn()) {
            throw new IllegalArgumentException("Check in the citizen before collecting biometrics.");
        }
        if (details.biometricCollected()) {
            throw new IllegalArgumentException("Biometrics have already been collected.");
        }
        var lockedAppointment = appointmentService.lockAppointment(details.appointment().getAppointmentId());
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
