package tn.gezenthi.supplier.ostravel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tn.gezenthi.supplier.SupplierRateLimitException;
import tn.gezenthi.supplier.HotelSupplier.BoardRate;
import tn.gezenthi.supplier.HotelSupplier.BookingCheck;
import tn.gezenthi.supplier.HotelSupplier.BookingConfirmation;
import tn.gezenthi.supplier.HotelSupplier.HotelDetails;
import tn.gezenthi.supplier.HotelSupplier.Offer;
import tn.gezenthi.supplier.HotelSupplier.RoomRate;
import tn.gezenthi.supplier.HotelSupplier.StaticHotel;
import tn.gezenthi.supplier.SupplierException;
import tools.jackson.databind.JsonNode;

/**
 * Converts OS-TRAVEL Hotel API v2 JSON into the normalised supplier model.
 * Written defensively: the API mixes types (Star "4" vs 4, prices as strings),
 * so every field is read leniently and missing values become null.
 */
final class OsTravelParser {

    private OsTravelParser() {
    }

    private static final Pattern RETRY_AFTER = Pattern.compile("Retry after (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})");
    private static final ZoneId SUPPLIER_ZONE = ZoneId.of("Africa/Tunis");

    /** Throws if the standard error envelope reports a failure (rate limits get a dedicated exception). */
    static void checkError(String supplierName, JsonNode root) {
        JsonNode err = root.path("ErrorMessage");
        if (err.isObject() && !text(err, "Description").isBlank()) {
            String description = text(err, "Description");
            String message = supplierName + " : " + description + " (code " + text(err, "Code") + ")";
            if ("429".equals(text(err, "Code"))) {
                throw new SupplierRateLimitException(message, retryAfter(description));
            }
            throw new SupplierException(message);
        }
    }

