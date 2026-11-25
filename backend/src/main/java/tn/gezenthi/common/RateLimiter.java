package tn.gezenthi.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sliding-window rate limiter keyed by client (IP). Memory-bounded: keys with no recent hit
 * are purged regularly, so the map can't grow forever with every visitor seen.
 * In-memory (per server instance) — move to Redis if the backend is scaled out.
 *
 * NOTE (deployment): behind a reverse proxy, configure forwarded headers so the key is the
 * real visitor IP, otherwise every visitor shares the proxy's IP.
 */
public final class RateLimiter {

    private static final int PURGE_EVERY = 500;

    private final int maxHits;
    private final Duration window;
    private final String message;
    private final Clock clock;
    private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
    private final AtomicInteger callsSincePurge = new AtomicInteger();

    public RateLimiter(int maxHits, Duration window, String message) {
        this(maxHits, window, message, Clock.systemUTC());
    }

    RateLimiter(int maxHits, Duration window, String message, Clock clock) {
        this.maxHits = maxHits;
        this.window = window;
        this.message = message;
        this.clock = clock;
    }

    /** Records a hit for this key; throws 429 if the limit is exceeded. */
    public void check(String key) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        Deque<Instant> q = hits.computeIfAbsent(key == null ? "?" : key, k -> new ArrayDeque<>());
        boolean allowed;
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst().isBefore(cutoff)) q.pollFirst();
            allowed = q.size() < maxHits;
            if (allowed) q.addLast(now);
        }
        maybePurge(cutoff);
        if (!allowed) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    int trackedKeys() {
        return hits.size();
    }

    private void maybePurge(Instant cutoff) {
        if (callsSincePurge.incrementAndGet() < PURGE_EVERY) return;
        callsSincePurge.set(0);
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Instant last = e.getValue().peekLast();
                return last == null || last.isBefore(cutoff);
            }
        });
    }
}
