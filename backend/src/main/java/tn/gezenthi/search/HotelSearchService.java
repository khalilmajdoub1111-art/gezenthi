package tn.gezenthi.search;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.catalog.hotel.Hotel;
import tn.gezenthi.catalog.hotel.HotelDto;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.HotelSupplier.SearchQuery;
import tn.gezenthi.supplier.SupplierHotel;
import tn.gezenthi.supplier.SupplierHotelRepository;
import tn.gezenthi.supplier.SupplierRegistry;

/**
 * Hotel search for the public site, merging:
 *  1. the agency's own hotels (managed in the admin, fixed price per night/person), and
 *  2. live offers from every enabled supplier (TunisiaBeds…), with the agency markup applied.
 * Suppliers run in parallel with a timeout; a failing supplier never breaks the search.
 *
 * Hotel refs (stable, public): "a-<uuid>" = agency hotel, "p-<uuid>" = partner (supplier) hotel,
 * using OUR ids — supplier ids and names are never exposed.
 */
@Service
public class HotelSearchService {

    private static final Logger log = LoggerFactory.getLogger(HotelSearchService.class);
    private static final long SUPPLIER_TIMEOUT_SECONDS = 30;
    private static final long RESULT_CACHE_SECONDS = 300;

    /** Ids let a booking point at an exact rate; prices are totals for the stay. */
    public record RoomResult(String id, String name, BigDecimal totalPrice, boolean available, boolean refundable,
                             String cancellationDeadline) {
    }

    public record BoardResult(String id, String code, String name, List<RoomResult> rooms) {
    }

    /** origin: "agence" (own offer) or "partenaire" (supplier) — the supplier name is never exposed. */
    public record HotelResult(String offerId, String ref, String origin, String name, String city, Integer stars,
                              String image, String currency, BigDecimal fromPrice, List<String> tags,
                              List<BoardResult> boards) {
    }

    public record SearchResponse(String city, LocalDate checkIn, LocalDate checkOut, long nights,
                                 List<HotelResult> hotels, List<String> warnings) {
    }

    private record CachedResponse(SearchResponse response, Instant expiresAt) {
    }

    private final CatalogCrud<Hotel, HotelDto> ownHotels;
    private final SupplierRegistry suppliers;
    private final SupplierHotelRepository supplierHotels;
    private final OfferRefCache offerRefs;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    HotelSearchService(CatalogCrud<Hotel, HotelDto> ownHotels, SupplierRegistry suppliers,
                       SupplierHotelRepository supplierHotels, OfferRefCache offerRefs) {
        this.ownHotels = ownHotels;
        this.suppliers = suppliers;
        this.supplierHotels = supplierHotels;
        this.offerRefs = offerRefs;
    }

    // ------------------------------------------------------------------ city search

