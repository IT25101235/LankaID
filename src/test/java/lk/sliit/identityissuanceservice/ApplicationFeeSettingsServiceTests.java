package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.ApplicationFeeSettings;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.ApplicationFeeSettingsRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.ApplicationFeeSettingsService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationFeeSettingsServiceTests {

    @Test
    void missingDatabaseRowUsesCurrentDefaultFees() {
        ApplicationFeeSettingsRepository repository = mock(ApplicationFeeSettingsRepository.class);
        when(repository.findById(1)).thenReturn(Optional.empty());
        ApplicationFeeSettingsService service = new ApplicationFeeSettingsService(repository);

        assertEquals(0, ApplicationFeeSettingsService.DEFAULT_NIC_FEE
                .compareTo(service.getFee("NIC")));
        assertEquals(0, ApplicationFeeSettingsService.DEFAULT_PASSPORT_FEE
                .compareTo(service.getFee("PASSPORT")));
    }

    @Test
    void validLkrAmountsAreSavedWithTwoDecimalPlaces() {
        ApplicationFeeSettingsRepository repository = mock(ApplicationFeeSettingsRepository.class);
        when(repository.findById(1)).thenReturn(Optional.empty());
        when(repository.save(any(ApplicationFeeSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ApplicationFeeSettingsService service = new ApplicationFeeSettingsService(repository);

        ApplicationFeeSettings saved = service.updateFees("2500", "12500.50");

        assertEquals(new BigDecimal("2500.00"), saved.getNicFee());
        assertEquals(new BigDecimal("12500.50"), saved.getPassportFee());
        verify(repository).save(saved);
    }

    @Test
    void zeroNegativeLettersAndExtraDecimalPlacesAreRejected() {
        ApplicationFeeSettingsRepository repository = mock(ApplicationFeeSettingsRepository.class);
        ApplicationFeeSettingsService service = new ApplicationFeeSettingsService(repository);

        assertThrows(IllegalArgumentException.class, () -> service.updateFees("0", "10000"));
        assertThrows(IllegalArgumentException.class, () -> service.updateFees("-1", "10000"));
        assertThrows(IllegalArgumentException.class, () -> service.updateFees("two thousand", "10000"));
        assertThrows(IllegalArgumentException.class, () -> service.updateFees("2000.001", "10000"));
        verify(repository, never()).save(any());
    }
}
