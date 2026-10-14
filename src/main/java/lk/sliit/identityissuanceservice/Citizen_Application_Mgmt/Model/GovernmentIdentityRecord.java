package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "GovernmentIdentityRecord", schema = "dbo")
// Represents an official NIC or passport record used for identity matching.
public class GovernmentIdentityRecord {

    // Database-generated registry record ID.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "government_record_id")
    private Integer governmentRecordId;

    @Column(name = "nic_no", length = 12)
    private String nicNo;

    @Column(name = "nic_issued_date")
    private LocalDate nicIssuedDate;

    @Column(name = "passport_no", length = 20)
    private String passportNo;

    @Column(name = "registered_email", length = 100)
    private String registeredEmail;

    @Column(name = "registered_phone", length = 15)
    private String registeredPhone;

    @Column(name = "identity_status", length = 20)
    private String identityStatus;

    // Returns the registry record ID.
    public Integer getGovernmentRecordId() { return governmentRecordId; }
    // Sets the registry record ID.
    public void setGovernmentRecordId(Integer governmentRecordId) { this.governmentRecordId = governmentRecordId; }
    // Returns the NIC number.
    public String getNicNo() { return nicNo; }
    // Sets the NIC number.
    public void setNicNo(String nicNo) { this.nicNo = nicNo; }
    // Returns the NIC issue date.
    public LocalDate getNicIssuedDate() { return nicIssuedDate; }
    // Sets the NIC issue date.
    public void setNicIssuedDate(LocalDate nicIssuedDate) { this.nicIssuedDate = nicIssuedDate; }
    // Returns the passport number.
    public String getPassportNo() { return passportNo; }
    // Sets the passport number.
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }
    // Returns the email stored in the government registry.
    public String getRegisteredEmail() { return registeredEmail; }
    // Sets the registry email.
    public void setRegisteredEmail(String registeredEmail) { this.registeredEmail = registeredEmail; }
    // Returns the phone stored in the government registry.
    public String getRegisteredPhone() { return registeredPhone; }
    // Sets the registry phone.
    public void setRegisteredPhone(String registeredPhone) { this.registeredPhone = registeredPhone; }
    // Returns whether the identity record is active or cancelled.
    public String getIdentityStatus() { return identityStatus; }
    // Sets the identity record status.
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }
}
