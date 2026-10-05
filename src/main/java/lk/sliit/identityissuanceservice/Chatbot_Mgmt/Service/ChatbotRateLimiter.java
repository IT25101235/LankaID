package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** Application-wide limits without storing IP addresses, identities, sessions, or messages. */
@Component
public class ChatbotRateLimiter {
    private final int requestsPerMinute;
    private final Semaphore inFlight;
    private final Clock clock;
    private long window;
    private int requests;

    @Autowired
    public ChatbotRateLimiter(@Value("${app.chatbot.requests-per-minute:30}") int requestsPerMinute,
                              @Value("${app.chatbot.max-concurrent-requests:4}") int concurrent) {
        this(requestsPerMinute, concurrent, Clock.systemUTC());
    }

    public ChatbotRateLimiter(int requestsPerMinute, int concurrent, Clock clock) {
        if (requestsPerMinute < 1 || concurrent < 1) throw new IllegalArgumentException("Chatbot limits must be positive.");
        this.requestsPerMinute = requestsPerMinute;
        this.inFlight = new Semaphore(concurrent);
        this.clock = clock;
        this.window = clock.millis() / 60_000;
    }

    public synchronized Permit acquire() {
        long minute = clock.millis() / 60_000;
        if (minute != window) { window = minute; requests = 0; }
        if (requests >= requestsPerMinute) throw new LimitExceeded(60);
        if (!inFlight.tryAcquire()) throw new LimitExceeded(3);
        requests++;
        return new Permit();
    }

    public final class Permit implements AutoCloseable {
        private final AtomicBoolean released = new AtomicBoolean();
        @Override public void close() {
            if (released.compareAndSet(false, true)) inFlight.release();
        }
    }

    public static final class LimitExceeded extends RuntimeException {
        private final int retryAfter;
        public LimitExceeded(int retryAfter) { super("Chatbot request limit reached."); this.retryAfter = retryAfter; }
        public int retryAfter() { return retryAfter; }
    }
}
