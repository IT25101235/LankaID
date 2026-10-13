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
        String cardNumber = normalizedCardNumber(form);
        if (!text(form.getCardNumber()).matches("[0-9]{16}|[0-9]{4}( [0-9]{4}){3}")) {
            errors.put("cardNumber", "Enter exactly 16 card digits, grouped as 1234 5678 9012 3456.");
        } else if (cardBrand(cardNumber) == null) {
            errors.put("cardNumber", "Enter a supported Visa or Mastercard number.");
        } else if (!passesLuhnCheck(cardNumber)) {
            errors.put("cardNumber", "Enter a valid card number.");
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

    public static void requireValid(CardPaymentForm form) {
        if (!errors(form).isEmpty()) {
            throw new IllegalArgumentException("Correct the highlighted card details before paying.");
        }
    }

    public static String normalizedCardNumber(CardPaymentForm form) {
        return text(form == null ? null : form.getCardNumber()).replace(" ", "");
    }

    public static String cardBrand(CardPaymentForm form) {
        return cardBrand(normalizedCardNumber(form));
    }

    public static String lastFour(CardPaymentForm form) {
        String number = normalizedCardNumber(form);
        return number.length() < 4 ? "" : number.substring(number.length() - 4);
    }

    private static String cardBrand(String number) {
        if (number.matches("4[0-9]{15}")) return "VISA";
        if (!number.matches("[0-9]{16}")) return null;
        int firstTwo = Integer.parseInt(number.substring(0, 2));
        int firstFour = Integer.parseInt(number.substring(0, 4));
        return (firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)
                ? "MASTERCARD" : null;
    }

    private static boolean passesLuhnCheck(String number) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int index = number.length() - 1; index >= 0; index--) {
            int digit = number.charAt(index) - '0';
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }

    private static String text(String value) { return value == null ? "" : value; }
}
