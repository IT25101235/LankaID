package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.ApplicationFeeSettings;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.ApplicationFeeSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class ApplicationFeeSettingsService {

    public static final BigDecimal DEFAULT_NIC_FEE = new BigDecimal("2000.00");
    public static final BigDecimal DEFAULT_PASSPORT_FEE = new BigDecimal("10000.00");
    private static final BigDecimal MAXIMUM_FEE = new BigDecimal("9999999999.99");

    private final ApplicationFeeSettingsRepository repository;

    public ApplicationFeeSettingsService(ApplicationFeeSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ApplicationFeeSettings getSettings() {
        return repository.findById(1).orElseGet(this::defaultSettings);
    }

    @Transactional(readOnly = true)
    public BigDecimal getFee(String serviceType) {
        ApplicationFeeSettings settings = getSettings();
        return "NIC".equals(serviceType) ? settings.getNicFee() : settings.getPassportFee();
    }

    @Transactional
    public ApplicationFeeSettings updateFees(String nicFeeInput, String passportFeeInput) {
        BigDecimal nicFee = parseLkrAmount(nicFeeInput, "NIC service fee");
        BigDecimal passportFee = parseLkrAmount(passportFeeInput, "Passport service fee");

        ApplicationFeeSettings settings = repository.findById(1).orElseGet(this::defaultSettings);
        settings.setNicFee(nicFee);
        settings.setPassportFee(passportFee);
        settings.setUpdatedAt(LocalDateTime.now());
        return repository.save(settings);
    }

    private BigDecimal parseLkrAmount(String input, String label) {
        String value = input == null ? "" : input.trim();
        if (!value.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException(label + " must be a valid LKR amount with up to two decimal places.");
        }

        BigDecimal amount = new BigDecimal(value);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(label + " must be greater than zero.");
        }
        if (amount.compareTo(MAXIMUM_FEE) > 0) {
            throw new IllegalArgumentException(label + " is too large.");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private ApplicationFeeSettings defaultSettings() {
        ApplicationFeeSettings settings = new ApplicationFeeSettings();
        settings.setSettingsId(1);
        settings.setNicFee(DEFAULT_NIC_FEE);
        settings.setPassportFee(DEFAULT_PASSPORT_FEE);
        return settings;
    }
}
