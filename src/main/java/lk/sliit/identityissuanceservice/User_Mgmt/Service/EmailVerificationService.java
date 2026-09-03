package lk.sliit.identityissuanceservice.User_Mgmt.Service;

import lk.sliit.identityissuanceservice.User_Mgmt.Dto.EmailVerificationChallenge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class EmailVerificationService {

    private static final int MAX_ATTEMPTS = 5;

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final long codeLifetimeMinutes;
    private final long resendCooldownSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress,
            @Value("${app.registration.email-code-minutes:10}") long codeLifetimeMinutes,
            @Value("${app.registration.email-resend-seconds:60}") long resendCooldownSeconds) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.codeLifetimeMinutes = codeLifetimeMinutes;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public EmailVerificationChallenge sendVerificationCode(String recipientEmail) {
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException(
                    "Registration email is not configured. Set LANKAID_MAIL_USERNAME and LANKAID_MAIL_APP_PASSWORD."
            );
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipientEmail);
        message.setSubject("Your LankaID verification code");
        message.setText("Your LankaID email verification code is " + code
                + ". It expires in " + codeLifetimeMinutes
                + " minutes. If you did not request this, you can ignore this email.");
        mailSender.send(message);

        Instant now = Instant.now();
        return new EmailVerificationChallenge(
                hash(code),
                now.plus(codeLifetimeMinutes, ChronoUnit.MINUTES),
                now.plus(resendCooldownSeconds, ChronoUnit.SECONDS)
        );
    }

    public boolean isValid(EmailVerificationChallenge challenge, String submittedCode) {
        if (challenge == null || submittedCode == null || !submittedCode.matches("\\d{6}")) {
            return false;
        }
        if (challenge.isExpired(Instant.now()) || challenge.getFailedAttempts() >= MAX_ATTEMPTS) {
            return false;
        }
        return MessageDigest.isEqual(
                challenge.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(submittedCode).getBytes(StandardCharsets.UTF_8)
        );
    }

    public int attemptsRemaining(EmailVerificationChallenge challenge) {
        return Math.max(0, MAX_ATTEMPTS - challenge.getFailedAttempts());
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
