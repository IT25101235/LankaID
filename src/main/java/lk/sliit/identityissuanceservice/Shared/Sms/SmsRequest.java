package lk.sliit.identityissuanceservice.Shared.Sms;

/** The JSON body expected by the phone's SMS API. */
public record SmsRequest(String requestId, String to, String message, int ttlSeconds) {
}
