package lk.sliit.identityissuanceservice.Payment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.NotificationService;
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

    public static final BigDecimal NIC_FEE = new BigDecimal("2000.00");
    public static final BigDecimal PASSPORT_FEE = new BigDecimal("10000.00");

    private final CitizenApplicationRepository applicationRepository;
    private final AppointmentRepository appointmentRepository;
    private final CentreRepository centreRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userRepository;
    private final NotificationService notificationService;

    public PaymentService(
            CitizenApplicationRepository applicationRepository,
            AppointmentRepository appointmentRepository,
            CentreRepository centreRepository,
            PaymentRepository paymentRepository,
            UserAccountRepository userRepository,
            NotificationService notificationService
    ) {
        this.applicationRepository = applicationRepository;
        this.appointmentRepository = appointmentRepository;
        this.centreRepository = centreRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        return new PaymentSummary(application, amountFor(application.getServiceType()), payment);
    }

    @Transactional
    public Payment payForApplication(Integer citizenId, Integer applicationId) {
        CitizenApplication application = findApprovedApplication(citizenId, applicationId);
        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(applicationId, "BIOMETRIC")
                .orElseGet(() -> createPendingAppointment(application));

        Payment existingPayment = paymentRepository
                .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                        appointment.getAppointmentId(), "PAID")
                .orElse(null);
        if (existingPayment != null) return existingPayment;

        Payment payment = new Payment();
        payment.setAppointmentId(appointment.getAppointmentId());
        payment.setTransactionReference(generateTransactionReference());
        payment.setAmount(amountFor(application.getServiceType()));
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        notificationService.sendToCitizen(
                citizenId,
                null,
                "PAYMENT_CONFIRMED",
                "Payment of LKR " + payment.getAmount().toPlainString() + " was received for "
                        + application.getServiceType() + " application " + application.getApplicationNo()
                        + ". You can now select your biometric appointment."
        );
        return payment;
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
        List<Centre> centres = centreRepository.findAllByOrderByCentreNameAsc();
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
                "APPROVED", "APPOINTMENT_BOOKED", "BIOMETRIC_COLLECTED", "PROCESSING"
        );
        if (!allowedStatuses.contains(application.getCurrentStatus())) {
            throw new IllegalArgumentException("Payment is available only after officer approval.");
        }
        return application;
    }

    private BigDecimal amountFor(String serviceType) {
        return "NIC".equals(serviceType) ? NIC_FEE : PASSPORT_FEE;
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
