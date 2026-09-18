package lk.sliit.identityissuanceservice.OTP_Mgmt.Email;

import java.io.Serializable;
import java.time.Instant;

public class EmailOtpChallenge implements Serializable {
    private final String email;
    private final String codeHash;
    private final Instant expiresAt;
    private final Instant resendAt;
    private int failedAttempts;
    private Instant verifiedUntil;

    public EmailOtpChallenge(String email, String codeHash, Instant expiresAt, Instant resendAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.resendAt = resendAt;
    }

    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResendAt() { return resendAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getVerifiedUntil() { return verifiedUntil; }
    public void recordFailedAttempt() { failedAttempts++; }
    public void markVerified(Instant until) { verifiedUntil = until; }
}
