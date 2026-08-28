package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentCheckInRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.BiometricCollectionRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Repository.UserNotificationRepository;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Model.Payment;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Repository.PaymentRepository;
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
    @Autowired StaffAccountRepository staffRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired AppointmentCheckInRepository checkInRepository;
    @Autowired BiometricCollectionRepository biometricRepository;
    @Autowired UserNotificationRepository userNotificationRepository;
    @Autowired CentreRepository centreRepository;

    @Test
    void nicAndPassportCompletePaymentAppointmentQrAndBiometricCollection() throws Exception {
        mockMvc.perform(get("/webjars/jsqr/1.4.0/dist/jsQR.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jsQR")));

        ensureServiceCentreExists();
        completeWorkflow("NIC", "NIC_OFFICER", new BigDecimal("2000.0000"));
        completeWorkflow("PASSPORT", "PASSPORT_OFFICER", new BigDecimal("10000.0000"));
    }

    private void completeWorkflow(String serviceType, String officerRole, BigDecimal expectedAmount) throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        UserAccount citizenUser = createUser(
                serviceType.toLowerCase() + "-bio-" + unique + "@example.com",
                "+947" + String.format("%08d", Math.abs(unique.hashCode() % 100_000_000))
        );
        CitizenAccount citizen = new CitizenAccount(citizenUser.getUserId());
        citizen.setGivenName(serviceType + " Citizen");
        citizen.setFamilyName("Perera");
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
                        .param("applicationId", application.getApplicationId().toString())
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

        mockMvc.perform(get("/citizen/appointments/biometric")
                        .param("applicationId", application.getApplicationId().toString())
                        .session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(serviceType + " application " + application.getApplicationNo())))
                .andExpect(content().string(not(containsString("null application null"))))
                .andExpect(content().string(containsString(LocalDate.now().plusDays(1).toString())))
                .andExpect(content().string(containsString(LocalDate.now().plusDays(14).toString())));

        LocalDate appointmentDate = LocalDate.now().plusDays(1);
        mockMvc.perform(post("/citizen/appointments/biometric")
                        .param("applicationId", application.getApplicationId().toString())
                        .param("centreId", appointment.getCentreId().toString())
                        .param("appointmentDate", appointmentDate.toString())
                        .param("appointmentTime", "10:00")
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
        assertTrue(userNotificationRepository
                .findAllById_UserIdOrderByDeliveredAtDesc(citizenUser.getUserId()).size() >= 4);

        mockMvc.perform(get("/citizen/notifications").session(citizenSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ready for printing")));
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
