package lk.sliit.identityissuanceservice.Payment_Mgmt.Service;

import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/** Validate the simulation form before recording a payment. */
public final class CardPaymentValidation {
    private CardPaymentValidation() { }

    public static Map<String, String> errors(CardPaymentForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = text(form.getCardholderName()).trim();
        if (name.length() > 150 || !name.matches("[\\p{L}][\\p{L}\\p{M} ]*")) {
            errors.put("cardholderName", "Enter the cardholder name using letters and spaces only (maximum 150 characters).");
        }
        if (!text(form.getCardNumber()).matches("[0-9]{16}|[0-9]{4}( [0-9]{4}){3}")) {
            errors.put("cardNumber", "Enter exactly 16 card digits, grouped as 1234 5678 9012 3456.");
        }
        String expiry = text(form.getExpiry());
        if (!expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            errors.put("expiry", "Enter a valid expiry in MM/YY format, for example 09/29.");
        } else {
            YearMonth expiryMonth = YearMonth.of(2000 + Integer.parseInt(expiry.substring(3)),
                    Integer.parseInt(expiry.substring(0, 2)));
            if (expiryMonth.isBefore(YearMonth.now())) {
                errors.put("expiry", "This card has expired. Enter the expiry of a current card.");
            }
        }
        if (!text(form.getCvv()).matches("[0-9]{3}")) {
            errors.put("cvv", "Enter exactly 3 digits for the CVV.");
        }
        return errors;
    }

    private static String text(String value) { return value == null ? "" : value; }
}
