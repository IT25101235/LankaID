package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminNotificationRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminRecipient;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.EmailDispatchResult;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.AdminNotificationService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Observer.NotificationPublisher;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.OfficialNotificationEmailService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNotificationServiceTests {

    private CitizenAccountRepository citizens;
    private StaffAccountRepository staff;
    private UserAccountRepository users;
    private CitizenApplicationRepository applications;
    private NotificationPublisher notifications;
    private OfficialNotificationEmailService email;
    private AdminNotificationService service;

    @BeforeEach
    void setUp() {
        citizens = mock(CitizenAccountRepository.class);
        staff = mock(StaffAccountRepository.class);
        users = mock(UserAccountRepository.class);
        applications = mock(CitizenApplicationRepository.class);
        notifications = mock(NotificationPublisher.class);
        email = mock(OfficialNotificationEmailService.class);
        service = new AdminNotificationService(citizens, staff, users, applications, notifications, email);
    }

    @Test
    void allCitizensSendsOnePersistedBroadcastOnlyToActiveCitizenAccounts() {
        when(citizens.findAll()).thenReturn(List.of(new CitizenAccount(10), new CitizenAccount(11)));
        when(users.findAll()).thenReturn(List.of(
                user(10, "active@example.com", "ACTIVE", "Y"),
                user(11, "suspended@example.com", "SUSPENDED", "Y")
        ));
        when(notifications.sendToUsers(anyList(), eq(99), eq("ADMIN_GENERAL"), eq("Planned work\n\nThe service will reopen at 10:00."), eq(true)))
                .thenReturn(1);

        int delivered = service.sendInApp(request("CITIZEN", "ALL", null), 99);

        assertEquals(1, delivered);
        verify(notifications).sendToUsers(
                List.of(10), 99, "ADMIN_GENERAL", "Planned work\n\nThe service will reopen at 10:00.", true);
    }

    @Test
    void filteredOfficerAudienceUsesRoleAndStatus() {
        StaffAccount csr = staff(20, "CSR-001", "CSR", "ACTIVE");
        StaffAccount nic = staff(21, "NIC-001", "NIC_OFFICER", "ACTIVE");
        when(staff.findAll()).thenReturn(List.of(csr, nic));
        when(users.findAll()).thenReturn(List.of(
                user(20, "csr@gov.lk", "ACTIVE", "Y"),
                user(21, "nic@gov.lk", "ACTIVE", "Y")
        ));
        AdminNotificationRequest request = request("OFFICER", "FILTERED", null);
        request.setOfficerRole("CSR");
        when(notifications.sendToUsers(anyList(), eq(99), eq("ADMIN_GENERAL"), org.mockito.ArgumentMatchers.anyString(), eq(true)))
                .thenReturn(1);

        service.sendInApp(request, 99);

        verify(notifications).sendToUsers(
                List.of(20), 99, "ADMIN_GENERAL", "Planned work\n\nThe service will reopen at 10:00.", true);
    }

    @Test
    void selectedOfficerCanBeAddressedByEmployeeNumber() {
        when(staff.findAll()).thenReturn(List.of(staff(20, "CSR-001", "CSR", "ACTIVE")));
        when(users.findAll()).thenReturn(List.of(user(20, "csr@gov.lk", "ACTIVE", "Y")));
        AdminNotificationRequest request = request("OFFICER", "SELECTED", "CSR-001");
        when(notifications.sendToUsers(anyList(), eq(99), eq("ADMIN_GENERAL"), org.mockito.ArgumentMatchers.anyString(), eq(false)))
                .thenReturn(1);

        service.sendInApp(request, 99);

        verify(notifications).sendToUsers(
                List.of(20), 99, "ADMIN_GENERAL", "Planned work\n\nThe service will reopen at 10:00.", false);
    }

    @Test
    void officialEmailSkipsUnverifiedAddresses() {
        when(citizens.findAll()).thenReturn(List.of(new CitizenAccount(10), new CitizenAccount(11)));
        when(users.findAll()).thenReturn(List.of(
                user(10, "verified@example.com", "ACTIVE", "Y"),
                user(11, "unverified@example.com", "ACTIVE", "N")
        ));
        when(email.sendOfficialEmail(anyList(), eq("Planned work"), eq("The service will reopen at 10:00.")))
                .thenReturn(new EmailDispatchResult(1, 0));

        service.sendEmail(request("CITIZEN", "ALL", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AdminRecipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(email).sendOfficialEmail(captor.capture(), eq("Planned work"), eq("The service will reopen at 10:00."));
        assertEquals(List.of(new AdminRecipient(10, "verified@example.com")), captor.getValue());
    }

    @Test
    void invalidSpecificRecipientStopsTheWholeDispatch() {
        when(citizens.findAll()).thenReturn(List.of(new CitizenAccount(10)));
        when(users.findAll()).thenReturn(List.of(user(10, "citizen@example.com", "ACTIVE", "Y")));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.sendInApp(request("CITIZEN", "SELECTED", "unknown@example.com"), 99));

        assertEquals("Unknown or ineligible recipient: unknown@example.com", error.getMessage());
    }

    private AdminNotificationRequest request(String type, String audience, String selected) {
        AdminNotificationRequest request = new AdminNotificationRequest();
        request.setRecipientType(type);
        request.setAudience(audience);
        request.setSelectedRecipients(selected);
        request.setTitle("Planned work");
        request.setMessage("The service will reopen at 10:00.");
        return request;
    }

    private UserAccount user(int id, String email, String status, String verified) {
        UserAccount user = new UserAccount();
        user.setUserId(id);
        user.setEmail(email);
        user.setAccountStatus(status);
        user.setEmailVerified(verified);
        return user;
    }

    private StaffAccount staff(int id, String employeeNo, String role, String status) {
        StaffAccount account = new StaffAccount();
        account.setUserId(id);
        account.setEmployeeNo(employeeNo);
        account.setStaffRole(role);
        account.setStaffStatus(status);
        return account;
    }
}
