package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "NICApplication", schema = "dbo")
public class NicApplication {

    @Id
    @Column(name = "application_id")
    private Integer applicationId;

    @Column(name = "last_nic_no", length = 12)
    private String lastNicNo;

    @Column(name = "last_issued_date")
    private LocalDateTime lastIssuedDate;

    @Column(name = "police_station", length = 100)
    private String policeStation;

    @Column(name = "police_report_date")
    private LocalDateTime policeReportDate;

    public Integer getApplicationId() { return applicationId; }
    public void setApplicationId(Integer applicationId) { this.applicationId = applicationId; }
    public String getLastNicNo() { return lastNicNo; }
    public void setLastNicNo(String lastNicNo) { this.lastNicNo = lastNicNo; }
    public LocalDateTime getLastIssuedDate() { return lastIssuedDate; }
    public void setLastIssuedDate(LocalDateTime lastIssuedDate) { this.lastIssuedDate = lastIssuedDate; }
    public String getPoliceStation() { return policeStation; }
    public void setPoliceStation(String policeStation) { this.policeStation = policeStation; }
    public LocalDateTime getPoliceReportDate() { return policeReportDate; }
    public void setPoliceReportDate(LocalDateTime policeReportDate) { this.policeReportDate = policeReportDate; }
}
