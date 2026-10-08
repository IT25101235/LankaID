package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "SupportRequest", schema = "dbo")
/** Stores the citizen message, officer response and current support workflow state. */
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;

    @Column(name = "handled_by_staff_id")
    private Integer handledByStaffId;

    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "request_status", length = 20)
    private String requestStatus;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "response", length = 2000)
    private String response;

    @PrePersist
    private void applyDefaults() {
        // Defaults are also applied to requests created outside the web form workflow.
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (requestStatus == null) requestStatus = "OPEN";
    }

    public Integer getRequestId() { return requestId; }
    public boolean isDeletableContact() {
        // Complaints, answered requests and verification records must remain for follow-up and audit.
        return "CONTACT".equals(requestType) && "OPEN".equals(requestStatus)
                && handledByStaffId == null && respondedAt == null
                && (response == null || response.isBlank())
                && !"Existing document verification".equals(subject);
    }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public Integer getCitizenId() { return citizenId; }
    public void setCitizenId(Integer citizenId) { this.citizenId = citizenId; }
    public Integer getHandledByStaffId() { return handledByStaffId; }
    public void setHandledByStaffId(Integer handledByStaffId) { this.handledByStaffId = handledByStaffId; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}
