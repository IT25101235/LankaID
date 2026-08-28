package lk.sliit.identityissuanceservice.Document_Renewal_And_Replacement_Mgmt.Dto;

import jakarta.validation.constraints.Size;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileForm;

/** Form data used for passport renewal and lost-passport replacement. */
public class PassportRenewalReplacementForm extends CitizenProfileForm {

    private String serviceLevel = "NORMAL";
    private String travelDocumentType = "ALL_COUNTRIES";

    @Size(max = 12)
    private String nicProofNo;

    @Size(max = 12)
    private String parentNicNo;

    public String getServiceLevel() { return serviceLevel; }
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
    public String getTravelDocumentType() { return travelDocumentType; }
    public void setTravelDocumentType(String travelDocumentType) { this.travelDocumentType = travelDocumentType; }
    public String getNicProofNo() { return nicProofNo; }
    public void setNicProofNo(String nicProofNo) { this.nicProofNo = nicProofNo; }
    public String getParentNicNo() { return parentNicNo; }
    public void setParentNicNo(String parentNicNo) { this.parentNicNo = parentNicNo; }
}
