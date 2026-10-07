package lk.sliit.identityissuanceservice.OTP_Mgmt.Sms;

/** The JSON body expected by the phone's SMS API. */
public record SmsRequest(String requestId, String to, String message, int ttlSeconds) {
}
