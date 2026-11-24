package tn.gezenthi.supplier;

import java.time.Instant;

/** The supplier refused the call because of its rate limit (HTTP/code 429). */
public class SupplierRateLimitException extends SupplierException {

    private final Instant retryAfter;

    public SupplierRateLimitException(String message, Instant retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    /** When the supplier says we may call again (null if it didn't say). */
    public Instant retryAfter() {
        return retryAfter;
    }
}
