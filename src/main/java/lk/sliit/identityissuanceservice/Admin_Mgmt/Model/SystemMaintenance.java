package lk.sliit.identityissuanceservice.Admin_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "SystemMaintenance", schema = "dbo")
public class SystemMaintenance {

    @Id
    @Column(name = "maintenance_id")
    private Integer maintenanceId;

    @Column(name = "maintenance_active", nullable = false)
    private boolean maintenanceActive;

    @Column(name = "maintenance_message", nullable = false, length = 300)
    private String maintenanceMessage;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getMaintenanceId() {
        return maintenanceId;
    }

    public void setMaintenanceId(Integer maintenanceId) {
        this.maintenanceId = maintenanceId;
    }

    public boolean isMaintenanceActive() {
        return maintenanceActive;
    }

    public void setMaintenanceActive(boolean maintenanceActive) {
        this.maintenanceActive = maintenanceActive;
    }

    public String getMaintenanceMessage() {
        return maintenanceMessage;
    }

    public void setMaintenanceMessage(String maintenanceMessage) {
        this.maintenanceMessage = maintenanceMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
