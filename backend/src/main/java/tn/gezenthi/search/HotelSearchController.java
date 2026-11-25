package tn.gezenthi.search;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import tn.gezenthi.catalog.CatalogCrud;
import tn.gezenthi.common.RateLimiter;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.SupplierRegistry;
import tn.gezenthi.catalog.hotel.Hotel;
import tn.gezenthi.catalog.hotel.HotelDto;
import tn.gezenthi.supplier.HotelDetailService;
import tn.gezenthi.supplier.HotelSupplier.Room;
import tn.gezenthi.supplier.HotelSupplier.SearchQuery;
import tn.gezenthi.supplier.SupplierHotel;
import tn.gezenthi.supplier.SupplierHotelDetail;
import tn.gezenthi.supplier.SupplierHotelRepository;

/**
 * Public hotel endpoints:
 *  GET /api/public/hotels/search?city&checkIn&checkOut&adults&children   — unified search
 *  GET /api/public/hotels/{ref}                                           — hotel page content
 *  GET /api/public/hotels/{ref}/availability?checkIn&checkOut&adults&children
 *  GET /api/public/hotels/partners/featured?limit=8                       — home page showcase
 * Inputs are validated strictly and searches are rate-limited per IP, because they can
 * trigger limited supplier API calls.
 */
@RestController
@RequestMapping("/api/public/hotels")
class HotelSearchController {

    /** Hotel page content. description/notes are sanitised HTML (text formatting only). */
    record HotelPage(String ref, String origin, String name, String city, Integer stars, String address,
                     List<String> images, String description, String notes, List<String> facilities,
                     List<String> tags, List<String> themes, List<String> boards, String checkIn,
                     String checkOut, String latitude, String longitude) {
    }

    record PartnerCard(String ref, String name, String city, Integer stars, String image) {
    }

    private final HotelSearchService search;
    private final CatalogCrud<Hotel, HotelDto> ownHotels;
    private final SupplierHotelRepository supplierHotels;
    private final HotelDetailService details;
    private final JdbcTemplate jdbc;
    private final SupplierRegistry suppliers;
    /** Searches can trigger supplier API calls: strict. Hotel pages may fetch HotelDetail: looser. */
    private final RateLimiter searchLimiter = new RateLimiter(20, Duration.ofMinutes(1), "Trop de recherches, réessayez dans une minute.");
    private final RateLimiter pageLimiter = new RateLimiter(60, Duration.ofMinutes(1), "Trop de requêtes, réessayez dans une minute.");

    HotelSearchController(HotelSearchService search, CatalogCrud<Hotel, HotelDto> ownHotels,
                          SupplierHotelRepository supplierHotels, HotelDetailService details, JdbcTemplate jdbc,
                          SupplierRegistry suppliers) {
        this.suppliers = suppliers;
        this.search = search;
        this.ownHotels = ownHotels;
        this.supplierHotels = supplierHotels;
        this.details = details;
        this.jdbc = jdbc;
    }

