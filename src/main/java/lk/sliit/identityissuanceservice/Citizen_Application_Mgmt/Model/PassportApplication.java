package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PassportApplication", schema = "dbo")
// Stores passport-specific details attached to a common application record.
public class PassportApplication {

    // Uses the common application ID as its primary key.
    @Id
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "service_level", length = 20)
    private String serviceLevel;

    @Column(name = "travel_document_type", length = 30)
    private String travelDocumentType;

    @Column(name = "nic_proof_no", length = 12)
    private String nicProofNo;

    @Column(name = "parent_nic_no", length = 12)
    private String parentNicNo;

    // Returns the common application ID.
    public Integer getApplicationId() { return applicationId; }
    // Sets the common application ID.
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    // Returns the requested service level.
    public String getServiceLevel() { return serviceLevel; }
    // Sets the requested service level.
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
    // Returns the requested travel-document type.
    public String getTravelDocumentType() { return travelDocumentType; }
    // Sets the requested travel-document type.
    public void setTravelDocumentType(String travelDocumentType) { this.travelDocumentType = travelDocumentType; }
    // Returns the citizen NIC used as passport proof.
    public String getNicProofNo() { return nicProofNo; }
    // Sets the citizen NIC used as passport proof.
    public void setNicProofNo(String nicProofNo) { this.nicProofNo = nicProofNo; }
    // Returns the parent NIC used when the citizen has no NIC.
    public String getParentNicNo() { return parentNicNo; }
    // Sets the parent NIC used when the citizen has no NIC.
    public void setParentNicNo(String parentNicNo) { this.parentNicNo = parentNicNo; }
}
