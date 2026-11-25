package tn.gezenthi.notification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Booking notifications (e-mail + WhatsApp), sent AFTER the booking is committed, on a
 * background thread: a slow or failing mail server never blocks or breaks a booking.
 * Every attempt is written to notification_log with a MASKED recipient (no message bodies).
 */
@Service
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationService {

    /** Published by the booking module. */
    public record BookingCreated(UUID bookingId) {
    }

    public record BookingStatusChanged(UUID bookingId, String status) {
    }

    public record LogEntry(String channel, String audience, String template, String recipient, String status,
                           String error, java.time.Instant at) {
    }

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationProperties props;
    private final EmailSender email;
    private final WhatsAppSender whatsapp;
    private final JdbcTemplate jdbc;

    NotificationService(NotificationProperties props, EmailSender email, WhatsAppSender whatsapp, JdbcTemplate jdbc) {
        this.props = props;
        this.email = email;
        this.whatsapp = whatsapp;
        this.jdbc = jdbc;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onCreated(BookingCreated e) {
        Thread.ofVirtual().name("notify-" + e.bookingId()).start(() -> {
            BookingMessage b = load(e.bookingId());
            if (b == null) return;
            sendEmail(b, "CUSTOMER", "BOOKING_RECEIVED", b.customerEmail(), MessageTemplates.customerReceived(b, props.getAgencyPhone()));
            if (b.whatsappOptIn()) sendWhatsApp(b);
            String adminUrl = props.getSiteUrl().replaceAll("/+$", "") + "/admin/reservations/" + b.bookingId();
            if (props.getAgencyEmail().isBlank()) {
                record(b, "EMAIL", "AGENCY", "AGENCY_NEW_BOOKING", "(aucune)", "SKIPPED", "Adresse de l’agence non configurée");
            } else {
                sendEmail(b, "AGENCY", "AGENCY_NEW_BOOKING", props.getAgencyEmail(), MessageTemplates.agencyNew(b, adminUrl));
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onStatusChanged(BookingStatusChanged e) {
        if (!List.of("CONFIRMED", "CANCELLED", "REJECTED").contains(e.status())) return;
        Thread.ofVirtual().name("notify-status-" + e.bookingId()).start(() -> {
            BookingMessage b = load(e.bookingId());
            if (b == null) return;
            MessageTemplates.Mail mail = MessageTemplates.customerStatus(b, props.getAgencyPhone());
            if (mail != null) sendEmail(b, "CUSTOMER", "STATUS_" + e.status(), b.customerEmail(), mail);
        });
    }

    public List<LogEntry> history(UUID bookingId) {
        return jdbc.query("select channel, audience, template, recipient, status, error, created_at from notification_log "
                        + "where booking_id = ? order by created_at",
                (rs, i) -> new LogEntry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getTimestamp(7).toInstant()), bookingId);
    }

    /** Status of each channel for the admin (no secrets). */
    public record ChannelStatus(boolean emailReady, boolean whatsappReady, boolean agencyEmailSet) {
    }

    public ChannelStatus status() {
        return new ChannelStatus(email.ready(), whatsapp.ready(), !props.getAgencyEmail().isBlank());
    }

    // ------------------------------------------------------------------ internals

    private void sendEmail(BookingMessage b, String audience, String template, String to, MessageTemplates.Mail mail) {
        String masked = maskEmail(to);
        if (!email.ready()) {
            record(b, "EMAIL", audience, template, masked, "SKIPPED", "E-mail non configuré (SMTP)");
            return;
        }
        try {
            email.send(to, mail);
            record(b, "EMAIL", audience, template, masked, "SENT", null);
        } catch (Exception ex) {
            log.warn("E-mail {} for {} failed: {}", template, b.reference(), ex.getClass().getSimpleName());
            record(b, "EMAIL", audience, template, masked, "FAILED", ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()));
        }
    }

    private void sendWhatsApp(BookingMessage b) {
        String to = WhatsAppSender.toE164Digits(b.customerPhone(), props.getWhatsapp().getDefaultCountryCode());
        String masked = maskPhone(to == null ? b.customerPhone() : to);
        if (!whatsapp.ready()) {
            record(b, "WHATSAPP", "CUSTOMER", "BOOKING_RECEIVED", masked, "SKIPPED", "WhatsApp API non configurée");
            return;
        }
        if (to == null) {
            record(b, "WHATSAPP", "CUSTOMER", "BOOKING_RECEIVED", masked, "FAILED", "Numéro de téléphone invalide");
            return;
        }
        try {
            whatsapp.sendTemplate(to, List.of(b.customerFirstName(), b.reference(), b.hotelName(), MessageTemplates.dates(b)));
            record(b, "WHATSAPP", "CUSTOMER", "BOOKING_RECEIVED", masked, "SENT", null);
        } catch (Exception ex) {
            log.warn("WhatsApp for {} failed: {}", b.reference(), ex.getClass().getSimpleName());
            record(b, "WHATSAPP", "CUSTOMER", "BOOKING_RECEIVED", masked, "FAILED", ex.getClass().getSimpleName() + ": " + safe(ex.getMessage()));
        }
    }

    private void record(BookingMessage b, String channel, String audience, String template, String recipient, String status, String error) {
        jdbc.update("insert into notification_log (id, booking_id, channel, audience, template, recipient, status, error) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), b.bookingId(), channel, audience, template, recipient, status,
                error == null ? null : error.substring(0, Math.min(500, error.length())));
    }

    private BookingMessage load(UUID id) {
        List<BookingMessage> rows = jdbc.query("""
                select id, reference, status, hotel_name, city, check_in, check_out, adults, cardinality(child_ages),
                       room_name, board_name, total_price, currency, customer_civility, customer_first_name,
                       customer_last_name, customer_email, customer_phone, whatsapp_opt_in
                from booking_request where id = ?""",
                (rs, i) -> new BookingMessage(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getInt(8),
                        rs.getInt(9), rs.getString(10), rs.getString(11), rs.getBigDecimal(12), rs.getString(13), rs.getString(14),
                        rs.getString(15), rs.getString(16), rs.getString(17), rs.getString(18), rs.getBoolean(19)), id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** amira@example.tn → a***@example.tn */
    static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    /** 21620123456 → ******456 */
    static String maskPhone(String phone) {
        if (phone == null) return "***";
        String d = phone.replaceAll("\\D", "");
        return d.length() <= 3 ? "***" : "*".repeat(Math.min(8, d.length() - 3)) + d.substring(d.length() - 3);
    }

    /** Error texts may echo addresses: keep them short and drop anything after an '@' local part. */
    private static String safe(String s) {
        if (s == null) return "";
        String cleaned = s.replaceAll("[\\w.+-]+@[\\w.-]+", "<adresse>");
        return cleaned.substring(0, Math.min(300, cleaned.length()));
    }
}
