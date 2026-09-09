package lk.sliit.identityissuanceservice.Shared.Sms;

/** Implement this interface to use a different SMS provider. */
public interface SmsSender {
    void send(String phone, String message, int ttlSeconds);
}
