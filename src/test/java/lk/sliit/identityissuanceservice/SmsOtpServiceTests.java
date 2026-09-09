package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Shared.Otp.OtpChallenge;
import lk.sliit.identityissuanceservice.Shared.Otp.SmsOtpService;
import lk.sliit.identityissuanceservice.Shared.Otp.SriLankanMobileNumber;
import lk.sliit.identityissuanceservice.Shared.Sms.SmsSender;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SmsOtpServiceTests {
    private static final String PHONE = "+94771234567";
    private static final String PURPOSE = "WALK_IN_BOOKING";
    private final Clock clock = mock(Clock.class);
    private final AtomicReference<String> sentCode = new AtomicReference<>();
    private final SmsSender sender = mock(SmsSender.class);
    private final MockHttpSession session = new MockHttpSession();
    private final Instant start = Instant.parse("2026-09-07T00:00:00Z");
    private final SmsOtpService service = new SmsOtpService(sender, 180, 5, 60, 900, clock);

    SmsOtpServiceTests() {
        when(clock.instant()).thenReturn(start);
        doAnswer(call -> {
            assertEquals(PHONE, call.getArgument(0));
            assertEquals(180, (Integer) call.getArgument(2));
            String message = call.getArgument(1);
            assertTrue(message.matches("LankaID \\(OTP\\) - [0-9]{6}"));
            sentCode.set(message.substring("LankaID (OTP) - ".length()));
            return null;
        }).when(sender).send(anyString(), anyString(), anyInt());
    }

    @Test void normalizesSriLankanMobileFormatsAndRejectsInvalidNumbers() {
        for (String phone : new String[]{"771234567", "0771234567", PHONE, "+94 77-1234567"}) {
            assertEquals(PHONE, SriLankanMobileNumber.normalize(phone));
        }
        for (String phone : new String[]{"", "77123", "7712345678", "+940771234567", "+44771234567", "111234567", "77abc4567"}) {
            assertThrows(IllegalArgumentException.class, () -> SriLankanMobileNumber.normalize(phone));
        }
        assertThrows(IllegalArgumentException.class, () -> SriLankanMobileNumber.normalize(null));
    }

    @Test void storesOnlyHashAndConsumesCodeAfterVerification() {
        service.send("771234567", PURPOSE, session);
        OtpChallenge challenge = (OtpChallenge) session.getAttribute("smsOtp:" + PURPOSE);
        assertNotEquals(sentCode.get(), challenge.getCodeHash());
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
        service.verify(sentCode.get(), PURPOSE, session);
        assertNull(challenge.getCodeHash());
        assertTrue(service.isVerified(PHONE, PURPOSE, session));
        assertFalse(service.isVerified("+94779999999", PURPOSE, session));
        assertFalse(service.isVerified(PHONE, "OTHER_PURPOSE", session));
        assertThrows(IllegalArgumentException.class, () -> service.verify(sentCode.get(), PURPOSE, session));
        service.clear(PURPOSE, session);
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void expiresAtExactlyThreeMinutes() {
        service.send(PHONE, PURPOSE, session);
        when(clock.instant()).thenReturn(start.plusSeconds(180));
        assertThrows(IllegalArgumentException.class, () -> service.verify(sentCode.get(), PURPOSE, session));
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void rejectsCorrectCodeAfterFiveWrongAttemptsIncludingMalformedInput() {
        service.send(PHONE, PURPOSE, session);
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalArgumentException.class, () -> service.verify("invalid", PURPOSE, session));
        }
        assertThrows(IllegalArgumentException.class, () -> service.verify(sentCode.get(), PURPOSE, session));
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void incorrectSixDigitCodeDoesNotVerify() {
        service.send(PHONE, PURPOSE, session);
        String incorrect = sentCode.get().equals("000000") ? "000001" : "000000";
        assertThrows(IllegalArgumentException.class, () -> service.verify(incorrect, PURPOSE, session));
        service.verify(sentCode.get(), PURPOSE, session);
        assertTrue(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void cooldownAppliesAcrossSessionsAndResendReplacesChallenge() {
        service.send(PHONE, PURPOSE, session);
        Object oldChallenge = session.getAttribute("smsOtp:" + PURPOSE);
        assertThrows(IllegalArgumentException.class, () -> service.send(PHONE, PURPOSE, session));
        assertThrows(IllegalArgumentException.class, () -> service.send(PHONE, PURPOSE, new MockHttpSession()));
        verify(sender, times(1)).send(anyString(), anyString(), anyInt());
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        service.send(PHONE, PURPOSE, session);
        assertNotSame(oldChallenge, session.getAttribute("smsOtp:" + PURPOSE));
        service.verify(sentCode.get(), PURPOSE, session);
        assertTrue(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void reportsRemainingCooldownAcrossSessionsAndAllowsRetryAtDeadline() {
        service.send(PHONE, PURPOSE, session);
        when(clock.instant()).thenReturn(start.plusMillis(15_500));
        assertEquals(45, service.retryAfterSeconds(PHONE, PURPOSE, session));
        assertEquals("Please wait 45 seconds before requesting another code.",
                assertThrows(IllegalArgumentException.class,
                        () -> service.send(PHONE, PURPOSE, new MockHttpSession())).getMessage());
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        assertEquals(0, service.retryAfterSeconds(PHONE, PURPOSE, session));
        service.send(PHONE, PURPOSE, session);
    }

    @Test void failedSendRetainsCooldownWithoutExposingGatewayDetails() {
        doThrow(new IllegalArgumentException("API key rejected")).when(sender).send(anyString(), anyString(), anyInt());
        String message = assertThrows(IllegalArgumentException.class,
                () -> service.send(PHONE, PURPOSE, session)).getMessage();
        assertTrue(message.startsWith("We couldn't send your code. Please try again in 60 seconds."));
        assertFalse(message.contains("API key"));
        assertEquals(60, service.retryAfterSeconds(PHONE, PURPOSE, session));
        assertNull(session.getAttribute("smsOtp:" + PURPOSE));
    }

    @Test void failedResendClearsPreviouslyVerifiedState() {
        service.send(PHONE, PURPOSE, session);
        service.verify(sentCode.get(), PURPOSE, session);
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        doThrow(new IllegalArgumentException("SMS unavailable")).when(sender).send(anyString(), anyString(), anyInt());
        assertThrows(IllegalArgumentException.class, () -> service.send(PHONE, PURPOSE, session));
        assertNull(session.getAttribute("smsOtp:" + PURPOSE));
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
        assertThrows(IllegalArgumentException.class, () -> service.verify(sentCode.get(), PURPOSE, session));
    }

    @Test void verificationExpiresAndIsNotSharedBetweenSessions() {
        service.send(PHONE, PURPOSE, session);
        service.verify(sentCode.get(), PURPOSE, session);
        assertFalse(service.isVerified(PHONE, PURPOSE, new MockHttpSession()));
        when(clock.instant()).thenReturn(start.plusSeconds(900));
        assertFalse(service.isVerified(PHONE, PURPOSE, session));
    }

    @Test void rejectsMissingChallengeAndInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> service.verify("123456", PURPOSE, session));
        assertThrows(IllegalArgumentException.class, () -> new SmsOtpService(sender, 0, 5, 60, 900,
                Clock.fixed(start, ZoneOffset.UTC)));
    }
}
