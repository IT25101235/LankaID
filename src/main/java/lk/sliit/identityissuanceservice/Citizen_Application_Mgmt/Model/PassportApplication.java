package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PassportApplication", schema = "dbo")
public class PassportApplication {

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

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public String getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
    public String getTravelDocumentType() { return travelDocumentType; }
    public void setTravelDocumentType(String travelDocumentType) { this.travelDocumentType = travelDocumentType; }
    public String getNicProofNo() { return nicProofNo; }
    public void setNicProofNo(String nicProofNo) { this.nicProofNo = nicProofNo; }
    public String getParentNicNo() { return parentNicNo; }
    public void setParentNicNo(String parentNicNo) { this.parentNicNo = parentNicNo; }
}
