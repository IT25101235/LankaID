package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.SystemMaintenance;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.SystemMaintenanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SystemMaintenanceService {

    public static final String DEFAULT_MESSAGE =
            "LankaID is temporarily unavailable for scheduled system maintenance. Please try again later.";

    private final SystemMaintenanceRepository repository;

    public SystemMaintenanceService(SystemMaintenanceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SystemMaintenance getSettings() {
        return repository.findById(1).orElseGet(this::defaultSettings);
    }

    @Transactional(readOnly = true)
    public boolean isMaintenanceActive() {
        return getSettings().isMaintenanceActive();
    }

    @Transactional
    public void updateMaintenance(boolean active, String message) {
        SystemMaintenance settings = repository.findById(1).orElseGet(this::defaultSettings);
        String cleanMessage = message == null ? "" : message.trim();

        if (active && cleanMessage.isBlank()) {
            cleanMessage = DEFAULT_MESSAGE;
        }
        if (cleanMessage.length() > 300) {
            throw new IllegalArgumentException("The maintenance message must be 300 characters or fewer.");
        }

        settings.setMaintenanceActive(active);
        settings.setMaintenanceMessage(cleanMessage.isBlank() ? DEFAULT_MESSAGE : cleanMessage);
        settings.setUpdatedAt(LocalDateTime.now());
        repository.save(settings);
    }

    private SystemMaintenance defaultSettings() {
        SystemMaintenance settings = new SystemMaintenance();
        settings.setMaintenanceId(1);
        settings.setMaintenanceActive(false);
        settings.setMaintenanceMessage(DEFAULT_MESSAGE);
        return settings;
    }
}
