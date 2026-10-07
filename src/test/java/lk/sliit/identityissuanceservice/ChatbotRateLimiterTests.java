package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service.ChatbotRateLimiter;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ChatbotRateLimiterTests {
    @Test void minuteLimitResetsWithoutStoringAnyUserIdentity() {
        TestClock clock = new TestClock();
        ChatbotRateLimiter limiter = new ChatbotRateLimiter(2, 2, clock);
        limiter.acquire().close();
        limiter.acquire().close();
        assertEquals(60, assertThrows(ChatbotRateLimiter.LimitExceeded.class, limiter::acquire).retryAfter());
        clock.now = clock.now.plusSeconds(60);
        assertDoesNotThrow(() -> limiter.acquire().close());
    }

    @Test void concurrentSlotsAreReleasedAfterFailureAndOnlyOnce() {
        ChatbotRateLimiter limiter = new ChatbotRateLimiter(20, 1);
        var permit = limiter.acquire();
        assertEquals(3, assertThrows(ChatbotRateLimiter.LimitExceeded.class, limiter::acquire).retryAfter());
        permit.close(); permit.close();
        var next = limiter.acquire();
        assertThrows(ChatbotRateLimiter.LimitExceeded.class, limiter::acquire);
        next.close();
        assertDoesNotThrow(() -> limiter.acquire().close());
    }

    private static class TestClock extends Clock {
        Instant now = Instant.parse("2026-10-05T00:00:00Z");
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return now; }
    }
}
