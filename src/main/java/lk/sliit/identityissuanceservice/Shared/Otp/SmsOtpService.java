package lk.sliit.identityissuanceservice.Shared.Otp;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Shared.Sms.SmsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Reusable OTP rules; purpose keeps future workflows separate from walk-in booking. */
@Service
public class SmsOtpService {
    private final SmsSender smsSender;
    private final int ttlSeconds;
    private final int maxAttempts;
    private final int resendSeconds;
    private final int verificationSeconds;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final Map<String, Instant> phoneCooldowns = new HashMap<>();

    @Autowired
    public SmsOtpService(SmsSender smsSender,
                         @Value("${app.otp.ttl-seconds:180}") int ttlSeconds,
                         @Value("${app.otp.max-attempts:5}") int maxAttempts,
                         @Value("${app.otp.resend-seconds:60}") int resendSeconds,
                         @Value("${app.otp.verification-seconds:900}") int verificationSeconds) {
        this(smsSender, ttlSeconds, maxAttempts, resendSeconds, verificationSeconds, Clock.systemUTC());
    }

    // A clock can be supplied in tests to check expiry without waiting three minutes.
    public SmsOtpService(SmsSender smsSender, int ttlSeconds, int maxAttempts,
                         int resendSeconds, int verificationSeconds, Clock clock) {
        if (ttlSeconds < 1 || maxAttempts < 1 || resendSeconds < 1 || verificationSeconds < 1) {
            throw new IllegalArgumentException("OTP settings must be positive numbers.");
        }
        this.smsSender = smsSender;
        this.ttlSeconds = ttlSeconds;
        this.maxAttempts = maxAttempts;
        this.resendSeconds = resendSeconds;
        this.verificationSeconds = verificationSeconds;
        this.clock = clock;
    }

    public void send(String phone, String purpose, HttpSession session) {
        String normalizedPhone = SriLankanMobileNumber.normalize(phone);
        synchronized (session) {
            Instant now = clock.instant();
            OtpChallenge previous = challenge(purpose, session);
            if (previous != null && now.isBefore(previous.getResendAt())) {
                throw new IllegalArgumentException(waitMessage(remainingSeconds(previous.getResendAt(), now)));
            }
            reservePhoneSend(normalizedPhone, now);
            // A new request invalidates the old code and any previous verification.
            clear(purpose, session);
            String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            OtpChallenge next = new OtpChallenge(normalizedPhone, encoder.encode(code),
                    now.plusSeconds(ttlSeconds), now.plusSeconds(resendSeconds));
            try {
                smsSender.send(normalizedPhone, "LankaID (OTP) - " + code, ttlSeconds);
            } catch (IllegalArgumentException exception) {
                long seconds = remainingSeconds(next.getResendAt(), clock.instant());
                throw new IllegalArgumentException("We couldn't send your code. "
                        + (seconds > 0 ? "Please try again in " + seconds + " seconds." : "Please try again.")
                        + " If this keeps happening, contact the service centre.", exception);
            }
            // A failed API call must never create a usable challenge.
            session.setAttribute(key(purpose), next);
        }
    }

    public void verify(String code, String purpose, HttpSession session) {
        synchronized (session) {
            OtpChallenge challenge = challenge(purpose, session);
            if (challenge == null || challenge.getCodeHash() == null) {
                throw new IllegalArgumentException("Request a new verification code.");
            }
            Instant now = clock.instant();
            if (!now.isBefore(challenge.getExpiresAt())) {
                throw new IllegalArgumentException("The OTP expired. Request a new code.");
            }
            if (challenge.getFailedAttempts() >= maxAttempts) {
                throw new IllegalArgumentException("Too many incorrect attempts. Request a new code.");
            }
            if (code == null || !code.matches("[0-9]{6}") || !encoder.matches(code, challenge.getCodeHash())) {
                challenge.recordFailedAttempt();
                throw new IllegalArgumentException("Incorrect OTP. "
                        + (maxAttempts - challenge.getFailedAttempts()) + " attempt(s) remaining.");
            }
            challenge.markVerified(now.plusSeconds(verificationSeconds));
        }
    }

    public boolean isVerified(String phone, String purpose, HttpSession session) {
        synchronized (session) {
            OtpChallenge challenge = challenge(purpose, session);
            return challenge != null && challenge.getPhone().equals(phone)
                    && challenge.getVerifiedUntil() != null
                    && clock.instant().isBefore(challenge.getVerifiedUntil());
        }
    }

    public void clear(String purpose, HttpSession session) {
        session.removeAttribute(key(purpose));
    }

    // Also limits resending to the same phone from a different browser session.
    private synchronized void reservePhoneSend(String phone, Instant now) {
        phoneCooldowns.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
        if (phoneCooldowns.containsKey(phone)) {
            throw new IllegalArgumentException(waitMessage(remainingSeconds(phoneCooldowns.get(phone), now)));
        }
        phoneCooldowns.put(phone, now.plusSeconds(resendSeconds));
    }

    private OtpChallenge challenge(String purpose, HttpSession session) {
        Object value = session.getAttribute(key(purpose));
        return value instanceof OtpChallenge challenge ? challenge : null;
    }

    public long retryAfterSeconds(String phone, String purpose, HttpSession session) {
        synchronized (session) {
            Instant now = clock.instant();
            OtpChallenge previous = challenge(purpose, session);
            long sessionWait = previous == null ? 0 : remainingSeconds(previous.getResendAt(), now);
            String normalizedPhone;
            try {
                normalizedPhone = SriLankanMobileNumber.normalize(phone);
            } catch (IllegalArgumentException exception) {
                return sessionWait;
            }
            synchronized (this) {
                return Math.max(sessionWait, remainingSeconds(phoneCooldowns.get(normalizedPhone), now));
            }
        }
    }

    private long remainingSeconds(Instant until, Instant now) {
        return until == null ? 0 : Math.max(0, (Duration.between(now, until).toMillis() + 999) / 1000);
    }

    private String waitMessage(long seconds) {
        return "Please wait " + seconds + " seconds before requesting another code.";
    }

    private String key(String purpose) { return "smsOtp:" + purpose; }
}
