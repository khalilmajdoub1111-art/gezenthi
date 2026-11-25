package tn.gezenthi.inquiry;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
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
import tn.gezenthi.inquiry.InquiryDtos.CreateInquiry;
import tn.gezenthi.inquiry.InquiryDtos.EventOut;
import tn.gezenthi.inquiry.InquiryDtos.InquiryDetail;
import tn.gezenthi.inquiry.InquiryDtos.InquirySummary;
import tn.gezenthi.notification.NotificationService;

/**
 * Quote requests: anything the customer can't book online (trips, Omra, visa, flights, CTN,
 * partner hotels without a live rate). Same rules as bookings: consent, rate limit, history
 * (who/when/what), notifications after commit, no personal data in logs.
 */
@Service
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final String WEBSITE = "client (site web)";

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final NotificationService notifications;

    InquiryService(JdbcTemplate jdbc, ApplicationEventPublisher events, NotificationService notifications) {
        this.jdbc = jdbc;
        this.events = events;
        this.notifications = notifications;
    }

    @Transactional
    public InquiryDtos.Created create(CreateInquiry in) {
        LocalDate from = in.dateFrom();
        LocalDate to = in.dateTo();
        if (from != null && from.isBefore(LocalDate.now())) throw bad("La date de départ est dans le passé.");
        if (from != null && to != null && to.isBefore(from)) throw bad("La date de retour doit être après la date de départ.");
        if (from != null && from.isAfter(LocalDate.now().plusYears(2))) throw bad("Date trop lointaine.");

        UUID id = UUID.randomUUID();
        Long n = jdbc.queryForObject("select nextval('inquiry_ref_seq')", Long.class);
        String reference = "DV-" + Year.now().getValue() + "-" + String.format("%06d", n);
        var c = in.customer();
        jdbc.update("""
                insert into inquiry (id, reference, status, type, subject, offer_ref, date_from, date_to, adults, children,
                  message, customer_civility, customer_first_name, customer_last_name, customer_email, customer_phone,
                  whatsapp_opt_in, consent_at)
                values (?, ?, 'NEW', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now())""",
                id, reference, in.type(), in.subject().trim(), blankToNull(in.offerRef()), from, to, in.adults(),
                in.children() == null ? 0 : in.children(), blankToNull(in.message()), c.civility(), c.firstName().trim(),
                c.lastName().trim(), c.email().trim().toLowerCase(Locale.ROOT), c.phone().trim(),
                Boolean.TRUE.equals(in.whatsappOptIn()));
        event(id, WEBSITE, "CREATED", in.type() + " — " + in.subject().trim());
        events.publishEvent(new NotificationService.InquiryCreated(id));
        log.info("Quote request {} created ({})", reference, in.type()); // no personal data
        return new InquiryDtos.Created(reference);
    }

    public List<InquirySummary> list(InquiryDtos.Status status) {
        String where = status == null ? "" : " where status = ?";
        Object[] args = status == null ? new Object[0] : new Object[] {status.name()};
        return jdbc.query("select id, reference, status, type, subject, date_from, date_to, adults, children, "
                        + "customer_first_name || ' ' || customer_last_name, created_at from inquiry" + where + " order by created_at desc",
                (rs, i) -> new InquirySummary(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getInt(8),
                        rs.getInt(9), rs.getString(10), rs.getTimestamp(11).toInstant()), args);
    }

    public InquiryDetail detail(UUID id) {
        List<InquiryDetail> rows = jdbc.query("""
                select id, reference, status, type, subject, offer_ref, date_from, date_to, adults, children, message,
                       customer_civility, customer_first_name, customer_last_name, customer_email, customer_phone,
                       whatsapp_opt_in, consent_at, created_at
                from inquiry where id = ?""",
                (rs, i) -> new InquiryDetail(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class), rs.getObject(8, LocalDate.class),
                        rs.getInt(9), rs.getInt(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14),
                        rs.getString(15), rs.getString(16), rs.getBoolean(17), rs.getTimestamp(18).toInstant(),
                        rs.getTimestamp(19).toInstant(), events(id), notifications.historyForInquiry(id), notifications.status()),
                id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable.");
        return rows.getFirst();
    }

    @Transactional
    public InquiryDetail changeStatus(UUID id, InquiryDtos.Status status, String note, String actor) {
        String from = detail(id).status();
        jdbc.update("update inquiry set status = ?, updated_at = now() where id = ?", status.name(), id);
        event(id, actor, "STATUS", from + " → " + status + (note == null || note.isBlank() ? "" : " — " + note.trim()));
        return detail(id);
    }

    @Transactional
    public InquiryDetail addNote(UUID id, String note, String actor) {
        detail(id);
        event(id, actor, "NOTE", note.trim());
        return detail(id);
    }

    private List<EventOut> events(UUID id) {
        return jdbc.query("select at, actor, action, details from inquiry_event where inquiry_id = ? order by at",
                (rs, i) -> new EventOut(rs.getTimestamp(1).toInstant(), rs.getString(2), rs.getString(3), rs.getString(4)), id);
    }

    private void event(UUID id, String actor, String action, String details) {
        jdbc.update("insert into inquiry_event (id, inquiry_id, at, actor, action, details) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), id, java.sql.Timestamp.from(Instant.now()), actor, action,
                details == null ? null : details.substring(0, Math.min(1000, details.length())));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}
