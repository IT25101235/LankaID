package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SupportRequestForm {

    @NotBlank(message = "Select Contact Us or Complaint.")
    @Pattern(
            regexp = "CONTACT|COMPLAINT",
            message = "Select a valid request type."
    )
    private String requestType;

    @NotBlank(message = "Enter a subject.")
    @Size(min = 5, max = 200, message = "Subject must contain between 5 and 200 characters.")
    private String subject;

    @NotBlank(message = "Enter your message.")
    @Size(min = 10, max = 2000, message = "Message must contain between 10 and 2000 characters.")
    private String message;

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
