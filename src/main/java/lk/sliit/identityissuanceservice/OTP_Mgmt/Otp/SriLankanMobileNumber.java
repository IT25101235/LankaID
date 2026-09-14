package lk.sliit.identityissuanceservice.OTP_Mgmt.Otp;

/** Accepts the nine form digits, a local mobile number, or a +94 mobile number. */
public final class SriLankanMobileNumber {
    private SriLankanMobileNumber() { }

    public static String normalize(String input) {
        String number = input == null ? "" : input.trim().replace(" ", "").replace("-", "");
        if (number.matches("7[0-9]{8}")) number = "+94" + number;
        else if (number.matches("07[0-9]{8}")) number = "+94" + number.substring(1);
        if (!number.matches("\\+947[0-9]{8}")) {
            throw new IllegalArgumentException("Enter nine mobile digits after +94, starting with 7 (for example, 771234567).");
        }
        return number;
    }
}