    @GetMapping("/search")
    HotelSearchService.SearchResponse search(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "") String children,
            HttpServletRequest request) {
        searchLimiter.check(request.getRemoteAddr());
        if (city.isBlank() || city.length() > 80) throw bad("Ville invalide.");
        return search.search(query(city.trim(), checkIn, checkOut, adults, children));
    }

    @GetMapping("/{ref}")
    HotelPage hotel(@PathVariable String ref, HttpServletRequest request) {
        pageLimiter.check(request.getRemoteAddr());
        if (ref.startsWith("a-")) {
            HotelDto h = ownHotel(ref);
            return new HotelPage(ref, "agence", h.name(), h.city(), h.stars(), null, List.of(h.image()), null, null,
                    h.tags(), List.of(), List.of(), List.of(h.board()), null, null, null, null);
        }
        SupplierHotel h = partnerHotel(ref);
        Optional<SupplierHotelDetail> d = details.forHotel(h);
        List<String> images = d.map(SupplierHotelDetail::getImages).filter(l -> !l.isEmpty())
                .orElse(h.getImage() == null ? List.of() : List.of(h.getImage()));
        return new HotelPage(ref, "partenaire", h.getName(), h.getCityName(), h.getStars(),
                d.map(SupplierHotelDetail::getAddress).orElse(null), images,
                d.map(SupplierHotelDetail::getDescription).orElse(null), d.map(SupplierHotelDetail::getNotes).orElse(null),
                d.map(SupplierHotelDetail::getFacilities).orElse(List.of()), d.map(SupplierHotelDetail::getTags).orElse(List.of()),
                d.map(SupplierHotelDetail::getThemes).orElse(List.of()), d.map(SupplierHotelDetail::getBoards).orElse(List.of()),
                d.map(SupplierHotelDetail::getCheckIn).orElse(null), d.map(SupplierHotelDetail::getCheckOut).orElse(null),
                d.map(SupplierHotelDetail::getLatitude).orElse(null), d.map(SupplierHotelDetail::getLongitude).orElse(null));
    }

    @GetMapping("/{ref}/availability")
    Optional<HotelSearchService.HotelResult> availability(
            @PathVariable String ref,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "") String children,
            HttpServletRequest request) {
        searchLimiter.check(request.getRemoteAddr());
        if (ref.startsWith("a-")) {
            HotelDto h = ownHotel(ref);
            return search.availabilityOwn(h, query(h.city(), checkIn, checkOut, adults, children));
        }
        SupplierHotel h = partnerHotel(ref);
        return search.availabilityPartner(h, query(h.getCityName(), checkIn, checkOut, adults, children));
    }

    /** A few partner hotels with photos, spread over several cities (no prices: "sur demande"). */
    @GetMapping("/partners/featured")
    List<PartnerCard> featuredPartners(@RequestParam(defaultValue = "8") int limit) {
        int n = Math.max(1, Math.min(limit, 24));
        String[] active = activeSupplierCodes();
        if (active.length == 0) return List.of(); // no sellable supplier → nothing to showcase
        return jdbc.query("""
                select id, name, city_name, stars, image from (
                  select h.*, row_number() over (partition by lower(city_name) order by stars desc nulls last, name) rn
                  from supplier_hotel h where active and image is not null and supplier_code = any(?)
                ) t where rn <= 2 order by stars desc nulls last, rn, city_name limit ?""",
                (rs, i) -> new PartnerCard("p-" + rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getString("city_name"), (Integer) rs.getObject("stars"), rs.getString("image")), active, n);
    }

    /** Suppliers that are configured AND enabled: only their hotels are shown publicly. */
    private String[] activeSupplierCodes() {
        return suppliers.activeForSearch().stream().map(HotelSupplier::code).toArray(String[]::new);
    }

    // ------------------------------------------------------------------ helpers

    private HotelDto ownHotel(String ref) {
        try {
            HotelDto h = ownHotels.get(UUID.fromString(ref.substring(2)));
            if (!h.published()) throw notFound();
            return h;
        } catch (IllegalArgumentException e) {
            throw notFound();
        }
    }

    private SupplierHotel partnerHotel(String ref) {
        if (!ref.startsWith("p-")) throw notFound();
        try {
            List<String> active = List.of(activeSupplierCodes());
            return supplierHotels.findById(UUID.fromString(ref.substring(2)))
                    .filter(SupplierHotel::isActive)
                    .filter(h -> active.contains(h.getSupplierCode())) // disabled supplier → hotel not shown
                    .orElseThrow(this::notFound);
        } catch (IllegalArgumentException e) {
            throw notFound();
        }
    }

    private static SearchQuery query(String city, LocalDate checkIn, LocalDate checkOut, int adults, String children) {
        if (checkIn.isBefore(LocalDate.now())) throw bad("La date d’arrivée est dans le passé.");
        if (!checkOut.isAfter(checkIn)) throw bad("La date de départ doit être après la date d’arrivée.");
        if (checkIn.until(checkOut).getDays() > 30 || checkIn.isAfter(LocalDate.now().plusYears(2))) throw bad("Séjour trop long ou trop lointain.");
        if (adults < 1 || adults > 6) throw bad("Entre 1 et 6 adultes.");
        return new SearchQuery(checkIn, checkOut, city, List.of(new Room(adults, parseAges(children))));
    }

    static List<Integer> parseAges(String children) {
        if (children == null || children.isBlank()) return List.of();
        try {
            List<Integer> ages = Arrays.stream(children.split(",")).map(String::trim).map(Integer::valueOf).toList();
            if (ages.size() > 4 || ages.stream().anyMatch(a -> a < 0 || a > 17)) throw bad("Jusqu’à 4 enfants de 0 à 17 ans.");
            return ages;
        } catch (NumberFormatException e) {
            throw bad("Âges des enfants invalides.");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Hôtel introuvable.");
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
