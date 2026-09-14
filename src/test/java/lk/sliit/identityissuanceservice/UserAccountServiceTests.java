package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTests {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private CitizenAccountRepository citizenAccountRepository;

    @Mock private CitizenApplicationService applicationService;

    @Test
    void registrationHashesPasswordAndCreatesCitizenAccount() {
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setUserId(25);
            return account;
        });

        UserAccountService service = new UserAccountService(
                userAccountRepository,
                citizenAccountRepository,
                applicationService
        );

        service.registerCitizen(" Citizen@Example.com ", "0771234567", "password123");

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        UserAccount savedUser = userCaptor.getValue();

        assertEquals("citizen@example.com", savedUser.getEmail());
        assertEquals("+94771234567", savedUser.getPhone());
        assertNotEquals("password123", savedUser.getPasswordHash());
        assertTrue(new BCryptPasswordEncoder().matches("password123", savedUser.getPasswordHash()));

        ArgumentCaptor<CitizenAccount> citizenCaptor = ArgumentCaptor.forClass(CitizenAccount.class);
        verify(citizenAccountRepository).save(citizenCaptor.capture());
        assertEquals(25, citizenCaptor.getValue().getUserId());
        verify(applicationService).getEligibility(25);
    }

    @Test
    void authenticationChecksTheHashedPassword() {
        UserAccount account = new UserAccount();
        account.setUserId(25);
        account.setEmail("citizen@example.com");
        account.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));

        when(userAccountRepository.findByEmailIgnoreCaseAndAccountStatusAndEmailVerified(
                "citizen@example.com",
                "ACTIVE",
                "Y"
        )).thenReturn(Optional.of(account));
        when(citizenAccountRepository.existsById(25)).thenReturn(true);

        UserAccountService service = new UserAccountService(
                userAccountRepository,
                citizenAccountRepository,
                applicationService
        );

        assertTrue(service.authenticateCitizen(
                "Citizen@Example.com",
                "password123"
        ).isPresent());
        assertTrue(service.authenticateCitizen(
                "citizen@example.com",
                "wrongPassword"
        ).isEmpty());
    }

    @Test
    void verifiedRegistrationMarksEmailAsVerified() {
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            account.setUserId(26);
            return account;
        });

        UserAccountService service = new UserAccountService(
                userAccountRepository,
                citizenAccountRepository,
                applicationService
        );
        PreparedCitizenRegistration prepared = service.prepareCitizenRegistration(
                "verified@example.com",
                "0771234568",
                "password123"
        );

        service.registerVerifiedCitizen(prepared);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        assertEquals("Y", userCaptor.getValue().getEmailVerified());
    }
}
