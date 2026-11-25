package tn.gezenthi.notification;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

/**
 * French e-mail templates (plain text + simple branded HTML). All dynamic values are
 * HTML-escaped. Kept in code (no template engine) because there are only a few messages.
 */
final class MessageTemplates {

    record Mail(String subject, String text, String html) {
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private MessageTemplates() {
    }

    /** To the customer, right after the request. */
    static Mail customerReceived(BookingMessage b, String agencyPhone) {
        String subject = "Votre demande de réservation " + b.reference() + " — Gezenthi Travel";
        String intro = "Nous avons bien reçu votre demande de réservation. Un conseiller Gezenthi vous contacte "
                + "rapidement pour confirmer la disponibilité et les modalités de paiement. Aucun paiement n’a été effectué.";
        return mail(subject, greeting(b), intro, b, "Une question ? Appelez-nous au " + agencyPhone
                + " en indiquant votre référence " + b.reference() + ".");
    }

    /** To the customer when the agency changes the status. Null if this status isn't worth a message. */
    static Mail customerStatus(BookingMessage b, String agencyPhone) {
        String subject;
        String intro;
        switch (b.status()) {
            case "CONFIRMED" -> {
                subject = "Réservation confirmée " + b.reference() + " — Gezenthi Travel";
                intro = "Bonne nouvelle : votre réservation est confirmée. Nous vous souhaitons un excellent séjour !";
            }
            case "CANCELLED" -> {
                subject = "Réservation annulée " + b.reference() + " — Gezenthi Travel";
                intro = "Votre réservation a été annulée. Pour toute question ou pour une autre proposition, contactez-nous.";
            }
            case "REJECTED" -> {
                subject = "Votre demande " + b.reference() + " — Gezenthi Travel";
                intro = "Nous sommes désolés : nous ne pouvons pas confirmer cette réservation (disponibilité ou tarif). "
                        + "Un conseiller peut vous proposer une alternative.";
            }
            default -> {
                return null;
            }
        }
        return mail(subject, greeting(b), intro, b, "Votre conseiller : " + agencyPhone + " · référence " + b.reference() + ".");
    }

    /** To the agency inbox: new request alert (contains customer contact — internal only). */
    static Mail agencyNew(BookingMessage b, String adminUrl) {
        String subject = "Nouvelle demande " + b.reference() + " — " + b.hotelName() + " (" + b.city() + ")";
        String intro = "Nouvelle demande de réservation reçue sur le site. Client : " + civ(b.customerCivility()) + " "
                + b.customerFirstName() + " " + b.customerLastName() + " · " + b.customerEmail() + " · " + b.customerPhone()
                + (b.whatsappOptIn() ? " · accepte WhatsApp" : "") + ".";
        return mail(subject, "Bonjour,", intro, b, "Traiter la demande : " + adminUrl);
    }

    // ------------------------------------------------------------------ rendering

    private static Mail mail(String subject, String greeting, String intro, BookingMessage b, String footer) {
        String[][] rows = {
                {"Référence", b.reference()},
                {"Hôtel", b.hotelName() + " — " + b.city()},
                {"Dates", DATE.format(b.checkIn()) + " → " + DATE.format(b.checkOut()) + " (" + b.nights() + " nuit(s))"},
                {"Voyageurs", b.adults() + " adulte(s)" + (b.children() > 0 ? ", " + b.children() + " enfant(s)" : "")},
                {"Chambre", nz(b.roomName()) + (b.boardName() == null ? "" : " · " + b.boardName())},
                {"Total séjour", money(b)},
        };
        StringBuilder text = new StringBuilder(greeting).append("\n\n").append(intro).append("\n\n");
        for (String[] r : rows) text.append(r[0]).append(" : ").append(r[1]).append('\n');
        text.append('\n').append(footer).append("\n\n— Gezenthi Travel Agency");

        StringBuilder html = new StringBuilder("""
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:auto;color:#0c3459">
                  <div style="background:#156AB3;color:#fff;padding:18px 24px;border-radius:12px 12px 0 0;font-size:20px;font-weight:bold">
                    Gezenthi <span style="color:#FFC523">Travel</span>
                  </div>
                  <div style="border:1px solid #e2e8f0;border-top:0;padding:24px;border-radius:0 0 12px 12px">
                """);
        html.append("<p>").append(esc(greeting)).append("</p><p>").append(esc(intro)).append("</p>");
        html.append("<table style=\"width:100%;border-collapse:collapse;margin:16px 0\">");
        for (String[] r : rows) {
            html.append("<tr><td style=\"padding:6px 0;color:#64748b\">").append(esc(r[0]))
                    .append("</td><td style=\"padding:6px 0;font-weight:bold;text-align:right\">").append(esc(r[1])).append("</td></tr>");
        }
        html.append("</table><p style=\"color:#475569\">").append(esc(footer))
                .append("</p><p style=\"color:#94a3b8;font-size:12px\">Gezenthi Travel Agency · Nabeul, Tunisie</p></div></div>");
        return new Mail(subject, text.toString(), html.toString());
    }

    private static String greeting(BookingMessage b) {
        return "Bonjour " + civ(b.customerCivility()) + " " + b.customerLastName() + ",";
    }

    static String civ(String c) {
        return switch (c == null ? "" : c) {
            case "Mr" -> "M.";
            case "Ms" -> "Mlle";
            case "Mrs" -> "Mme";
            default -> "";
        };
    }

    static String money(BookingMessage b) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRENCH);
        nf.setMaximumFractionDigits(0);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return nf.format(b.totalPrice()) + " " + ("TND".equals(b.currency()) ? "DT" : b.currency());
    }

    static String dates(BookingMessage b) {
        return DATE.format(b.checkIn()) + " → " + DATE.format(b.checkOut());
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s == null ? "" : s);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
