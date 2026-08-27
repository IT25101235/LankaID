package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.AdminAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.AdminAccountRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.AdminAccountService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTests {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminAccountRepository adminAccountRepository;

    @Test
    void createsTheFixedAdminWithAHashedPassword() {
        when(userAccountRepository.findByEmailIgnoreCase("admin@gov.lk"))
                .thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setUserId(50);
            return user;
        });
        when(adminAccountRepository.findById(50)).thenReturn(Optional.empty());

        AdminAccountService service = new AdminAccountService(
                userAccountRepository,
                adminAccountRepository
        );

        service.createDefaultAdminAccount();

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository, atLeastOnce()).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("admin@gov.lk", savedUser.getEmail());
        assertEquals("ACTIVE", savedUser.getAccountStatus());
        assertEquals("Y", savedUser.getEmailVerified());
        assertTrue(new BCryptPasswordEncoder().matches("admin12345", savedUser.getPasswordHash()));

        ArgumentCaptor<AdminAccount> adminCaptor = ArgumentCaptor.forClass(AdminAccount.class);
        verify(adminAccountRepository).save(adminCaptor.capture());
        assertEquals(50, adminCaptor.getValue().getUserId());
        assertEquals("ADMIN-001", adminCaptor.getValue().getEmployeeNo());
        assertEquals("SYSTEM_ADMIN", adminCaptor.getValue().getStaffRole());
    }

    @Test
    void authenticatesOnlyAnActiveSystemAdmin() {
        UserAccount user = new UserAccount();
        user.setUserId(50);
        user.setEmail("admin@gov.lk");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("admin12345"));

        when(userAccountRepository.findByEmailIgnoreCaseAndAccountStatus(
                "admin@gov.lk",
                "ACTIVE"
        )).thenReturn(Optional.of(user));
        when(adminAccountRepository.existsByUserIdAndStaffRoleAndStaffStatus(
                50,
                "SYSTEM_ADMIN",
                "ACTIVE"
        )).thenReturn(true);

        AdminAccountService service = new AdminAccountService(
                userAccountRepository,
                adminAccountRepository
        );

        assertTrue(service.authenticateAdmin("ADMIN@gov.lk", "admin12345").isPresent());
        assertTrue(service.authenticateAdmin("admin@gov.lk", "wrongPassword").isEmpty());
    }
}
