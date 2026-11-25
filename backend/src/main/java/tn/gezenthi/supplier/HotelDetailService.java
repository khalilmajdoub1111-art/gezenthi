package tn.gezenthi.supplier;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Supplier hotel details (photos, description, location…), fetched ON DEMAND the first time
 * a hotel page is opened and cached for a week — no need to call HotelDetail for 500+ hotels.
 * If the supplier is unavailable, the cached (even stale) version or nothing is returned:
 * the page still works with the basic info from the sync.
 */
@Service
public class HotelDetailService {

    private static final Logger log = LoggerFactory.getLogger(HotelDetailService.class);
    static final Duration TTL = Duration.ofDays(7);

    /** Text formatting only: no scripts, styles, iframes, images or links. */
    private static final Safelist SAFE_HTML = Safelist.basic().removeTags("a", "img");

    private final SupplierRegistry registry;
    private final SupplierHotelDetailRepository details;
    private final TransactionTemplate tx;

    HotelDetailService(SupplierRegistry registry, SupplierHotelDetailRepository details, TransactionTemplate tx) {
        this.registry = registry;
        this.details = details;
        this.tx = tx;
    }

    public Optional<SupplierHotelDetail> forHotel(SupplierHotel hotel) {
        Optional<SupplierHotelDetail> cached = details.findById(hotel.getId());
        if (cached.isPresent() && cached.get().getFetchedAt().isAfter(Instant.now().minus(TTL))) return cached;

        Optional<HotelSupplier> supplier = registry.find(hotel.getSupplierCode()).filter(HotelSupplier::configured);
        if (supplier.isEmpty()) return cached;
        try {
            HotelSupplier.HotelDetails d = supplier.get().hotelDetail(hotel.getExternalId());
            return Optional.ofNullable(tx.execute(status -> {
                SupplierHotelDetail row = details.findById(hotel.getId()).orElseGet(() -> new SupplierHotelDetail(hotel.getId()));
                row.fill(d, sanitize(d.description()), sanitize(d.notes()), Instant.now());
                return details.save(row);
            }));
        } catch (SupplierException e) {
            log.warn("HotelDetail {}#{} unavailable: {}", hotel.getSupplierCode(), hotel.getExternalId(), e.getMessage());
            return cached;
        }
    }

    static String sanitize(String html) {
        if (html == null || html.isBlank()) return null;
        String clean = Jsoup.clean(html, SAFE_HTML).trim();
        if (clean.length() > 20_000) clean = clean.substring(0, 20_000);
        return clean.isBlank() ? null : clean;
    }
}
