package tn.gezenthi.search;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tn.gezenthi.supplier.HotelSupplier.SearchQuery;

/**
 * Keeps supplier booking references (Token, Source…) on the SERVER, keyed by an opaque offerId.
 * The browser only ever sees the offerId — it can't tamper with supplier tokens or see which
 * supplier/price chain is behind an offer. Entries expire with the supplier token.
 */
@Component
public class OfferRefCache {

    static final Duration TTL = Duration.ofMinutes(30);

    public record SupplierOfferRef(String supplierCode, String externalHotelId, String token, String source,
                                   SearchQuery query, Instant expiresAt) {
    }

    private final Map<String, SupplierOfferRef> refs = new ConcurrentHashMap<>();

    public String put(String supplierCode, String externalHotelId, String token, String source, SearchQuery query) {
        evictExpired();
        String offerId = UUID.randomUUID().toString();
        refs.put(offerId, new SupplierOfferRef(supplierCode, externalHotelId, token, source, query, Instant.now().plus(TTL)));
        return offerId;
    }

    public Optional<SupplierOfferRef> get(String offerId) {
        SupplierOfferRef ref = refs.get(offerId);
        if (ref == null || ref.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(ref);
    }

    private void evictExpired() {
        Instant now = Instant.now();
        refs.values().removeIf(r -> r.expiresAt().isBefore(now));
    }
}