    /** "… Retry after 2026-11-25 00:51:00." → instant (supplier time is Africa/Tunis). */
    static Instant retryAfter(String description) {
        Matcher m = RETRY_AFTER.matcher(description);
        if (!m.find()) return null;
        try {
            return LocalDateTime.parse(m.group(1).replace(' ', 'T')).atZone(SUPPLIER_ZONE).toInstant();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** List* responses: either {"ListX":[...]} or a bare array. */
    static List<JsonNode> list(JsonNode root, String field) {
        JsonNode arr = root.isArray() ? root : root.path(field);
        List<JsonNode> out = new ArrayList<>();
        if (arr.isArray()) for (int i = 0; i < arr.size(); i++) out.add(arr.get(i));
        return out;
    }

    static StaticHotel staticHotel(JsonNode h, String cityId, String cityName) {
        JsonNode city = h.path("City");
        return new StaticHotel(
                text(h, "Id"),
                text(h, "Name"),
                city.isObject() && !text(city, "Id").isBlank() ? text(city, "Id") : cityId,
                city.isObject() && !text(city, "Name").isBlank() ? text(city, "Name") : cityName,
                integer(h.path("Category"), "Star"),
                blankToNull(text(h, "Image")));
    }

    static List<Offer> offers(JsonNode root) {
        List<Offer> offers = new ArrayList<>();
        for (JsonNode r : list(root, "HotelSearch")) {
            JsonNode hotel = r.path("Hotel");
            List<BoardRate> boards = new ArrayList<>();
            for (JsonNode b : list(r.path("Price"), "Boarding")) {
                List<RoomRate> rooms = new ArrayList<>();
                // Pax groups = requested room occupancies; v1 searches a single room, so all groups are read.
                for (JsonNode pax : list(b, "Pax")) {
                    for (JsonNode room : list(pax, "Rooms")) {
                        rooms.add(new RoomRate(
                                text(room, "Id"),
                                text(room, "Name"),
                                decimal(room, "Price"),
                                !room.path("StopReservation").asBoolean(false),
                                isRefundable(room.path("CancellationPolicy")),
                                blankToNull(text(room, "CancellationDeadline")),
                                integer(room, "Quantity")));
                    }
                }
                boards.add(new BoardRate(text(b, "Id"), text(b, "Code"), text(b, "Name"), rooms));
            }
            offers.add(new Offer(
                    text(hotel, "Id"),
                    text(hotel, "Name"),
                    text(hotel.path("City"), "Name"),
                    integer(hotel.path("Category"), "Star"),
                    blankToNull(text(hotel, "Image")),
                    blankToNull(text(hotel, "Adress")),
                    text(r, "Currency").isBlank() ? null : text(r, "Currency"),
                    boards,
                    decimal(r.path("Promotion"), "Rate"),
                    text(r, "Token"),
                    text(r, "Source")));
        }
        return offers;
    }

    /** HotelDetail response → normalised details (HTML left raw: sanitised by the caller). */
    static HotelDetails hotelDetails(JsonNode root) {
        JsonNode d = root.has("HotelDetail") ? root.path("HotelDetail") : root;
        List<String> images = new ArrayList<>();
        if (!text(d, "Image").isBlank()) images.add(text(d, "Image"));
        for (JsonNode a : list(d, "Album")) {
            String url = a.isObject() ? text(a, "Url") : a.asString();
            if (!url.isBlank() && !images.contains(url)) images.add(url);
        }
        List<String> facilities = titles(d, "Facilities");
        for (String o : titles(d, "Option")) if (!facilities.contains(o)) facilities.add(o);
        List<String> themes = new ArrayList<>();
        for (JsonNode t : list(d, "Theme")) if (!t.asString().isBlank()) themes.add(t.asString().trim());
        List<String> boards = new ArrayList<>();
        for (JsonNode b : list(d, "Boarding")) if (!text(b, "Name").isBlank()) boards.add(text(b, "Name"));
        JsonNode loc = d.path("Localization");
        return new HotelDetails(
                blankToNull(text(d, "LongDescription").isBlank() ? text(d, "ShortDescription") : text(d, "LongDescription")),
                blankToNull(text(d, "Note")),
                blankToNull(text(d, "Adress").isBlank() ? text(d, "Address") : text(d, "Adress")),
                blankToNull(text(loc, "Latitude")),
                blankToNull(text(loc, "Longitude")),
                blankToNull(text(d, "CheckIn")),
                blankToNull(text(d, "CheckOut")),
                blankToNull(text(d, "Type")),
                images, facilities, titles(d, "Tag"), themes, boards);
    }

    /**
     * BookingCreation (PreBooking=true) → verification result. Field names for the total price are
     * not fully specified by the docs, so the usual candidates are tried (shape is logged by the client).
     */
    static BookingCheck bookingCheck(JsonNode root) {
        JsonNode b = root.path("BookingCreation");
        if (!b.isObject()) return new BookingCheck(false, null, null, false, "Aucune réponse de pré-réservation.");
        return new BookingCheck(true, totalPrice(b), blankToNull(text(b, "Currency")),
                b.path("OnRequest").asBoolean(false) || "OnRequest".equalsIgnoreCase(text(b, "State")),
                blankToNull(text(b, "Comment")));
    }

    static BookingConfirmation bookingConfirmation(JsonNode root) {
        JsonNode b = root.path("BookingCreation");
        if (!b.isObject() || text(b, "Id").isBlank()) {
            throw new SupplierException("Réservation non confirmée par le fournisseur (aucun numéro de réservation).");
        }
        JsonNode voucher = b.path("Voucher");
        if (voucher.isArray() && voucher.size() > 0) voucher = voucher.get(0);
        return new BookingConfirmation(text(b, "Id"), blankToNull(text(b, "State")),
                blankToNull(text(voucher, "Url")), totalPrice(b));
    }

    private static BigDecimal totalPrice(JsonNode b) {
        for (String f : List.of("TotalPrice", "Price", "Total", "Amount", "TotalAmount")) {
            BigDecimal v = decimal(b, f);
            if (v != null) return v;
        }
        return null;
    }

    private static List<String> titles(JsonNode node, String field) {
        List<String> out = new ArrayList<>();
        for (JsonNode n : list(node, field)) {
            String t = n.isObject() ? text(n, "Title") : n.asString().trim();
            if (!t.isBlank() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    private static boolean isRefundable(JsonNode policies) {
        for (JsonNode p : list(policies, "")) {
            if ("NON_REFUNDABLE".equalsIgnoreCase(text(p, "Nature"))) return false;
        }
        return true;
    }

    static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? "" : v.asString().trim();
    }

    static Integer integer(JsonNode node, String field) {
        String s = text(node, field);
        try {
            return s.isBlank() ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static BigDecimal decimal(JsonNode node, String field) {
        String s = text(node, field);
        try {
            return s.isBlank() ? null : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
