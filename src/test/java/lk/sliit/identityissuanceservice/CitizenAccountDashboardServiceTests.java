package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountDashboardDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Dto.CitizenAccountRowDto;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.CitizenAccountDashboardService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Repository.CitizenApplicationRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.CitizenAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.CitizenAccountRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitizenAccountDashboardServiceTests {

    @Test
    void dashboardUsesCitizenUsersAndTheirRealApplicationCounts() {
        CitizenAccountRepository citizenRepository = mock(CitizenAccountRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        CitizenApplicationRepository applicationRepository = mock(CitizenApplicationRepository.class);
        CitizenAccountDashboardService service = new CitizenAccountDashboardService(
                citizenRepository, userRepository, applicationRepository);

        CitizenAccount activeCitizen = citizen(41, "Nimali", "Perera");
        CitizenAccount suspendedCitizen = citizen(42, null, null);
        UserAccount activeUser = user(41, "nimali@example.com", "ACTIVE", LocalDateTime.now());
        UserAccount suspendedUser = user(42, "waiting@example.com", "SUSPENDED", LocalDateTime.now().minusMonths(2));
        List<CitizenApplicationRepository.ApplicationCount> applicationCounts = List.of(
                count(41, "NIC", 2), count(41, "PASSPORT", 1), count(42, "NIC", 1));

        when(citizenRepository.findAll()).thenReturn(List.of(activeCitizen, suspendedCitizen));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(activeUser, suspendedUser));
        when(applicationRepository.countApplicationsByCitizenAndServiceType(anyCollection()))
                .thenReturn(applicationCounts);

        CitizenAccountDashboardDto dashboard = service.getDashboard();

        assertEquals(2, dashboard.citizenAccountCount());
        assertEquals(1, dashboard.registeredThisMonthCount());
        assertEquals(1, dashboard.suspendedCitizenCount());
        assertEquals(List.of(42, 41), dashboard.citizens().stream().map(CitizenAccountRowDto::userId).toList());

        CitizenAccountRowDto suspendedRow = dashboard.citizens().get(0);
        assertEquals("CTZ-000042", suspendedRow.accountId());
        assertEquals("Profile not completed", suspendedRow.displayName());
        assertEquals(1, suspendedRow.nicApplicationCount());
        assertEquals(0, suspendedRow.passportApplicationCount());
        assertFalse(suspendedRow.active());

        CitizenAccountRowDto activeRow = dashboard.citizens().get(1);
        assertEquals("Nimali Perera", activeRow.displayName());
        assertEquals(2, activeRow.nicApplicationCount());
        assertEquals(1, activeRow.passportApplicationCount());
        assertTrue(activeRow.active());
        assertTrue(activeRow.registeredThisMonth());
    }

    @Test
    void dashboardHandlesAnEmptyCitizenDatabase() {
        CitizenAccountRepository citizenRepository = mock(CitizenAccountRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        CitizenApplicationRepository applicationRepository = mock(CitizenApplicationRepository.class);
        when(citizenRepository.findAll()).thenReturn(List.of());
        when(userRepository.findAllById(List.of())).thenReturn(List.of());

        CitizenAccountDashboardDto dashboard = new CitizenAccountDashboardService(
                citizenRepository, userRepository, applicationRepository).getDashboard();

        assertTrue(dashboard.citizens().isEmpty());
        assertEquals(0, dashboard.citizenAccountCount());
    }

    private CitizenAccount citizen(int id, String givenName, String familyName) {
        CitizenAccount citizen = new CitizenAccount(id);
        citizen.setGivenName(givenName);
        citizen.setFamilyName(familyName);
        return citizen;
    }

    private UserAccount user(int id, String email, String status, LocalDateTime registeredAt) {
        UserAccount user = new UserAccount();
        user.setUserId(id);
        user.setEmail(email);
        user.setAccountStatus(status);
        user.setRegisteredAt(registeredAt);
        return user;
    }

    private CitizenApplicationRepository.ApplicationCount count(
            int citizenId,
            String serviceType,
            long applicationCount
    ) {
        CitizenApplicationRepository.ApplicationCount projection =
                mock(CitizenApplicationRepository.ApplicationCount.class);
        when(projection.getCitizenId()).thenReturn(citizenId);
        when(projection.getServiceType()).thenReturn(serviceType);
        when(projection.getApplicationCount()).thenReturn(applicationCount);
        return projection;
    }
}
