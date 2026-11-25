package tn.gezenthi.booking;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.gezenthi.booking.BookingDtos.BookingDetail;
import tn.gezenthi.booking.BookingDtos.BookingSummary;
import tn.gezenthi.booking.BookingDtos.CreateBooking;
import tn.gezenthi.booking.BookingDtos.Created;
import tn.gezenthi.booking.BookingDtos.EventOut;
import tn.gezenthi.booking.BookingDtos.OfferSummary;
import tn.gezenthi.booking.BookingDtos.PassengerIn;
import tn.gezenthi.booking.BookingDtos.PassengerOut;
import tn.gezenthi.booking.BookingDtos.SupplierCheckResult;
import tn.gezenthi.search.HotelSearchService.BoardResult;
import tn.gezenthi.search.HotelSearchService.HotelResult;
import tn.gezenthi.search.HotelSearchService.RoomResult;
import tn.gezenthi.notification.NotificationService;
import tn.gezenthi.search.OfferRefCache;
import tn.gezenthi.supplier.HotelSupplier;
import tn.gezenthi.supplier.SupplierException;
import tn.gezenthi.supplier.SupplierProperties;
import tn.gezenthi.supplier.SupplierRegistry;

/**
 * Booking requests: creation from a server-side offer, admin workflow, supplier verification.
 *
 * Rules:
 *  - The browser only sends offerId + room choice; price, hotel and dates come from OfferRefCache.
 *  - Every change is recorded in booking_event (actor, action) — no personal data in the details.
 *  - Supplier "check" = pre-booking (verify only). Supplier "book" = real reservation, refused
 *    unless booking-enabled=true for that supplier (also enforced inside the adapter).
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String WEBSITE = "client (site web)";

    private final BookingRepository bookings;
    private final OfferRefCache offers;
    private final SupplierRegistry suppliers;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;

    BookingService(BookingRepository bookings, OfferRefCache offers, SupplierRegistry suppliers, JdbcTemplate jdbc,
                   ApplicationEventPublisher events, NotificationService notifications) {
        this.events = events;
        this.notifications = notifications;
        this.bookings = bookings;
        this.offers = offers;
        this.suppliers = suppliers;
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ public

    public OfferSummary summary(String offerId, int board, int room) {
        OfferRefCache.OfferRef ref = offer(offerId);
        HotelResult h = ref.result();
        BoardResult b = pick(h.boards(), board);
        RoomResult r = pick(b.rooms(), room);
        HotelSupplier.SearchQuery q = ref.query();
        return new OfferSummary(h.name(), h.city(), h.stars(), h.image(), q.checkIn(), q.checkOut(), q.nights(),
                q.rooms().getFirst().adults(), q.rooms().getFirst().childAges(), b.name(), r.name(),
                r.totalPrice(), h.currency(), r.refundable(), r.cancellationDeadline(), r.available());
    }

    @Transactional
    public Created create(CreateBooking in) {
        OfferRefCache.OfferRef ref = offer(in.offerId());
        HotelResult h = ref.result();
        BoardResult b = pick(h.boards(), in.board());
        RoomResult r = pick(b.rooms(), in.room());
        if (!r.available()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette chambre n’est plus disponible en ligne.");

        HotelSupplier.SearchQuery q = ref.query();
        int adults = q.rooms().getFirst().adults();
        List<Integer> ages = q.rooms().getFirst().childAges();
        List<PassengerIn> adultsIn = in.passengers().stream().filter(p -> "ADULT".equals(p.type())).toList();
        List<PassengerIn> childrenIn = in.passengers().stream().filter(p -> "CHILD".equals(p.type())).toList();
        if (adultsIn.size() != adults || childrenIn.size() != ages.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indiquez " + adults + " adulte(s) et " + ages.size() + " enfant(s), comme dans votre recherche.");
        }
        if (adultsIn.stream().anyMatch(p -> p.civility() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La civilité est obligatoire pour chaque adulte.");
        }

        BookingRequest bk = new BookingRequest();
        bk.reference = nextReference();
        bk.origin = h.origin();
        bk.hotelRef = h.ref();
        bk.hotelName = h.name();
        bk.city = h.city();
        bk.stars = h.stars();
        bk.supplierCode = ref.supplierCode();
        bk.supplierHotelId = ref.externalHotelId();
        bk.supplierCityId = ref.cityExternalId();
        bk.checkIn = q.checkIn();
        bk.checkOut = q.checkOut();
        bk.adults = adults;
        bk.childAges = new ArrayList<>(ages);
        bk.boardId = b.id();
        bk.boardName = b.name();
        bk.roomId = r.id();
        bk.roomName = r.name();
        bk.totalPrice = r.totalPrice();
        bk.currency = h.currency();
        bk.refundable = r.refundable();
        bk.cancellationDeadline = r.cancellationDeadline();
        bk.customerCivility = in.customer().civility();
        bk.customerFirstName = in.customer().firstName().trim();
        bk.customerLastName = in.customer().lastName().trim();
        bk.customerEmail = in.customer().email().trim().toLowerCase(Locale.ROOT);
        bk.customerPhone = in.customer().phone().trim();
        bk.comment = in.comment() == null || in.comment().isBlank() ? null : in.comment().trim();
        bk.consentAt = Instant.now();
        bk.whatsappOptIn = Boolean.TRUE.equals(in.whatsappOptIn());
        for (PassengerIn p : adultsIn) bk.addPassenger(passenger(p, null));
        for (int i = 0; i < childrenIn.size(); i++) bk.addPassenger(passenger(childrenIn.get(i), ages.get(i)));
        bk.log(WEBSITE, "CREATED", "Prix affiché " + r.totalPrice() + " " + h.currency());
        bookings.save(bk);
        events.publishEvent(new NotificationService.BookingCreated(bk.id)); // sent after commit
        log.info("Booking request {} created ({}, {})", bk.reference, bk.origin, bk.city); // no personal data
        return new Created(bk.reference, bk.status.name());
    }

    // ------------------------------------------------------------------ admin

    @Transactional(readOnly = true)
    public List<BookingSummary> list(BookingRequest.Status status) {
        List<BookingRequest> rows = status == null ? bookings.findAllByOrderByCreatedAtDesc() : bookings.findByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(b -> new BookingSummary(b.id, b.reference, b.status.name(), b.origin, b.hotelName, b.city,
                b.checkIn, b.checkOut, b.customerFirstName + " " + b.customerLastName, b.totalPrice, b.currency, b.createdAt)).toList();
    }

    @Transactional(readOnly = true)
    public BookingDetail detail(UUID id) {
        return toDetail(load(id));
    }

    @Transactional
    public BookingDetail changeStatus(UUID id, BookingRequest.Status status, String note, String actor) {
        BookingRequest b = load(id);
        BookingRequest.Status from = b.status;
        b.status = status;
        b.log(actor, "STATUS", from + " → " + status + (note == null || note.isBlank() ? "" : " — " + note.trim()));
        if (from != status) events.publishEvent(new NotificationService.BookingStatusChanged(b.id, status.name()));
        return toDetail(b);
    }

    @Transactional
    public BookingDetail addNote(UUID id, String note, String actor) {
        BookingRequest b = load(id);
        b.log(actor, "NOTE", note.trim());
        return toDetail(b);
    }

    /** Pre-booking at the supplier: verifies price/availability, books NOTHING. */
    @Transactional
    public SupplierCheckResult supplierCheck(UUID id, String actor) {
        BookingRequest b = load(id);
        HotelSupplier supplier = supplierOf(b);
        HotelSupplier.BookingSpec spec = freshSpec(b, supplier);
        if (spec == null) {
            b.log(actor, "SUPPLIER_CHECK", "Tarif introuvable chez le fournisseur pour ces dates / cette chambre.");
            return new SupplierCheckResult(false, null, null, false, null,
                    "Le fournisseur ne propose plus cette chambre pour ces dates. Refaites une recherche avec le client.");
        }
        try {
            HotelSupplier.BookingCheck check = supplier.preBook(spec);
            b.supplierPrice = check.price();
            if (check.available()) b.status = BookingRequest.Status.PRICE_CHECKED;
            b.log(actor, "SUPPLIER_CHECK", (check.available() ? "Disponible" : "Indisponible")
                    + (check.price() == null ? "" : " — prix fournisseur " + check.price() + " " + nz(check.currency()))
                    + (check.onRequest() ? " — sur demande" : ""));
            return new SupplierCheckResult(check.available(), check.price(), check.currency(), check.onRequest(),
                    b.totalPrice, check.message());
        } catch (SupplierException e) {
            b.log(actor, "SUPPLIER_CHECK", "Échec : " + e.getMessage());
            return new SupplierCheckResult(false, null, null, false, b.totalPrice, e.getMessage());
        }
    }

    /** REAL booking at the supplier. Refused while booking-enabled=false (the refusal itself is recorded). */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public BookingDetail supplierBook(UUID id, String actor) {
        BookingRequest b = load(id);
        HotelSupplier supplier = supplierOf(b);
        SupplierProperties.Supplier settings = suppliers.settings(supplier.code());
        if (settings == null || !settings.isBookingEnabled()) {
            b.log(actor, "SUPPLIER_BOOK_REFUSED", "Réservation fournisseur désactivée (booking-enabled=false).");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La réservation automatique chez le fournisseur est désactivée. Réservez par téléphone/e-mail, puis confirmez la demande ici.");
        }
        HotelSupplier.BookingSpec spec = freshSpec(b, supplier);
        if (spec == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tarif introuvable chez le fournisseur pour ces dates.");
        HotelSupplier.BookingConfirmation c = supplier.book(spec);
        b.supplierBookingId = c.bookingId();
        b.supplierState = c.state();
        b.voucherUrl = c.voucherUrl();
        if (c.price() != null) b.supplierPrice = c.price();
        b.status = BookingRequest.Status.CONFIRMED;
        events.publishEvent(new NotificationService.BookingStatusChanged(b.id, b.status.name()));
        b.log(actor, "SUPPLIER_BOOKED", "Réservation fournisseur n° " + c.bookingId() + " (" + nz(c.state()) + ")");
        log.info("Supplier booking {} for request {} by {}", c.bookingId(), b.reference, actor);
        return toDetail(b);
    }

    // ------------------------------------------------------------------ internals

    private OfferRefCache.OfferRef offer(String offerId) {
        return offers.get(offerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.GONE,
                "Cette offre a expiré (les prix sont valables 30 minutes). Merci de relancer la recherche."));
    }

    private static <T> T pick(List<T> list, int index) {
        if (index < 0 || index >= list.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chambre invalide.");
        return list.get(index);
    }

    private static BookingPassenger passenger(PassengerIn p, Integer age) {
        BookingPassenger out = new BookingPassenger();
        out.type = p.type();
        out.civility = p.civility();
        out.firstName = p.firstName().trim();
        out.lastName = p.lastName().trim();
        out.age = age;
        return out;
    }

    private String nextReference() {
        Long n = jdbc.queryForObject("select nextval('booking_ref_seq')", Long.class);
        return "GZ-" + Year.now().getValue() + "-" + String.format("%06d", n);
    }

    private BookingRequest load(UUID id) {
        return bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable."));
    }

    private HotelSupplier supplierOf(BookingRequest b) {
        if (!"partenaire".equals(b.origin) || b.supplierCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette réservation ne concerne pas un hôtel partenaire.");
        }
        return suppliers.find(b.supplierCode).filter(HotelSupplier::configured)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Fournisseur non configuré."));
    }

    /** Supplier tokens expire: re-search the hotel now and find the same board/room to get a fresh token. */
    private HotelSupplier.BookingSpec freshSpec(BookingRequest b, HotelSupplier supplier) {
        HotelSupplier.SearchQuery q = new HotelSupplier.SearchQuery(b.checkIn, b.checkOut, b.city,
                List.of(new HotelSupplier.Room(b.adults, List.copyOf(b.childAges))));
        for (HotelSupplier.Offer o : supplier.search(q, List.of(b.supplierHotelId))) {
            for (HotelSupplier.BoardRate board : o.boards()) {
                if (!board.id().equals(b.boardId)) continue;
                for (HotelSupplier.RoomRate room : board.rooms()) {
                    if (!room.id().equals(b.roomId) || !room.bookable()) continue;
                    List<HotelSupplier.Passenger> adults = new ArrayList<>();
                    List<HotelSupplier.Passenger> children = new ArrayList<>();
                    boolean holder = true;
                    for (BookingPassenger p : b.passengers) {
                        if ("ADULT".equals(p.type)) {
                            adults.add(new HotelSupplier.Passenger(p.civility, p.firstName, p.lastName, holder, null));
                            holder = false;
                        } else {
                            children.add(new HotelSupplier.Passenger(null, p.firstName, p.lastName, false, p.age));
                        }
                    }
                    return new HotelSupplier.BookingSpec(b.supplierCityId, b.supplierHotelId, b.checkIn, b.checkOut,
                            o.source(), o.token(), b.roomId, b.boardId, adults, children, "Gezenthi " + b.reference);
                }
            }
        }
        return null;
    }

    private BookingDetail toDetail(BookingRequest b) {
        boolean enabled = b.supplierCode != null && suppliers.settings(b.supplierCode) != null
                && suppliers.settings(b.supplierCode).isBookingEnabled();
        String supplierName = b.supplierCode == null ? null : suppliers.find(b.supplierCode).map(HotelSupplier::displayName).orElse(b.supplierCode);
        return new BookingDetail(b.id, b.reference, b.status.name(), b.origin, b.hotelRef, b.hotelName, b.city, b.stars,
                b.checkIn, b.checkOut, b.adults, List.copyOf(b.childAges), b.boardName, b.roomName, b.totalPrice, b.currency,
                b.refundable, b.cancellationDeadline, b.customerCivility, b.customerFirstName, b.customerLastName,
                b.customerEmail, b.customerPhone, b.comment, b.consentAt, supplierName, b.supplierBookingId,
                b.supplierState, b.supplierPrice, b.voucherUrl, enabled,
                b.passengers.stream().map(p -> new PassengerOut(p.position, p.type, p.civility, p.firstName, p.lastName, p.age)).toList(),
                b.events.stream().map(e -> new EventOut(e.at, e.actor, e.action, e.details)).toList(),
                b.createdAt, b.whatsappOptIn, notifications.history(b.id), notifications.status());
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
