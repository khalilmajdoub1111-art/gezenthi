package tn.gezenthi.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Brute-force protection: after {@value #MAX_FAILURES} failed logins for the same
 * email within the window, further attempts are refused until the lock expires.
 * In-memory (resets on restart) — enough for a single server; move to Redis/DB if scaled out.
 */
@Component
class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK = Duration.ofMinutes(15);

    private record Attempts(int failures, Instant firstFailure) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    boolean isLocked(String email) {
        Attempts a = attempts.get(key(email));
        if (a == null) return false;
        if (expired(a)) {
            attempts.remove(key(email));
            return false;
        }
        return a.failures() >= MAX_FAILURES;
    }

    void recordFailure(String email) {
        attempts.compute(key(email), (k, a) -> a == null || expired(a)
                ? new Attempts(1, clock.instant())
                : new Attempts(a.failures() + 1, a.firstFailure()));
    }

    void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private boolean expired(Attempts a) {
        return a.firstFailure().plus(LOCK).isBefore(clock.instant());
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
