package lk.sliit.identityissuanceservice.Payment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.PaymentSummary;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {

    private final CitizenApplicationRepository applicationRepository;
    private final AppointmentRepository appointmentRepository;
    private final CentreRepository centreRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    private final DocumentDeliveryService deliveryService;
    private final ApplicationFeeSettingsService feeSettingsService;

    public PaymentService(
            CitizenApplicationRepository applicationRepository,
            AppointmentRepository appointmentRepository,
            CentreRepository centreRepository,
            PaymentRepository paymentRepository,
            UserAccountRepository userRepository,
            NotificationPublisher notificationPublisher,
            DocumentDeliveryService deliveryService,
            ApplicationFeeSettingsService feeSettingsService
    ) {
        this.applicationRepository = applicationRepository;
        this.appointmentRepository = appointmentRepository;
        this.centreRepository = centreRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
        this.deliveryService = deliveryService;
        this.feeSettingsService = feeSettingsService;
    }

    @Transactional(readOnly = true)
    public PaymentSummary getPaymentSummary(Integer citizenId, Integer applicationId) {
        CitizenApplication application = findApprovedApplication(citizenId, applicationId);
        Payment payment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .flatMap(appointment -> paymentRepository
                        .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                                appointment.getAppointmentId(), "PAID"))
                .orElse(null);
        return new PaymentSummary(
                application,
                amountFor(application.getServiceType()),
                deliveryService.homeDeliveryFee(application.getServiceType()),
                payment,
                deliveryService.findByApplicationId(applicationId).orElse(null),
                deliveryService.getAvailableCentres(),
                deliveryService.getHomeAddress(citizenId)
        );
    }

    @Transactional
    public Payment payForApplication(
            Integer citizenId,
            Integer applicationId,
            String deliveryMethod,
            Integer centreId
    ) {
        CitizenApplication application = findApprovedApplication(citizenId, applicationId);
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseGet(() -> createPendingAppointment(application));

        Payment existingPayment = paymentRepository
                .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                        appointment.getAppointmentId(), "PAID")
                .orElse(null);
        DocumentDelivery existingDelivery = deliveryService.findByApplicationId(applicationId).orElse(null);
        if (existingPayment != null && existingDelivery != null) return existingPayment;

        DocumentDelivery delivery = deliveryService.chooseDelivery(
                citizenId,
                applicationId,
                deliveryMethod,
                centreId
        );

        if (existingPayment != null) {
            BigDecimal extraFee = deliveryService.deliveryFee(application.getServiceType(), deliveryMethod);
            if (extraFee.compareTo(BigDecimal.ZERO) > 0) {
                // Payment.appointment_id is unique in the existing database. For an older
                // paid application, include the delivery fee in that payment instead of
                // trying to insert a second payment row for the same appointment.
                existingPayment.setAmount(existingPayment.getAmount().add(extraFee));
                existingPayment.setPaidAt(LocalDateTime.now());
                existingPayment = paymentRepository.save(existingPayment);
                sendDeliveryFeeNotification(citizenId, application, extraFee);
            }
            if ("ISSUED".equals(application.getCurrentStatus())) {
                deliveryService.updateAfterPrinting(application, null);
            }
            return existingPayment;
        }

        BigDecimal total = amountFor(application.getServiceType())
                .add(deliveryService.deliveryFee(application.getServiceType(), delivery.getDeliveryMethod()));
        Payment payment = createPayment(appointment, total);
        sendPaymentNotification(citizenId, application, payment, false);
        return payment;
    }

    private Payment createPayment(Appointment appointment, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setAppointmentId(appointment.getAppointmentId());
        payment.setTransactionReference(generateTransactionReference());
        payment.setAmount(amount);
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    private void sendDeliveryFeeNotification(
            Integer citizenId,
            CitizenApplication application,
            BigDecimal deliveryFee
    ) {
        notificationPublisher.sendToCitizen(
                citizenId,
                null,
                "PAYMENT_CONFIRMED",
                "Home-delivery payment of LKR " + deliveryFee.toPlainString()
                        + " was received for " + application.getServiceType()
                        + " application " + application.getApplicationNo() + "."
        );
    }

    private void sendPaymentNotification(
            Integer citizenId,
            CitizenApplication application,
            Payment payment,
            boolean deliveryOnly
    ) {
        notificationPublisher.sendToCitizen(
                citizenId,
                null,
                "PAYMENT_CONFIRMED",
                "Payment of LKR " + payment.getAmount().toPlainString() + " was received for "
                        + (deliveryOnly ? "home delivery of " : "")
                        + application.getServiceType() + " application " + application.getApplicationNo()
                        + (deliveryOnly ? "." : ". You can now select your biometric appointment.")
        );
    }

    @Transactional(readOnly = true)
    public boolean hasPaid(Integer applicationId) {
        return appointmentRepository.findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .flatMap(appointment -> paymentRepository
                        .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                                appointment.getAppointmentId(), "PAID"))
                .isPresent();
    }

    private Appointment createPendingAppointment(CitizenApplication application) {
        Centre centre = getAvailableCentre();
        UserAccount user = userRepository.findById(application.getCitizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen account was not found."));
        String reference = generateAppointmentReference();

        Appointment appointment = new Appointment();
        appointment.setCitizenId(application.getCitizenId());
        appointment.setApplicationId(application.getApplicationId());
        appointment.setCentreId(centre.getCentreId());
        appointment.setAppointmentType("BIOMETRIC");
        appointment.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
        appointment.setAppointmentStatus("PENDING_PAYMENT");
        appointment.setReferenceNo(reference);
        appointment.setQrCode(reference);
        appointment.setVisitorPhone(user.getPhone());
        return appointmentRepository.save(appointment);
    }

    private Centre getAvailableCentre() {
        List<Centre> centres = centreRepository.findAllByCentreStatusOrderByCentreNameAsc("ACTIVE");
        if (centres.isEmpty()) {
            throw new IllegalArgumentException(
                    "No service centres are available. Ask the system administrator to add one first."
            );
        }
        return centres.get(0);
    }

    private CitizenApplication findApprovedApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = applicationRepository
                .findByApplicationIdAndCitizenId(applicationId, citizenId)
                .orElseThrow(() -> new IllegalArgumentException("Application was not found."));
        List<String> allowedStatuses = List.of(
                "APPROVED", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "PROCESSING", "ISSUED"
        );
        if (!allowedStatuses.contains(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Payment is available only after officer approval.");
        }
        return application;
    }

    private BigDecimal amountFor(String serviceType) {
        return feeSettingsService.getFee(serviceType);
    }

    private String generateAppointmentReference() {
        String reference;
        do {
            reference = "BIO-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (appointmentRepository.existsByReferenceNo(reference));
        return reference;
    }

    private String generateTransactionReference() {
        String reference;
        do {
            reference = "PAY-" + Year.now().getValue() + "-"
                    + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
        } while (paymentRepository.existsByTransactionReference(reference));
        return reference;
    }
}
