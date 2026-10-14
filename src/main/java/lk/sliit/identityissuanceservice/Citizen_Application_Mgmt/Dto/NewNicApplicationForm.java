package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

/** Form data used only when a citizen applies for their first NIC. */
public class NewNicApplicationForm extends CitizenProfileForm {

    // Returns true when the date of birth meets the NIC minimum-age rule.
    @AssertTrue(message = "You must be at least 15 years old to apply for an NIC.")
    public boolean isOldEnoughForNic() {
        LocalDate dateOfBirth = getDateOfBirth();
        return dateOfBirth == null || !dateOfBirth.isAfter(LocalDate.now().minusYears(15));
    }
}
