package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import java.time.LocalDate;

public class CitizenEligibilityDto {

    private final boolean hasNic;
    private final boolean hasPassport;
    private final boolean identityConflict;
    private final String nicNumber;
    private final String passportNumber;
    private final LocalDate nicIssuedDate;

    public CitizenEligibilityDto(
            boolean hasNic,
            boolean hasPassport,
            boolean identityConflict,
            String nicNumber,
            String passportNumber) {
        this(hasNic, hasPassport, identityConflict, nicNumber, passportNumber, null);
    }

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

    public boolean isHasNic() { return hasNic; }
    public boolean isHasPassport() { return hasPassport; }
    public boolean isIdentityConflict() { return identityConflict; }
    public String getNicNumber() { return nicNumber; }
    public String getPassportNumber() { return passportNumber; }
    public LocalDate getNicIssuedDate() { return nicIssuedDate; }
}
