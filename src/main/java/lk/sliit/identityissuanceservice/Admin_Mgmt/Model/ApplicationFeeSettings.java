package lk.sliit.identityissuanceservice.Admin_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ApplicationFeeSettings", schema = "dbo")
public class ApplicationFeeSettings {

    @Id
    @Column(name = "settings_id")
    private Integer settingsId;

    @Column(name = "nic_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal nicFee;

    @Column(name = "passport_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal passportFee;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getSettingsId() {
        return settingsId;
    }

    public void setSettingsId(Integer settingsId) {
        this.settingsId = settingsId;
    }

    public BigDecimal getNicFee() {
        return nicFee;
    }

    public void setNicFee(BigDecimal nicFee) {
        this.nicFee = nicFee;
    }

    public BigDecimal getPassportFee() {
        return passportFee;
    }

    public void setPassportFee(BigDecimal passportFee) {
        this.passportFee = passportFee;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
