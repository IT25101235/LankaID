package lk.sliit.identityissuanceservice.OTP_Mgmt.Otp;

import java.io.Serializable;
import java.time.Instant;

/** Temporary server-side state. The actual SMS code is never stored here. */
public class OtpChallenge implements Serializable {
    private final String phone;
    private String codeHash;
    private final Instant expiresAt;
    private final Instant resendAt;
    private int failedAttempts;
    private Instant verifiedUntil;

    public OtpChallenge(String phone, String codeHash, Instant expiresAt, Instant resendAt) {
        this.phone = phone;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.resendAt = resendAt;
    }

    public String getPhone() { return phone; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResendAt() { return resendAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getVerifiedUntil() { return verifiedUntil; }
    public void recordFailedAttempt() { failedAttempts++; }

    public void markVerified(Instant until) {
        codeHash = null; // Consumes the OTP so it cannot be verified again.
        verifiedUntil = until;
    }
}
