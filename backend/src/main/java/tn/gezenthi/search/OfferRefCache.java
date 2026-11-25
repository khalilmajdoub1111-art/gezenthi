package tn.gezenthi.search;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.HotelSupplier.SearchQuery;

/**
 * Server-side memory of every offer shown to a visitor, keyed by an opaque offerId.
 *
 * - Supplier booking references (Token, Source, supplier name, net prices) never leave the server.
 * - A booking request only sends offerId + chosen room: hotel, dates and PRICE are taken from here,
 *   so a visitor can't tamper with the price in the browser.
 * Entries expire with the supplier token (30 min); the visitor then simply searches again.
 */
@Component
public class OfferRefCache {

    static final Duration TTL = Duration.ofMinutes(30);

    /** supplierCode/token/source/supplierOffer are null for the agency's own offers. */
    public record OfferRef(String hotelRef, String supplierCode, String externalHotelId, String cityExternalId,
                           String token, String source, HotelSupplier.Offer supplierOffer, SearchQuery query,
                           HotelSearchService.HotelResult result, Instant expiresAt) {
    }

    private final Map<String, OfferRef> refs = new ConcurrentHashMap<>();

    public String newId() {
        return UUID.randomUUID().toString();
    }

    public void put(String offerId, OfferRef ref) {
        evictExpired();
        refs.put(offerId, ref);
    }

    public Optional<OfferRef> get(String offerId) {
        if (offerId == null) return Optional.empty();
        OfferRef ref = refs.get(offerId);
        if (ref == null || ref.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(ref);
    }

    static Instant expiry() {
        return Instant.now().plus(TTL);
    }

    private void evictExpired() {
        Instant now = Instant.now();
        refs.values().removeIf(r -> r.expiresAt().isBefore(now));
    }
}
