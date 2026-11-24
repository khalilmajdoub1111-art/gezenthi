package tn.gezenthi.supplier.ostravel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tn.gezenthi.supplier.HotelSupplier.Offer;
import tn.gezenthi.supplier.HotelSupplier.StaticHotel;
import tn.gezenthi.supplier.SupplierException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Parses the official examples from https://api-edocs.os-travel.com (no network call). */
class OsTravelParserTests {

    private final JsonMapper json = JsonMapper.builder().build();

    private static final String HOTEL_SEARCH = """
            { "HotelSearch": [ { "Hotel": { "Id": 101, "Name": "Hotel Example Resort", "Category": { "Title": "5 étoiles", "Star": 5 },
              "City": { "Id": 12, "Name": "Example City" }, "Adress": "123 Example Boulevard", "Image": "https://cdn.example.com/101.jpg" },
              "Token": "eyJleGFtcGxlX3Rva2VuIjoiMTAxIn0=",
              "Price": { "Boarding": [ { "Id": 4, "Code": "BB", "Name": "Bed & Breakfast", "Pax": [ { "Adult": 2, "Rooms": [
                { "Id": 201, "Name": "Standard Room", "Quantity": 10, "Price": "450.000", "StopReservation": false,
                  "CancellationPolicy": [ { "Fees": "100.00", "Type": "PERCENT", "Nature": "BEFORE_ARRIVAL" } ],
                  "CancellationDeadline": "15/07/2026 00:00" } ] } ] } ], "BasePrice": "150.000" },
              "Source": "local-2", "Currency": "EUR" },
              { "Hotel": { "Id": 102, "Name": "Hotel Example Spa", "Category": { "Title": "4 étoiles", "Star": "4" }, "City": { "Id": 12, "Name": "Example City" } },
              "Token": "tok-102", "Price": { "Boarding": [ { "Id": 20, "Code": "AI", "Name": "All Inclusive", "Pax": [ { "Adult": 2, "Rooms": [
                { "Id": 301, "Name": "Superior Room", "Price": "720.000", "StopReservation": true,
                  "CancellationPolicy": [ { "Fees": "1", "Type": "NIGHT", "Nature": "NON_REFUNDABLE" } ] } ] } ] } ] },
              "Source": "local-2", "Currency": "EUR", "Promotion": { "Title": null, "Description": "", "Rate": "25.00" } } ] }
            """;

    @Test
    void parsesHotelSearchIntoNormalisedOffers() {
        List<Offer> offers = OsTravelParser.offers(json.readTree(HOTEL_SEARCH));

        assertThat(offers).hasSize(2);
        Offer first = offers.getFirst();
        assertThat(first.externalHotelId()).isEqualTo("101");
        assertThat(first.stars()).isEqualTo(5);
        assertThat(first.currency()).isEqualTo("EUR");
        assertThat(first.token()).isEqualTo("eyJleGFtcGxlX3Rva2VuIjoiMTAxIn0=");
        assertThat(first.source()).isEqualTo("local-2");
        assertThat(first.boards().getFirst().code()).isEqualTo("BB");
        assertThat(first.boards().getFirst().rooms().getFirst().price()).isEqualByComparingTo("450");
        assertThat(first.boards().getFirst().rooms().getFirst().bookable()).isTrue();
        assertThat(first.boards().getFirst().rooms().getFirst().refundable()).isTrue();

        Offer second = offers.get(1);
        assertThat(second.stars()).as("Star sent as a string").isEqualTo(4);
        assertThat(second.promotionRate()).isEqualByComparingTo(new BigDecimal("25"));
        assertThat(second.boards().getFirst().rooms().getFirst().bookable()).as("StopReservation").isFalse();
        assertThat(second.boards().getFirst().rooms().getFirst().refundable()).as("NON_REFUNDABLE").isFalse();
    }

    @Test
    void parsesListHotelBareArray() {
        JsonNode root = json.readTree("""
                [ { "Id": 101, "Name": "Hotel Les Orangers", "Category": { "Title": "4 Stars", "Star": "4" },
                    "City": { "Id": 1, "Name": "Hammamet" }, "Image": "https://cdn.example.com/hotels/101.jpg" } ]""");
        StaticHotel h = OsTravelParser.staticHotel(OsTravelParser.list(root, "ListHotel").getFirst(), "9", "Fallback");
        assertThat(h).isEqualTo(new StaticHotel("101", "Hotel Les Orangers", "1", "Hammamet", 4, "https://cdn.example.com/hotels/101.jpg"));
    }

    @Test
    void rateLimitErrorCarriesRetryAfter() {
        JsonNode root = json.readTree("""
                { "ListHotel": null, "ErrorMessage": { "Code": 429,
                  "Description": "Too many requests: ListHotel is limited to 5 calls per minute for your account. Retry after 2026-11-25 00:51:00." } }""");
        assertThatThrownBy(() -> OsTravelParser.checkError("TunisiaBeds", root))
                .isInstanceOfSatisfying(tn.gezenthi.supplier.SupplierRateLimitException.class, e ->
                        assertThat(e.retryAfter()).isEqualTo(java.time.Instant.parse("2026-11-24T23:51:00Z"))); // Tunis = UTC+1
    }

    @Test
    void errorEnvelopeBecomesSupplierException() {
        JsonNode root = json.readTree("""
                { "ListCategorie": null, "CountResults": 0,
                  "ErrorMessage": { "Code": 400, "Description": "Check the sending of the login and password" } }""");
        assertThatThrownBy(() -> OsTravelParser.checkError("TunisiaBeds", root))
                .isInstanceOf(SupplierException.class)
                .hasMessageContaining("Check the sending of the login and password");
    }
}
