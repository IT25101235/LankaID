package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricAppointmentSlotRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricScheduleService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.GovernmentIdentityRecord;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.GovernmentIdentityRecordRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Model.DocumentDelivery;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Repository.DocumentDeliveryRepository;
import lk.sliit.identityissuanceservice.Document_Renewal_Replacement_And_Delivery_Mgmt.Service.DocumentDeliveryService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.UserNotificationRepository;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.PaymentService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PaymentBiometricWorkflowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userRepository;
    @Autowired CitizenAccountRepository citizenRepository;
    @Autowired CitizenApplicationRepository applicationRepository;
    @Autowired GovernmentIdentityRecordRepository governmentRecordRepository;
    @Autowired DocumentDeliveryRepository deliveryRepository;
    @Autowired StaffAccountRepository staffRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired AppointmentCheckInRepository checkInRepository;
    @Autowired BiometricCollectionRepository biometricRepository;
    @Autowired UserNotificationRepository userNotificationRepository;
    @Autowired CentreRepository centreRepository;
    @Autowired BiometricAppointmentSlotRepository biometricSlotRepository;
    @Autowired BiometricScheduleService biometricScheduleService;
    @Autowired ApplicationFeeSettingsService feeSettingsService;
    @Autowired PaymentService paymentService;

    @Test
    void configuredApplicationFeeIsUsedByThePaymentSummary() {
        feeSettingsService.updateFees("2750.50", "12500.00");
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = createUser("configured-fee-" + unique + "@example.com", "+94710000002");

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Fee");
        citizen.setFamilyName("Test");
        citizen.setPermanentHouseNo("10");
        citizen.setPermanentStreet("Main Street");
        citizen.setPermanentCity("Colombo");
        citizen.setPermanentPostalCode("00100");
        citizenRepository.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-NIC-FEE-" + unique);
        application.setCitizenId(user.getUserId());
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("APPROVED");
        application.setDeclarationAccepted("Y");
        application = applicationRepository.saveAndFlush(application);

        assertEquals(
                0,
                new BigDecimal("2750.50").compareTo(
                        paymentService.getPaymentSummary(
                                user.getUserId(), application.getApplicationId()).amount())
        );
    }

    @Test
    void nicAndPassportCompletePaymentAppointmentQrAndBiometricCollection() throws Exception {
        mockMvc.perform(get("/webjars/jsqr/1.4.0/dist/jsQR.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jsQR")));

        ensureServiceCentreExists();
        completeWorkflow(
                "NIC", "NIC_OFFICER", DocumentDeliveryService.HOME_DELIVERY,
                new BigDecimal("2500.0000")
        );
        completeWorkflow(
                "PASSPORT", "PASSPORT_OFFICER", DocumentDeliveryService.CENTRE_COLLECTION,
                new BigDecimal("10000.0000")
        );
    }

    @Test
    void alreadyPaidApplicationAddsHomeDeliveryWithoutCreatingSecondPayment() throws Exception {
        ensureServiceCentreExists();
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount user = createUser("legacy-payment-" + unique + "@example.com", "+94710000001");

        CitizenAccount citizen = new CitizenAccount(user.getUserId());
        citizen.setGivenName("Legacy");
        citizen.setFamilyName("Citizen");
        citizen.setPermanentHouseNo("10");
        citizen.setPermanentStreet("Main Street");
        citizen.setPermanentCity("Colombo");
        citizen.setPermanentPostalCode("00100");
        citizenRepository.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-NIC-LEGACY-" + unique);
        application.setCitizenId(user.getUserId());
        application.setServiceType("NIC");
        application.setApplicationKind("NEW");
        application.setCurrentStatus("ISSUED");
        application.setDeclarationAccepted("Y");
        application = applicationRepository.saveAndFlush(application);

        Appointment appointment = new Appointment();
        appointment.setCitizenId(user.getUserId());
        appointment.setApplicationId(application.getApplicationId());
        appointment.setCentreId(centreRepository.findAllByOrderByCentreNameAsc().get(0).getCentreId());
        appointment.setAppointmentType("BIOMETRIC");
        appointment.setScheduledAt(LocalDateTime.now());
        appointment.setAppointmentStatus("COMPLETED");
        appointment.setReferenceNo("BIO-LEGACY-" + unique);
        appointment.setQrCode(appointment.getReferenceNo());
        appointment.setVisitorPhone(user.getPhone());
        appointment = appointmentRepository.saveAndFlush(appointment);

        Payment payment = new Payment();
        payment.setAppointmentId(appointment.getAppointmentId());
        payment.setTransactionReference("PAY-LEGACY-" + unique);
        payment.setAmount(new BigDecimal("2000.00"));
        payment.setPaymentStatus("PAID");
        payment.setPaidAt(LocalDateTime.now().minusDays(1));
        payment = paymentRepository.saveAndFlush(payment);
        Integer appointmentId = appointment.getAppointmentId();

        mockMvc.perform(post("/citizen/payment")
                        .param("cardholderName", "Test Citizen")
                        .param("cardNumber", "4111 1111 1111 1111")
                        .param("expiry", java.time.YearMonth.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("MM/yy")))
                        .param("cvv", "123")
                        .param("applicationId", application.getApplicationId().toString())
                        .param("deliveryMethod", DocumentDeliveryService.HOME_DELIVERY)
                        .session(citizenSession(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/citizen/applications"));

        Payment updatedPayment = paymentRepository.findById(payment.getPaymentId()).orElseThrow();
        assertEquals(0, new BigDecimal("2500.00").compareTo(updatedPayment.getAmount()));
        assertEquals(
                1,
                paymentRepository.findAll().stream()
                        .filter(item -> appointmentId.equals(item.getAppointmentId()))
                        .count()
        );
        DocumentDelivery delivery = deliveryRepository.findByApplicationId(application.getApplicationId())
                .orElseThrow();
        assertEquals(DocumentDeliveryService.HOME_DELIVERY, delivery.getDeliveryMethod());
        assertEquals(DocumentDeliveryService.DISPATCHED, delivery.getDeliveryStatus());
    }

    private void completeWorkflow(
            String serviceType,
            String officerRole,
            String deliveryMethod,
            BigDecimal expectedAmount
    ) throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount citizenUser = createUser(
                serviceType.toLowerCase() + "-bio-" + unique + "@example.com",
                "+947" + String.format("%08d", Math.abs(unique.hashCode() % 100_000_000))
        );
        CitizenAccount citizen = new CitizenAccount(citizenUser.getUserId());
        citizen.setGivenName(serviceType + " Citizen");
        citizen.setFamilyName("Perera");
        citizen.setGender("MALE");
        citizen.setDateOfBirth(LocalDateTime.of(2000, 1, 15, 0, 0));
        citizen.setPermanentHouseNo("25");
        citizen.setPermanentStreet("Galle Road");
        citizen.setPermanentCity("Colombo");
        citizen.setPermanentPostalCode("00300");
        citizenRepository.saveAndFlush(citizen);

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("LK-" + serviceType + "-BIO-" + unique);
        application.setCitizenId(citizenUser.getUserId());
        application.setServiceType(serviceType);
        application.setApplicationKind("NEW");
        application.setCurrentStatus("APPROVED");
        application.setDeclarationAccepted("Y");
        application = applicationRepository.saveAndFlush(application);

        UserAccount officerUser = createUser(
                serviceType.toLowerCase() + "-collector-" + unique + "@gov.lk",
                "+946" + String.format("%08d", Math.abs((unique + "staff").hashCode() % 100_000_000))
        );
        StaffAccount officer = new StaffAccount();
        officer.setUserId(officerUser.getUserId());
        officer.setEmployeeNo(serviceType.substring(0, 3) + "-BIO-" + unique);
        officer.setStaffRole(officerRole);
        officer.setStaffStatus("ACTIVE");
        staffRepository.saveAndFlush(officer);

        MockHttpSession citizenSession = citizenSession(citizenUser);
        mockMvc.perform(get("/citizen/payment")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(application.getApplicationNo())))
                .andExpect(content().string(containsString("LKR")));

        mockMvc.perform(post("/citizen/payment")
                        .param("cardholderName", "Test Citizen")
                        .param("cardNumber", "4111 1111 1111 1111")
                        .param("expiry", java.time.YearMonth.now().plusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("MM/yy")))
                        .param("cvv", "123")
                        .param("applicationId", application.getApplicationId().toString())
                        .param("deliveryMethod", deliveryMethod)
                        .param("centreId", centreRepository.findAllByOrderByCentreNameAsc()
                                .get(0).getCentreId().toString())
                        .session(citizenSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/citizen/appointments/biometric?applicationId=" + application.getApplicationId()));

        Appointment appointment = appointmentRepository
                .findByApplicationIdAndAppointmentType(application.getApplicationId(), "BIOMETRIC")
                .orElseThrow();
        Payment payment = paymentRepository
                .findFirstByAppointmentIdAndPaymentStatusOrderByCreatedAtDesc(
                        appointment.getAppointmentId(), "PAID")
                .orElseThrow();
        assertEquals(0, expectedAmount.compareTo(payment.getAmount()));
        assertEquals("PENDING_PAYMENT", appointment.getAppointmentStatus());
        DocumentDelivery delivery = deliveryRepository.findByApplicationId(application.getApplicationId())
                .orElseThrow();
        assertEquals(deliveryMethod, delivery.getDeliveryMethod());
        assertEquals("PREPARING", delivery.getDeliveryStatus());

        mockMvc.perform(get("/citizen/appointments/biometric")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(serviceType + " application " + application.getApplicationNo())))
                .andExpect(content().string(not(containsString("null application null"))))
                .andExpect(content().string(containsString(LocalDate.now().plusDays(1).toString())))
                .andExpect(content().string(containsString(LocalDate.now().plusDays(90).toString())));

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        // Each workflow gets its own centre so it does not compete for the same half-hour slot.
        Centre biometricCentre = new Centre();
        biometricCentre.setCentreName("Biometric workflow " + unique);
        biometricCentre.setCity("Colombo");
        biometricCentre.setDistrict("Colombo");
        biometricCentre.setDailyCapacity(100);
        centreRepository.saveAndFlush(biometricCentre);
        biometricScheduleService.publishDateRange(serviceType, biometricCentre.getCentreId(),
                appointmentDate, appointmentDate, java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(16, 30), 1, officer.getUserId());
        var selectedSlot = biometricSlotRepository
                .findByCentreIdAndServiceTypeAndSlotDateAndSlotTime(
                        biometricCentre.getCentreId(), serviceType, appointmentDate, java.time.LocalTime.of(10, 0))
                .orElseThrow();
        mockMvc.perform(post("/citizen/appointments/biometric")
                        .param("applicationId", application.getApplicationId().toString())
                        .param("slotId", selectedSlot.getSlotId().toString())
                        .session(citizenSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/citizen/appointments/" + appointment.getReferenceNo() + "/pass"));

        appointment = appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow();
        assertEquals("CONFIRMED", appointment.getAppointmentStatus());
        assertEquals(appointment.getReferenceNo(), appointment.getQrCode());
        assertEquals(
                "APPOINTMENT_BOOKED",
                applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus()
        );

        mockMvc.perform(get("/citizen/appointments/{reference}/pass", appointment.getReferenceNo())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(appointment.getReferenceNo())))
                .andExpect(content().string(containsString("Download PNG pass")));
        mockMvc.perform(get("/citizen/appointments/{reference}/qr", appointment.getReferenceNo())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("image/png")));
        mockMvc.perform(get("/citizen/appointments/{reference}/pass/download", appointment.getReferenceNo())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Type", containsString("image/png")));

        MockHttpSession officerSession = officerSession(officer, officerUser, officerRole);
        String servicePath = serviceType.toLowerCase();
        mockMvc.perform(get("/officer/{service}/biometrics", servicePath)
                        .param("reference", appointment.getReferenceNo())
                        .param("checkInMethod", "QR")
                        .session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(serviceType + " Citizen Perera")))
                .andExpect(content().string(containsString("Check in citizen")))
                .andExpect(content().string(containsString("/webjars/jsqr/1.4.0/dist/jsQR.js")))
                .andExpect(content().string(containsString("scanWebcamFrame")))
                .andExpect(content().string(not(containsString("BarcodeDetector"))));

        mockMvc.perform(get("/officer/{service}/applications", servicePath)
                        .session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Biometric Collection")))
                .andExpect(content().string(not(containsString("Collection Workbench"))));

        mockMvc.perform(post("/officer/{service}/biometrics/{reference}/check-in",
                                servicePath, appointment.getReferenceNo())
                        .param("checkInMethod", "QR")
                        .session(officerSession))
                .andExpect(status().is3xxRedirection());
        assertTrue(checkInRepository.existsById(appointment.getAppointmentId()));

        mockMvc.perform(post("/officer/{service}/biometrics/{reference}/complete",
                                servicePath, appointment.getReferenceNo())
                        .session(officerSession))
                .andExpect(status().is3xxRedirection());

        assertTrue(biometricRepository.existsById(application.getApplicationId()));
        assertEquals(
                "BIOMETRIC_COLLECTED",
                applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus()
        );
        assertEquals(
                "COMPLETED",
                appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow().getAppointmentStatus()
        );
        mockMvc.perform(get("/officer/{service}/biometrics", servicePath).session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(appointment.getReferenceNo()))));
        mockMvc.perform(get("/officer/{service}/appointments", servicePath).session(officerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(appointment.getReferenceNo())));
        assertTrue(userNotificationRepository
                .findAllById_UserIdOrderByDeliveredAtDesc(citizenUser.getUserId()).size() >= 4);

        mockMvc.perform(get("/citizen/notifications").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ready for printing")));

        MockHttpSession directorSession = officerSession(officer, officerUser, "DIRECTOR");
        mockMvc.perform(post("/officer/director/printing/process")
                        .param("applicationIds", application.getApplicationId().toString())
                        .session(directorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/director/printing"));

        assertEquals(
                "ISSUED",
                applicationRepository.findById(application.getApplicationId()).orElseThrow().getCurrentStatus()
        );
        CitizenAccount printedCitizen = citizenRepository.findById(citizenUser.getUserId()).orElseThrow();
        GovernmentIdentityRecord identityRecord = governmentRecordRepository
                .findById(printedCitizen.getGovernmentRecordId()).orElseThrow();
        if ("NIC".equals(serviceType)) {
            assertTrue(identityRecord.getNicNo().matches("2000\\d{8}"));
        } else {
            assertTrue(identityRecord.getPassportNo().matches("N\\d{7}"));
        }

        delivery = deliveryRepository.findByApplicationId(application.getApplicationId()).orElseThrow();
        if (DocumentDeliveryService.HOME_DELIVERY.equals(deliveryMethod)) {
            assertEquals("DISPATCHED", delivery.getDeliveryStatus());
            mockMvc.perform(post("/citizen/deliveries/confirm")
                            .param("applicationId", application.getApplicationId().toString())
                            .session(citizenSession))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/citizen/applications"));
            delivery = deliveryRepository.findByApplicationId(application.getApplicationId()).orElseThrow();
            assertEquals("DELIVERED", delivery.getDeliveryStatus());
            assertTrue(delivery.getCompletedAt() != null);
        } else {
            assertEquals("READY_FOR_COLLECTION", delivery.getDeliveryStatus());
            MockHttpSession csrSession = officerSession(officer, officerUser, "CSR");
            mockMvc.perform(get("/officer/csr/deliveries")
                            .param("reference", delivery.getTrackingReference())
                            .session(csrSession))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(delivery.getTrackingReference())))
                    .andExpect(content().string(containsString("Confirm document handover")));

            mockMvc.perform(post("/officer/csr/deliveries/handover")
                            .param("reference", delivery.getTrackingReference())
                            .session(csrSession))
                    .andExpect(status().is3xxRedirection());
            assertEquals(
                    "COLLECTED",
                    deliveryRepository.findByApplicationId(application.getApplicationId())
                            .orElseThrow().getDeliveryStatus()
            );
        }
    }

    private UserAccount createUser(String email, String phone) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash("test-hash");
        return userRepository.saveAndFlush(user);
    }

    private void ensureServiceCentreExists() {
        if (centreRepository.count() > 0) return;

        Centre centre = new Centre();
        centre.setCentreName("Integration Test Centre");
        centre.setStreet("Test Street");
        centre.setCity("Colombo");
        centre.setDistrict("Colombo");
        centre.setDailyCapacity(100);
        centreRepository.saveAndFlush(centre);
    }

    private MockHttpSession citizenSession(UserAccount citizen) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUserId", citizen.getUserId());
        session.setAttribute("loggedInUserEmail", citizen.getEmail());
        session.setAttribute("loggedInUserRole", "CITIZEN");
        return session;
    }

    private MockHttpSession officerSession(
            StaffAccount officer,
            UserAccount user,
            String role
    ) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", officer.getUserId());
        session.setAttribute("loggedInOfficerRole", role);
        session.setAttribute("loggedInOfficerRoleLabel", role);
        session.setAttribute("loggedInOfficerEmployeeNo", officer.getEmployeeNo());
        session.setAttribute("loggedInOfficerEmail", user.getEmail());
        return session;
    }
}
