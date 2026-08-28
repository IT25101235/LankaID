package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class NicApplicationForm extends CitizenProfileForm {

    @Size(max = 12)
    private String lastNicNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "Last issued date cannot be in the future.")
    private LocalDate lastIssuedDate;

    @Size(max = 100)
    private String policeStation;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "Police report date cannot be in the future.")
    private LocalDate policeReportDate;

    public String getLastNicNo() { return lastNicNo; }
    public void setLastNicNo(String lastNicNo) { this.lastNicNo = lastNicNo; }
    public LocalDate getLastIssuedDate() { return lastIssuedDate; }
    public void setLastIssuedDate(LocalDate lastIssuedDate) { this.lastIssuedDate = lastIssuedDate; }
    public String getPoliceStation() { return policeStation; }
    public void setPoliceStation(String policeStation) { this.policeStation = policeStation; }
    public LocalDate getPoliceReportDate() { return policeReportDate; }
    public void setPoliceReportDate(LocalDate policeReportDate) { this.policeReportDate = policeReportDate; }
}
