package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Service.CitizenApplicationService;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.PreparedCitizenRegistration;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
class CitizenRegistrationTransactionIntegrationTests {

    @Autowired private UserAccountService accounts;
    @Autowired private UserAccountRepository users;
    @Autowired private CitizenAccountRepository citizens;

    @MockitoBean private CitizenApplicationService applications;

    @Test
    void identityLinkFailureRollsBackBothAccountRows() {
        String email = "rollback-" + UUID.randomUUID() + "@example.com";
        String phone = "07%08d".formatted(Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
        PreparedCitizenRegistration registration = accounts.prepareCitizenRegistration(
                email, phone, "Rollback-Test1!"
        );
        AtomicReference<Integer> createdUserId = new AtomicReference<>();
        doAnswer(invocation -> {
            createdUserId.set(invocation.getArgument(0));
            throw new IllegalStateException("Simulated identity-link failure");
        })
                .when(applications).getEligibility(anyInt());

        assertThrows(IllegalStateException.class, () -> accounts.registerVerifiedCitizen(registration));

        assertFalse(users.existsByEmailIgnoreCase(registration.email()));
        assertFalse(users.existsByPhone(registration.phone()));
        assertFalse(citizens.existsById(createdUserId.get()));
    }
}
