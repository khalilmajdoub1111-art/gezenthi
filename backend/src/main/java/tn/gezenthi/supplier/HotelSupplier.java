package tn.gezenthi.supplier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A B2B hotel supplier (TunisiaBeds, another bed bank…), seen through ONE common contract.
 * Each supplier API gets an adapter implementing this interface; the rest of the
 * application (search, admin, future booking) never deals with supplier-specific formats.
 *
 * Booking methods (pre-book / book / cancel) are deliberately not part of this contract yet:
 * they will be added with the booking workflow, behind an explicit "booking enabled" switch.
 */
public interface HotelSupplier {

    /** Stable technical code, e.g. "tunisiabeds". */
    String code();

    /** Display name for the back-office (never shown to customers). */
    String displayName();

    /** True when URL and credentials are configured. */
    boolean configured();

    /** Cheap authenticated call to check URL + credentials. Returns a short summary. */
    String testConnection();

    /**
     * Static catalogue: every hotel the supplier sells in the given countries (ISO codes),
     * delivered city by city so the caller can save progressively (sync can take minutes
     * because suppliers rate-limit these calls).
     */
    void fetchHotels(List<String> countryCodes, CityBatchListener listener);

    @FunctionalInterface
    interface CityBatchListener {
        /** index is 1-based; total = number of cities to process. */
        void onCity(String cityName, int index, int total, List<StaticHotel> hotels);
    }

    /** Live availability and prices for the given supplier hotel ids (≤ 200 per call). */
    List<Offer> search(SearchQuery query, List<String> hotelIds);

    /** Rich static content for one hotel (photos, description, location…). Read-only. */
    HotelDetails hotelDetail(String externalHotelId);

    /** Description/notes are RAW supplier HTML: always sanitise before display. */
    record HotelDetails(String description, String notes, String address, String latitude, String longitude,
                        String checkIn, String checkOut, String type, List<String> images,
                        List<String> facilities, List<String> tags, List<String> themes, List<String> boards) {
    }

    // ---------------------------------------------------------------- normalised model

    record StaticHotel(String externalId, String name, String cityExternalId, String cityName,
                       Integer stars, String image) {
    }

    record Room(int adults, List<Integer> childAges) {
    }

    record SearchQuery(LocalDate checkIn, LocalDate checkOut, String city, List<Room> rooms) {
        public long nights() {
            return checkIn.until(checkOut).getDays();
        }
    }

    /** One hotel's availability. token/source are supplier booking references (kept server-side). */
    record Offer(String externalHotelId, String hotelName, String cityName, Integer stars, String image,
                 String address, String currency, List<BoardRate> boards, BigDecimal promotionRate,
                 String token, String source) {
    }

    record BoardRate(String id, String code, String name, List<RoomRate> rooms) {
    }

    /** Price = supplier (net) price for the whole stay, for the requested occupancy. */
    record RoomRate(String id, String name, BigDecimal price, boolean bookable, boolean refundable,
                    String cancellationDeadline, Integer quantity) {
    }
}
