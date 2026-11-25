package tn.gezenthi.booking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Booking endpoints: public (offer summary + request) and admin (workflow + supplier actions). */
final class BookingControllers {

    private BookingControllers() {
    }

    @RestController
    @RequestMapping("/api/public")
    static class PublicBookingController {

        private static final int MAX_REQUESTS_PER_10_MIN = 5;
        private final BookingService service;
        private final Map<String, List<Instant>> hits = new ConcurrentHashMap<>();

        PublicBookingController(BookingService service) {
            this.service = service;
        }

        @GetMapping("/offers/{offerId}")
        BookingDtos.OfferSummary offer(@PathVariable String offerId, @RequestParam(defaultValue = "0") int board,
                                       @RequestParam(defaultValue = "0") int room) {
            return service.summary(offerId, board, room);
        }

        @PostMapping("/bookings")
        @ResponseStatus(HttpStatus.CREATED)
        BookingDtos.Created create(@Valid @RequestBody BookingDtos.CreateBooking body, HttpServletRequest request) {
            rateLimit(request.getRemoteAddr());
            return service.create(body);
        }

        /** Anti-spam: a few booking requests per IP per 10 minutes. */
        private void rateLimit(String ip) {
            Instant cutoff = Instant.now().minusSeconds(600);
            List<Instant> recent = hits.compute(ip, (k, l) -> {
                List<Instant> list = l == null ? new ArrayList<>() : new ArrayList<>(l);
                list.removeIf(t -> t.isBefore(cutoff));
                list.add(Instant.now());
                return list;
            });
            if (recent.size() > MAX_REQUESTS_PER_10_MIN) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Trop de demandes. Appelez-nous directement.");
            }
        }
    }

    @RestController
    @RequestMapping("/api/admin/bookings")
    static class AdminBookingController {

        private final BookingService service;

        AdminBookingController(BookingService service) {
            this.service = service;
        }

        @GetMapping
        List<BookingDtos.BookingSummary> list(@RequestParam(required = false) BookingRequest.Status status) {
            return service.list(status);
        }

        @GetMapping("/{id}")
        BookingDtos.BookingDetail detail(@PathVariable UUID id) {
            return service.detail(id);
        }

        @PostMapping("/{id}/status")
        BookingDtos.BookingDetail status(@PathVariable UUID id, @Valid @RequestBody BookingDtos.StatusChange body, Authentication auth) {
            return service.changeStatus(id, body.status(), body.note(), auth.getName());
        }

        @PostMapping("/{id}/notes")
        BookingDtos.BookingDetail note(@PathVariable UUID id, @Valid @RequestBody BookingDtos.Note body, Authentication auth) {
            return service.addNote(id, body.note(), auth.getName());
        }

        /** Pre-booking at the supplier (verification only, nothing is booked). */
        @PostMapping("/{id}/supplier/check")
        BookingDtos.SupplierCheckResult check(@PathVariable UUID id, Authentication auth) {
            return service.supplierCheck(id, auth.getName());
        }

        /** Real supplier booking — refused while booking-enabled=false. */
        @PostMapping("/{id}/supplier/book")
        BookingDtos.BookingDetail book(@PathVariable UUID id, Authentication auth) {
            return service.supplierBook(id, auth.getName());
        }
    }
}
