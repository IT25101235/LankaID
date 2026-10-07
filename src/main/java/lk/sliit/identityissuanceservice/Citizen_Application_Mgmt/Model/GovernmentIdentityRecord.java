package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GovernmentIdentityRecord", schema = "dbo")
public class GovernmentIdentityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "government_record_id")
    private Integer governmentRecordId;

    @Column(name = "nic_no", length = 12)
    private String nicNo;

    @Column(name = "passport_no", length = 20)
    private String passportNo;

    @Column(name = "registered_email", length = 100)
    private String registeredEmail;

    @Column(name = "registered_phone", length = 15)
    private String registeredPhone;

    @Column(name = "identity_status", length = 20)
    private String identityStatus;

    public Integer getGovernmentRecordId() { return governmentRecordId; }
    public void setGovernmentRecordId(Integer governmentRecordId) { this.governmentRecordId = governmentRecordId; }
    public String getNicNo() { return nicNo; }
    public void setNicNo(String nicNo) { this.nicNo = nicNo; }
    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }
    public String getRegisteredEmail() { return registeredEmail; }
    public void setRegisteredEmail(String registeredEmail) { this.registeredEmail = registeredEmail; }
    public String getRegisteredPhone() { return registeredPhone; }
    public void setRegisteredPhone(String registeredPhone) { this.registeredPhone = registeredPhone; }
    public String getIdentityStatus() { return identityStatus; }
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }
}