    public SearchResponse search(SearchQuery q) {
        String key = q.toString();
        CachedResponse cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return cached.response();

        List<HotelResult> results = new ArrayList<>(ownHotels.findPublished().stream()
                .filter(h -> h.city().equalsIgnoreCase(q.city().trim()))
                .map(h -> ownOffer(h, q)).toList());
        List<String> warnings = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<List<HotelResult>>> calls = suppliers.activeForSearch().stream()
                .map(s -> {
                    // DB lookup in the request thread; only the remote API call runs in parallel.
                    List<SupplierHotel> inCity = searchable(s, supplierHotels.findActiveInCity(s.code(), q.city().trim()));
                    return CompletableFuture.supplyAsync(() -> supplierOffers(s, q, inCity), pool)
                            .orTimeout(SUPPLIER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                            .exceptionally(e -> {
                                log.warn("Search: supplier {} failed: {}", s.code(), e.getMessage());
                                warnings.add("Certaines offres partenaires sont momentanément indisponibles.");
                                return List.<HotelResult>of();
                            });
                })
                .toList();
        calls.forEach(c -> results.addAll(c.join()));

        results.sort(Comparator.comparing(HotelResult::fromPrice, Comparator.nullsLast(Comparator.naturalOrder())));
        SearchResponse response = new SearchResponse(q.city(), q.checkIn(), q.checkOut(), q.nights(),
                results, warnings.stream().distinct().toList());
        if (warnings.isEmpty()) {
            cache.values().removeIf(c -> c.expiresAt().isBefore(Instant.now()));
            cache.put(key, new CachedResponse(response, Instant.now().plusSeconds(RESULT_CACHE_SECONDS)));
        }
        return response;
    }

    // ------------------------------------------------------------------ single hotel (hotel page)

    /** Live availability for one agency hotel. */
    public Optional<HotelResult> availabilityOwn(HotelDto hotel, SearchQuery q) {
        return hotel.published() ? Optional.of(ownOffer(hotel, q)) : Optional.empty();
    }

    /** Live availability for one partner hotel (empty if not sellable / no rates). */
    public Optional<HotelResult> availabilityPartner(SupplierHotel hotel, SearchQuery q) {
        Optional<HotelSupplier> s = suppliers.activeForSearch().stream()
                .filter(x -> x.code().equals(hotel.getSupplierCode())).findFirst();
        if (s.isEmpty()) return Optional.empty();
        List<SupplierHotel> allowed = searchable(s.get(), List.of(hotel));
        return supplierOffers(s.get(), q, allowed).stream().findFirst();
    }

    // ------------------------------------------------------------------ internals

    /** Agency hotel: price per night and per person × nights × adults (children on request). */
    private HotelResult ownOffer(HotelDto h, SearchQuery q) {
        int adults = q.rooms().stream().mapToInt(HotelSupplier.Room::adults).sum();
        BigDecimal total = BigDecimal.valueOf(h.pricePerNight()).multiply(BigDecimal.valueOf(q.nights() * adults));
        RoomResult room = new RoomResult("standard", "Chambre standard", total, true, true, null);
        String offerId = offerRefs.newId();
        String ref = "a-" + h.id();
        HotelResult result = new HotelResult(offerId, ref, "agence", h.name(), h.city(), h.stars(), h.image(), "TND",
                total, h.tags(), List.of(new BoardResult("board", null, h.board(), List.of(room))));
        offerRefs.put(offerId, new OfferRefCache.OfferRef(ref, null, null, null, null, null, null, q, result, OfferRefCache.expiry()));
        return result;
    }

    /** Applies the supplier's "only these hotels" restriction (test accounts). */
    private List<SupplierHotel> searchable(HotelSupplier s, List<SupplierHotel> hotels) {
        List<String> only = suppliers.settings(s.code()).getOnlyHotels();
        return hotels.stream().filter(h -> only.isEmpty() || only.contains(h.getExternalId())).toList();
    }

    private List<HotelResult> supplierOffers(HotelSupplier s, SearchQuery q, List<SupplierHotel> hotels) {
        if (hotels.isEmpty()) return List.of();
        Map<String, SupplierHotel> byId = new ConcurrentHashMap<>();
        hotels.forEach(h -> byId.put(h.getExternalId(), h));
        BigDecimal factor = BigDecimal.ONE.add(
                Objects.requireNonNullElse(suppliers.settings(s.code()).getMarkupPercent(), BigDecimal.ZERO)
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));

        return s.search(q, List.copyOf(byId.keySet())).stream().map(o -> {
            SupplierHotel known = byId.get(o.externalHotelId());
            List<BoardResult> boards = o.boards().stream().map(b -> new BoardResult(b.id(), b.code(), b.name(),
                    b.rooms().stream().filter(r -> r.price() != null).map(r -> new RoomResult(
                            r.id(), r.name(), sellPrice(r.price(), factor), r.bookable(), r.refundable(), r.cancellationDeadline()))
                            .toList()))
                    .filter(b -> !b.rooms().isEmpty()).toList();
            BigDecimal from = boards.stream().flatMap(b -> b.rooms().stream()).filter(RoomResult::available)
                    .map(RoomResult::totalPrice).min(Comparator.naturalOrder()).orElse(null);
            if (from == null) return null;
            String offerId = offerRefs.newId();
            String ref = known == null ? null : "p-" + known.getId();
            HotelResult result = new HotelResult(offerId, ref, "partenaire",
                    o.hotelName().isBlank() && known != null ? known.getName() : o.hotelName(),
                    o.cityName().isBlank() && known != null ? known.getCityName() : o.cityName(),
                    o.stars() != null ? o.stars() : known == null ? null : known.getStars(),
                    o.image() != null ? o.image() : known == null ? null : known.getImage(),
                    o.currency() == null ? "TND" : o.currency(), from, List.of(), boards);
            offerRefs.put(offerId, new OfferRefCache.OfferRef(ref, s.code(), o.externalHotelId(),
                    known == null ? null : known.getCityExternalId(), o.token(), o.source(), o, q, result, OfferRefCache.expiry()));
            return result;
        }).filter(Objects::nonNull).toList();
    }

    /** Sell price = supplier net price × (1 + markup), rounded up to the next whole unit. */
    static BigDecimal sellPrice(BigDecimal net, BigDecimal factor) {
        return net.multiply(factor).setScale(0, RoundingMode.CEILING);
    }
}
