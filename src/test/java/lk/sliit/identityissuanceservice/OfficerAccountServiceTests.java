package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerDetailsDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerRegistrationDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.OfficerUpdateDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficerAccountServiceTests {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StaffAccountRepository staffAccountRepository;

    private OfficerAccountService officerAccountService;

    @BeforeEach
    void setUp() {
        officerAccountService = new OfficerAccountService(
                userAccountRepository,
                staffAccountRepository
        );
    }

    @Test
    void createOfficerSavesHashedUserAndStaffRole() {
        OfficerRegistrationDto request = new OfficerRegistrationDto();
        request.setEmail("NIC.Officer@GOV.LK");
        request.setPhone("+94771234567");
        request.setRole("nic-officer");
        request.setPassword("Password123!");

        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setUserId(80);
            return user;
        });
        when(staffAccountRepository.findByEmployeeNoStartingWith("EMP-NIC-"))
                .thenReturn(List.of(
                        staffAccount(10, "EMP-NIC-001", OfficerAccountService.NIC_OFFICER),
                        staffAccount(11, "EMP-NIC-003", OfficerAccountService.NIC_OFFICER)
                ));

        officerAccountService.createOfficer(request);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        ArgumentCaptor<StaffAccount> staffCaptor = ArgumentCaptor.forClass(StaffAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        verify(staffAccountRepository).save(staffCaptor.capture());

        assertEquals("nic.officer@gov.lk", userCaptor.getValue().getEmail());
        assertTrue(new BCryptPasswordEncoder().matches(
                "Password123!",
                userCaptor.getValue().getPasswordHash()
        ));
        assertEquals(80, staffCaptor.getValue().getUserId());
        assertEquals("EMP-NIC-004", staffCaptor.getValue().getEmployeeNo());
        assertEquals(OfficerAccountService.NIC_OFFICER, staffCaptor.getValue().getStaffRole());
    }

    @Test
    void createOfficerRejectsEmailAlreadyUsedByCitizenAccount() {
        OfficerRegistrationDto request = new OfficerRegistrationDto();
        request.setEmail("Citizen@Example.com");
        request.setPhone("+94771234567");
        request.setRole("nic-officer");
        request.setPassword("Password123!");
        when(userAccountRepository.existsByEmailIgnoreCase("citizen@example.com"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> officerAccountService.createOfficer(request)
        );

        assertEquals("That email address is already registered.", exception.getMessage());
        verify(userAccountRepository, never()).save(any(UserAccount.class));
        verify(staffAccountRepository, never()).save(any(StaffAccount.class));
    }

    @Test
    void directorCredentialsCanAuthenticateForCsrDashboard() {
        UserAccount user = activeUser(81, "director@gov.lk", "Password123!");
        StaffAccount director = staffAccount(81, "EMP-DIR-001", OfficerAccountService.DIRECTOR);

        when(userAccountRepository.findByEmailIgnoreCaseAndAccountStatus("director@gov.lk", "ACTIVE"))
                .thenReturn(Optional.of(user));
        when(staffAccountRepository.findById(81)).thenReturn(Optional.of(director));
        when(userAccountRepository.findById(81)).thenReturn(Optional.of(user));

        Optional<OfficerDetailsDto> result = officerAccountService.authenticateOfficer(
                "director@gov.lk",
                "Password123!",
                "csr"
        );

        assertTrue(result.isPresent());
        assertEquals(OfficerAccountService.DIRECTOR, result.get().getRoleCode());
    }

    @Test
    void nicOfficerCannotAuthenticateForPassportDashboard() {
        UserAccount user = activeUser(82, "nic@gov.lk", "Password123!");
        StaffAccount nicOfficer = staffAccount(82, "EMP-NIC-002", OfficerAccountService.NIC_OFFICER);

        when(userAccountRepository.findByEmailIgnoreCaseAndAccountStatus("nic@gov.lk", "ACTIVE"))
                .thenReturn(Optional.of(user));
        when(staffAccountRepository.findById(82)).thenReturn(Optional.of(nicOfficer));

        Optional<OfficerDetailsDto> result = officerAccountService.authenticateOfficer(
                "nic@gov.lk",
                "Password123!",
                "passport-officer"
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void updateOfficerChangesDetailsAndHashesNewPassword() {
        UserAccount user = activeUser(83, "old@gov.lk", "oldpassword");
        StaffAccount staff = staffAccount(83, "EMP-NIC-005", OfficerAccountService.NIC_OFFICER);

        OfficerUpdateDto request = new OfficerUpdateDto();
        request.setEmail("NEW.Officer@GOV.LK");
        request.setPhone("+94771112233");
        request.setRole("csr");
        request.setNewPassword("NewPassword123!");

        when(staffAccountRepository.findById(83)).thenReturn(Optional.of(staff));
        when(userAccountRepository.findById(83)).thenReturn(Optional.of(user));
        when(userAccountRepository.findByEmailIgnoreCase("new.officer@gov.lk"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.findByPhone("+94771112233"))
                .thenReturn(Optional.empty());
        when(staffAccountRepository.findByEmployeeNoStartingWith("EMP-CSR-"))
                .thenReturn(List.of(staffAccount(20, "EMP-CSR-002", OfficerAccountService.CSR)));

        officerAccountService.updateOfficer(83, request);

        assertEquals("new.officer@gov.lk", user.getEmail());
        assertEquals("+94771112233", user.getPhone());
        assertTrue(new BCryptPasswordEncoder().matches("NewPassword123!", user.getPasswordHash()));
        assertEquals(OfficerAccountService.CSR, staff.getStaffRole());
        assertEquals("EMP-CSR-003", staff.getEmployeeNo());
        verify(userAccountRepository).save(user);
        verify(staffAccountRepository).save(staff);
    }

    @Test
    void toggleOfficerStatusDisablesAndReactivatesBothAccounts() {
        UserAccount user = activeUser(84, "csr@gov.lk", "Password123!");
        StaffAccount staff = staffAccount(84, "EMP-CSR-004", OfficerAccountService.CSR);

        when(staffAccountRepository.findById(84)).thenReturn(Optional.of(staff));
        when(userAccountRepository.findById(84)).thenReturn(Optional.of(user));

        String disabledStatus = officerAccountService.toggleOfficerStatus(84);
        assertEquals("SUSPENDED", disabledStatus);
        assertEquals("SUSPENDED", user.getAccountStatus());
        assertEquals("SUSPENDED", staff.getStaffStatus());

        String activeStatus = officerAccountService.toggleOfficerStatus(84);
        assertEquals("ACTIVE", activeStatus);
        assertEquals("ACTIVE", user.getAccountStatus());
        assertEquals("ACTIVE", staff.getStaffStatus());
    }

    @Test
    void removeOfficerMarksBothAccountsAsRemoved() {
        UserAccount user = activeUser(85, "passport@gov.lk", "Password123!");
        StaffAccount staff = staffAccount(85, "EMP-PASS-003", OfficerAccountService.PASSPORT_OFFICER);

        when(staffAccountRepository.findById(85)).thenReturn(Optional.of(staff));
        when(userAccountRepository.findById(85)).thenReturn(Optional.of(user));

        officerAccountService.removeOfficer(85);

        assertEquals("REMOVED", user.getAccountStatus());
        assertEquals("REMOVED", staff.getStaffStatus());
        verify(userAccountRepository).save(user);
        verify(staffAccountRepository).save(staff);
    }

    private UserAccount activeUser(Integer userId, String email, String password) {
        UserAccount user = new UserAccount();
        user.setUserId(userId);
        user.setEmail(email);
        user.setPhone("+94770000000");
        user.setAccountStatus("ACTIVE");
        user.setPasswordHash(new BCryptPasswordEncoder().encode(password));
        return user;
    }

    private StaffAccount staffAccount(Integer userId, String employeeNo, String role) {
        StaffAccount staff = new StaffAccount();
        staff.setUserId(userId);
        staff.setEmployeeNo(employeeNo);
        staff.setStaffRole(role);
        staff.setStaffStatus("ACTIVE");
        return staff;
    }
}
