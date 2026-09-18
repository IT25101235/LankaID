package lk.sliit.identityissuanceservice.OTP_Mgmt.Email;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/** Session-scoped email OTP workflow shared by registration and password reset. */
@Service
public class EmailOtpService {
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final int ttlSeconds;
    private final int maxAttempts;
    private final int resendSeconds;
    private final int verificationSeconds;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    public EmailOtpService(JavaMailSender mailSender,
                           @Value("${app.mail.from:}") String fromAddress,
                           @Value("${app.otp.email-ttl-seconds:600}") int ttlSeconds,
                           @Value("${app.otp.max-attempts:5}") int maxAttempts,
                           @Value("${app.registration.email-resend-seconds:60}") int resendSeconds,
                           @Value("${app.otp.verification-seconds:900}") int verificationSeconds) {
        this(mailSender, fromAddress, ttlSeconds, maxAttempts, resendSeconds, verificationSeconds, Clock.systemUTC());
    }

    public EmailOtpService(JavaMailSender mailSender, String fromAddress, int ttlSeconds, int maxAttempts,
                    int resendSeconds, int verificationSeconds, Clock clock) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.ttlSeconds = ttlSeconds;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
        this.verificationSeconds = verificationSeconds;
        this.clock = clock;
    }

    public void send(String email, String purpose, HttpSession session) {
        String normalizedEmail = normalize(email);
        EmailOtpChallenge previous = challenge(purpose, session);
        Instant now = clock.instant();
        if (previous != null && now.isBefore(previous.getResendAt())) {
            throw new IllegalArgumentException("Please wait " + retryAfterSeconds(purpose, session)
                    + " seconds before requesting another code.");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("Email delivery is not configured.");
        }

        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(normalizedEmail);
        message.setSubject("Your LankaID verification code");
        message.setText("Your LankaID verification code is " + code + ". It expires in "
                + Math.max(1, ttlSeconds / 60) + " minutes. If you did not request this, ignore this email.");
        mailSender.send(message);
        session.setAttribute(key(purpose), new EmailOtpChallenge(normalizedEmail, encoder.encode(code),
                now.plusSeconds(ttlSeconds), now.plusSeconds(resendSeconds)));
    }

    public void verify(String code, String purpose, HttpSession session) {
        EmailOtpChallenge challenge = challenge(purpose, session);
        if (challenge == null) throw new IllegalArgumentException("Request a new verification code.");
        Instant now = clock.instant();
        if (!now.isBefore(challenge.getExpiresAt())) throw new IllegalArgumentException("The code expired. Request a new code.");
        if (challenge.getFailedAttempts() >= maxAttempts) throw new IllegalArgumentException("Too many incorrect attempts. Request a new code.");
        if (code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, challenge.getCodeHash())) {
            challenge.recordFailedAttempt();
            throw new IllegalArgumentException("Incorrect code. "
                    + Math.max(0, maxAttempts - challenge.getFailedAttempts()) + " attempt(s) remaining.");
        }
        challenge.markVerified(now.plusSeconds(verificationSeconds));
    }

    public boolean isVerified(String email, String purpose, HttpSession session) {
        EmailOtpChallenge challenge = challenge(purpose, session);
        String normalizedEmail;
        try {
            normalizedEmail = normalize(email);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return challenge != null && challenge.getEmail().equals(normalizedEmail)
                && challenge.getVerifiedUntil() != null
                && clock.instant().isBefore(challenge.getVerifiedUntil());
    }

    public long retryAfterSeconds(String purpose, HttpSession session) {
        EmailOtpChallenge challenge = challenge(purpose, session);
        if (challenge == null) return 0;
        return Math.max(0, (Duration.between(clock.instant(), challenge.getResendAt()).toMillis() + 999) / 1000);
    }

    public void clear(String purpose, HttpSession session) { session.removeAttribute(key(purpose)); }

    private EmailOtpChallenge challenge(String purpose, HttpSession session) {
        Object value = session.getAttribute(key(purpose));
        return value instanceof EmailOtpChallenge challenge ? challenge : null;
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email address is required.");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String key(String purpose) { return "emailOtp:" + purpose; }
}
