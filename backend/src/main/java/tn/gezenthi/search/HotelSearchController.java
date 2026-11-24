package tn.gezenthi.search;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.gezenthi.supplier.HotelSupplier.Room;
import tn.gezenthi.supplier.HotelSupplier.SearchQuery;

/**
 * Public hotel search: GET /api/public/hotels/search?city=Hammamet&checkIn=2027-07-01&checkOut=2027-07-08&adults=2&children=8,12
 * Inputs are validated strictly and the endpoint is rate-limited per IP, because each search
 * can trigger paid/limited supplier API calls.
 */
@RestController
@RequestMapping("/api/public/hotels")
class HotelSearchController {

    private static final int MAX_SEARCHES_PER_MINUTE = 20;

    private final HotelSearchService search;
    private final Map<String, List<Instant>> hits = new ConcurrentHashMap<>();

    HotelSearchController(HotelSearchService search) {
        this.search = search;
    }

    @GetMapping("/search")
    HotelSearchService.SearchResponse search(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "2") int adults,
            @RequestParam(defaultValue = "") String children,
            HttpServletRequest request) {

        rateLimit(request.getRemoteAddr());
        if (city.isBlank() || city.length() > 80) throw bad("Ville invalide.");
        if (checkIn.isBefore(LocalDate.now())) throw bad("La date d’arrivée est dans le passé.");
        if (!checkOut.isAfter(checkIn)) throw bad("La date de départ doit être après la date d’arrivée.");
        if (checkIn.until(checkOut).getDays() > 30 || checkIn.isAfter(LocalDate.now().plusYears(2))) throw bad("Séjour trop long ou trop lointain.");
        if (adults < 1 || adults > 6) throw bad("Entre 1 et 6 adultes.");
        List<Integer> ages = parseAges(children);

        return search.search(new SearchQuery(checkIn, checkOut, city.trim(), List.of(new Room(adults, ages))));
    }

    private static List<Integer> parseAges(String children) {
        if (children.isBlank()) return List.of();
        try {
            List<Integer> ages = Arrays.stream(children.split(",")).map(String::trim).map(Integer::valueOf).toList();
            if (ages.size() > 4 || ages.stream().anyMatch(a -> a < 0 || a > 17)) throw bad("Jusqu’à 4 enfants de 0 à 17 ans.");
            return ages;
        } catch (NumberFormatException e) {
            throw bad("Âges des enfants invalides.");
        }
    }

    private void rateLimit(String ip) {
        Instant cutoff = Instant.now().minusSeconds(60);
        List<Instant> recent = hits.compute(ip, (k, list) -> {
            List<Instant> l = list == null ? new ArrayList<>() : new ArrayList<>(list);
            l.removeIf(t -> t.isBefore(cutoff));
            l.add(Instant.now());
            return l;
        });
        if (recent.size() > MAX_SEARCHES_PER_MINUTE) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de recherches, réessayez dans une minute.");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
