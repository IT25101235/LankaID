package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/** Form data used only when a citizen applies for their first passport. */
public class NewPassportApplicationForm extends CitizenProfileForm {

    @Pattern(regexp = "NORMAL", message = "Select Normal Service.")
    private String serviceLevel = "NORMAL";
    @Pattern(regexp = "ALL_COUNTRIES", message = "Select the All Countries travel document.")
    private String travelDocumentType = "ALL_COUNTRIES";

    @Size(max = 12)
    @Pattern(regexp = "^$|[0-9]{9}[VvXx]|[0-9]{12}", message = "NIC number must contain 12 digits, or 9 digits followed by V or X.")
    private String nicProofNo;

    @Size(max = 12)
    @Pattern(regexp = "^$|[0-9]{9}[VvXx]|[0-9]{12}", message = "NIC number must contain 12 digits, or 9 digits followed by V or X.")
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
