package lk.sliit.identityissuanceservice;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.NewNicApplicationForm;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto.CitizenProfileMapper;
import lk.sliit.identityissuanceservice.User_Mgmt.Dto.CitizenRegistrationDto;
import lk.sliit.identityissuanceservice.User_Mgmt.Service.PasswordRules;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.CardPaymentValidation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class FormValidationTests {
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    @AfterAll
    static void closeValidator() { factory.close(); }

    @ParameterizedTest
    @ValueSource(strings = {"771234567", "0771234567", "+94771234567"})
    void registrationAcceptsWalkInMobileFormats(String phone) {
        CitizenRegistrationDto form = new CitizenRegistrationDto();
        form.setPhone(phone);
        assertTrue(validator.validateProperty(form, "phone").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"77123456", "7712345678", "07123abc67", "+94871234567"})
    void registrationRejectsInvalidMobileNumbers(String phone) {
        CitizenRegistrationDto form = new CitizenRegistrationDto();
        form.setPhone(phone);
        assertFalse(validator.validateProperty(form, "phone").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"password123", "PASSWORD1!", "Password!", "Password123", "Aa1!", "Pass word1!"})
    void backendRejectsWeakPasswords(String password) {
        assertThrows(IllegalArgumentException.class, () -> PasswordRules.validate(password));
    }

    @Test
    void passwordEnforcesBcryptByteLimit() {
        assertDoesNotThrow(() -> PasswordRules.validate("Password123!"));
        assertDoesNotThrow(() -> PasswordRules.validate("Aa1!" + "a".repeat(68)));
        assertThrows(IllegalArgumentException.class, () -> PasswordRules.validate("Aa1!" + "a".repeat(69)));
        assertThrows(IllegalArgumentException.class, () -> PasswordRules.validate("Aa1!" + "සි".repeat(15)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kamal Perera", "සිල්වා", "குமார்"})
    void namesAcceptLettersInLocalLanguages(String name) {
        NewNicApplicationForm form = new NewNicApplicationForm();
        form.setFamilyName(name);
        assertTrue(validator.validateProperty(form, "familyName").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kamal123", "Kamal@", "---", "   "})
    void namesRejectNumbersAndSymbols(String name) {
        NewNicApplicationForm form = new NewNicApplicationForm();
        form.setGivenName(name);
        assertFalse(validator.validateProperty(form, "givenName").isEmpty());
    }

    @Test
    void postalCodesKeepLeadingZerosAndRejectWrongLength() {
        NewNicApplicationForm form = new NewNicApplicationForm();
        form.setPermanentPostalCode("00100");
        assertTrue(validator.validateProperty(form, "permanentPostalCode").isEmpty());
        for (String code : new String[]{"100", "123456", "A0100", "12 34"}) {
            form.setPermanentPostalCode(code);
            assertFalse(validator.validateProperty(form, "permanentPostalCode").isEmpty());
        }
        form.setPostalSameAsPermanent(false);
        form.setPostalHouseNo("12/A");
        form.setPostalStreet("Main Street");
        form.setPostalCity("Colombo");
        form.setPostalPostalCode("ABC12");
        assertThrows(IllegalArgumentException.class, () -> CitizenProfileMapper.validateConditionalFields(form));
        form.setPostalPostalCode("00100");
        assertDoesNotThrow(() -> CitizenProfileMapper.validateConditionalFields(form));
    }

    @Test
    void nicApplicationRequiresTheApplicantToBeAtLeastFifteen() {
        NewNicApplicationForm form = new NewNicApplicationForm();
        form.setDateOfBirth(LocalDate.now().minusYears(15).plusDays(1));
        assertFalse(validator.validateProperty(form, "oldEnoughForNic").isEmpty());

        form.setDateOfBirth(LocalDate.now().minusYears(15));
        assertTrue(validator.validateProperty(form, "oldEnoughForNic").isEmpty());
    }

    @Test
    void cardValidationChecksLengthExpiryAndCvv() {
        CardPaymentForm form = validCard();
        assertTrue(CardPaymentValidation.errors(form).isEmpty());
        form.setCardNumber("4111 1111 1111 111");
        form.setExpiry("13/29");
        form.setCvv("1234");
        assertEquals(3, CardPaymentValidation.errors(form).size());
        form.setExpiry(YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MM/yy")));
        assertTrue(CardPaymentValidation.errors(form).get("expiry").contains("expired"));
        form.setCardholderName("Citizen 123");
        assertTrue(CardPaymentValidation.errors(form).containsKey("cardholderName"));
    }

    static CardPaymentForm validCard() {
        CardPaymentForm form = new CardPaymentForm();
        form.setCardholderName("Test Citizen");
        form.setCardNumber("4111 1111 1111 1111");
        form.setExpiry(YearMonth.now().format(DateTimeFormatter.ofPattern("MM/yy")));
        form.setCvv("123");
        return form;
    }
}
