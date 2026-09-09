# Walk-in SMS OTP: setup and explanation

## Setup

The application sends a **POST** request to the full URL configured in `src/main/resources/application.properties`. The phone must be reachable from the computer running the backend.

Set these environment variables in your IDE run configuration or PowerShell before starting the application:

```powershell
$env:LANKAID_SMS_URL="http://YOUR_PHONE_IP:PORT/YOUR_SMS_ENDPOINT"
$env:LANKAID_SMS_API_KEY="your-api-key"
```

The corresponding properties are `app.sms.url` and `app.sms.api-key`. You can edit the URL in the properties file instead; keep real keys out of source control. Restart the application after changing the configuration. Supply the **complete API URL**, including its path, rather than only the phone IP.

The endpoint path is `/sms/` (for example, `http://192.168.1.7:8080/sms/`). The response example has not yet been supplied. This implementation assumes POST and treats HTTP 2xx as acceptance by the gateway. If your API reports failures inside a 2xx JSON response, add its documented success check in `PhoneSmsSender`. Acceptance does not guarantee delivery to the handset. The client has bounded connection/read timeouts and does not automatically retry a failed POST, since the phone might already have accepted it.

## API request

```http
Authorization: Bearer <configured-api-key>
Content-Type: application/json
```

```json
{
  "requestId": "<new UUID for each send>",
  "to": "+94778833578",
  "message": "LankaID (OTP) - <random six-digit code>",
  "ttlSeconds": 180
}
```

## Files to explain to the lecturer

All shared paths below start at `src/main/java/lk/sliit/identityissuanceservice/Shared/`.

| File | Responsibility |
| --- | --- |
| `Sms/SmsSender.java` | Small interface for sending SMS; another provider can implement it. |
| `Sms/SmsRequest.java` | The four fields in the API's JSON body. |
| `Sms/PhoneSmsSender.java` | Reads configuration and sends the authenticated HTTP request. |
| `Otp/SriLankanMobileNumber.java` | Validates mobile numbers and converts them to `+947XXXXXXXX`. |
| `Otp/OtpChallenge.java` | Temporary OTP hash, number, expiry, attempts and verification state. |
| `Otp/SmsOtpService.java` | Generates, sends and verifies codes; applies expiry and resend limits. |

`WalkInAppointmentService` calls the OTP service using the purpose `WALK_IN_BOOKING`. It continues to handle appointments. The controller routes requests and displays feedback. The existing booking template shows a fixed `+94` prefix with nine editable digits. Its small `walkin-phone.js` script validates typing and accepts pasted local or international numbers. The backend validates independently and saves the canonical `+94` number. Recovery also checks the older `07...` format so previous local-number bookings still work.

## Flow and storage

1. Visitor enters name and nine mobile digits starting with 7.
2. Backend normalizes the number, checks resend limits and generates six random digits using `SecureRandom`.
3. The backend sends `LankaID (OTP) - <code>` through `SmsSender`.
4. Only after gateway acceptance, a challenge with a salted BCrypt hash is saved in the server-side HTTP session. The application does not log or persist the plain code. No new database table is needed.
5. Visitor submits the code. The backend checks expiry, attempts and the hash.
6. Success removes the code hash and grants verification for this phone and purpose for 15 minutes. The same code cannot be verified again.
7. Booking checks this verification before saving; successful booking clears it.

OTP expiry is 180 seconds, matching the API TTL. Five wrong attempts block the challenge. Resend is limited to once per 60 seconds, both within the session and for the same number across sessions. Malformed code submissions also count as attempts. A new send replaces the previous code and verification; if the API call fails, no usable new challenge is saved. A failed send still uses the phone cooldown, so retry after 60 seconds. All settings are configurable under `app.otp.*`.

The challenge is temporary session data, not a database record. If the session is lost, request another code. The phone cooldown map lives in this application's memory. Multiple application instances would need shared rate-limit storage; persistent challenges would need database/Redis storage. Session storage is kept directly in the OTP service to keep this first implementation easy to follow.

## SOLID in this implementation

- **Single responsibility:** sending SMS, OTP rules, number validation and appointment booking are separate.
- **Open/closed:** add a new `SmsSender` implementation to change gateways without editing booking or OTP rules.
- **Liskov substitution:** each sender follows the same contract: send the message or report failure.
- **Interface segregation:** the sender interface contains only the operation callers need.
- **Dependency inversion:** OTP rules depend on `SmsSender`, injected through the constructor, rather than creating a phone API client.

The HTTP session dependency stays explicit instead of introducing an additional storage abstraction for this small project. Future SMS OTP workflows can call `SmsOtpService` with their own purpose. Existing registration email verification remains separate.

## Verification

`SmsOtpServiceTests` checks normalization, hashing, expiry, attempts, single use, resending, failure and session/purpose isolation. `PhoneSmsSenderTests` uses a local HTTP server to verify JSON, Bearer authentication and gateway error handling. `WalkInAppointmentWorkflowIntegrationTests` replaces the real sender with a mock and verifies the actual generated code during the booking workflow. It requires the configured SQL Server database and uses rollback.

For a live check, configure your endpoint/key, run the application, open `/walk-in`, enter your number, receive the SMS, verify it and book an available slot. Live delivery requires your running phone server and has not been inferred from automated tests.
