package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;

/** Form data used only when a citizen applies for their first passport. */
public class NewPassportApplicationForm extends CitizenProfileForm {

    // Passport-only values entered or confirmed by the citizen.
    @NotBlank(message = "Service level is required.")
    @Pattern(regexp = "NORMAL", message = "Select Normal Service.")
    private String serviceLevel = "NORMAL";
    @NotBlank(message = "Travel document type is required.")
    @Pattern(regexp = "ALL_COUNTRIES", message = "Select the All Countries travel document.")
    private String travelDocumentType = "ALL_COUNTRIES";

    @Size(max = 12)
    @Pattern(regexp = "^$|[0-9]{9}[VvXx]|[0-9]{12}", message = "NIC number must contain 12 digits, or 9 digits followed by V or X.")
    private String nicProofNo;

    @Size(max = 12)
    @Pattern(regexp = "^$|[0-9]{9}[VvXx]|[0-9]{12}", message = "NIC number must contain 12 digits, or 9 digits followed by V or X.")
    private String parentNicNo;

    // Returns the selected service level.
    public String getServiceLevel() { return serviceLevel; }
    // Sets the selected service level.
    public void setServiceLevel(String serviceLevel) { this.serviceLevel = serviceLevel; }
    // Returns the selected travel-document type.
    public String getTravelDocumentType() { return travelDocumentType; }
    // Sets the selected travel-document type.
    public void setTravelDocumentType(String travelDocumentType) { this.travelDocumentType = travelDocumentType; }
    // Returns the citizen NIC supplied as proof.
    public String getNicProofNo() { return nicProofNo; }
    // Sets the citizen NIC supplied as proof.
    public void setNicProofNo(String nicProofNo) { this.nicProofNo = nicProofNo; }
    // Returns the parent NIC supplied as proof.
    public String getParentNicNo() { return parentNicNo; }
    // Sets the parent NIC supplied as proof.
    public void setParentNicNo(String parentNicNo) { this.parentNicNo = parentNicNo; }
}
