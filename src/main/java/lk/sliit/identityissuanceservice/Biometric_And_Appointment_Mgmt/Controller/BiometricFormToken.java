package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Require biometric and appointment forms to come from the browser session that displayed them. */
@ControllerAdvice(assignableTypes = {AppointmentController.class, BiometricController.class,
        BiometricScheduleController.class})
public class BiometricFormToken {
    public static final String KEY = "biometricFormToken";

    @ModelAttribute("formToken")
    public String token(HttpSession session) {
        if (session.getAttribute(KEY) == null) session.setAttribute(KEY, UUID.randomUUID().toString());
        return (String) session.getAttribute(KEY);
    }

    /** Rejects the request if the token is missing or different (blocks forged cross-site form posts). */
    // OOP - Abstraction: callers use BiometricFormToken.check() without knowing how the token is stored or
    // compared.
    public static void check(String token, HttpSession session) {
        Object expected = session.getAttribute(KEY);
        if (token == null || !(expected instanceof String value)
                || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                        value.getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Reload the page and try again.");
        }
    }
}
