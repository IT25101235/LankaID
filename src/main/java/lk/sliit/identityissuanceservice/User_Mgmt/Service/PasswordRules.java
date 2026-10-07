package lk.sliit.identityissuanceservice.User_Mgmt.Service;

import java.nio.charset.StandardCharsets;

/** Shared by citizen registration and administrator-created officer accounts. */
public final class PasswordRules {
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{8,72}$";
    public static final String MESSAGE = "Use 8 to 72 characters with an uppercase letter, a lowercase letter, a number and a symbol, without spaces.";

    private PasswordRules() { }

    public static void validate(String password) {
        if (password == null || !password.matches(PATTERN)) {
            throw new IllegalArgumentException(MESSAGE);
        }
        // BCrypt accepts at most 72 UTF-8 bytes, which can be fewer than 72 letters.
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password is too long. Use at most 72 UTF-8 bytes (fewer non-English characters).");
        }
    }
}
