package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import java.io.Serializable;
import java.time.Instant;

public class EmailVerificationChallenge implements Serializable {

    private final String codeHash;
    private final Instant expiresAt;
    private final Instant resendAvailableAt;
    private int failedAttempts;

    public EmailVerificationChallenge(String codeHash, Instant expiresAt, Instant resendAvailableAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean canResend(Instant now) {
        return !now.isBefore(resendAvailableAt);
    }

    public long resendWaitSeconds(Instant now) {
        return Math.max(1, resendAvailableAt.getEpochSecond() - now.getEpochSecond());
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void recordFailedAttempt() {
        failedAttempts++;
    }
}
