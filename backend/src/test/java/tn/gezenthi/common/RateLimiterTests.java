package tn.gezenthi.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RateLimiterTests {

    /** Mutable clock for tests. */
    static final class TestClock extends Clock {
        Instant now = Instant.parse("2027-01-01T10:00:00Z");
        public ZoneOffset getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    @Test
    void blocksAfterLimitAndRecoversAfterWindow() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), "trop", clock);
        for (int i = 0; i < 3; i++) limiter.check("1.2.3.4");
        assertThatThrownBy(() -> limiter.check("1.2.3.4")).isInstanceOf(ResponseStatusException.class);
        limiter.check("5.6.7.8"); // other visitors unaffected

        clock.now = clock.now.plusSeconds(61);
        limiter.check("1.2.3.4");
    }

    @Test
    void forgetsIdleVisitorsSoMemoryStaysBounded() {
        TestClock clock = new TestClock();
        RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1), "trop", clock);
        for (int i = 0; i < 499; i++) limiter.check("10.0.0." + i);
        assertThat(limiter.trackedKeys()).isEqualTo(499);

        clock.now = clock.now.plusSeconds(120);
        limiter.check("new-visitor"); // 500th call triggers the purge
        assertThat(limiter.trackedKeys()).isEqualTo(1);
    }
}
