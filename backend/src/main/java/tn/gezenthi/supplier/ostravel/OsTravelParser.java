package tn.gezenthi.supplier.ostravel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import tn.gezenthi.supplier.HotelSupplier.BoardRate;
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

    /** Throws if the standard error envelope reports a failure. */
    static void checkError(String supplierName, JsonNode root) {
        JsonNode err = root.path("ErrorMessage");
        if (err.isObject() && !text(err, "Description").isBlank()) {
            throw new SupplierException(supplierName + " : " + text(err, "Description") + " (code " + text(err, "Code") + ")");
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
