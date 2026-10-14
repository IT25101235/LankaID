package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import java.time.LocalDate;

// Carries the result of the citizen's NIC and passport eligibility check.
public class CitizenEligibilityDto {

    // Eligibility values are fixed after the DTO is created.
    private final boolean hasNic;
    private final boolean hasPassport;
    private final boolean identityConflict;
    private final String nicNumber;
    private final String passportNumber;
    private final LocalDate nicIssuedDate;

    // Creates an eligibility result when no NIC issue date is needed.
    public CitizenEligibilityDto(
            boolean hasNic,
            boolean hasPassport,
            boolean identityConflict,
            String nicNumber,
            String passportNumber) {
        this(hasNic, hasPassport, identityConflict, nicNumber, passportNumber, null);
    }

    // Creates a complete eligibility result including the NIC issue date.
    public CitizenEligibilityDto(
            boolean hasNic,
            boolean hasPassport,
            boolean identityConflict,
            String nicNumber,
            String passportNumber,
            LocalDate nicIssuedDate) {
        this.hasNic = hasNic;
        this.hasPassport = hasPassport;
        this.identityConflict = identityConflict;
        this.nicNumber = nicNumber;
        this.passportNumber = passportNumber;
        this.nicIssuedDate = nicIssuedDate;
    }

    // Returns whether the citizen already has an NIC.
    public boolean isHasNic() { return hasNic; }
    // Returns whether the citizen already has a passport.
    public boolean isHasPassport() { return hasPassport; }
    // Returns whether registry information needs manual verification.
    public boolean isIdentityConflict() { return identityConflict; }
    // Returns the matched NIC number.
    public String getNicNumber() { return nicNumber; }
    // Returns the matched passport number.
    public String getPassportNumber() { return passportNumber; }
    // Returns the matched NIC issue date.
    public LocalDate getNicIssuedDate() { return nicIssuedDate; }
}
