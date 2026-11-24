package tn.gezenthi.supplier;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** A fake supplier for tests — no network. Two cities, one hotel each. */
@TestConfiguration
class FakeSupplierConfig {

    static final String SECRET_TOKEN = "SUPPLIER-SECRET-TOKEN";
    static final String CODE = "fake";

    @Bean
    HotelSupplier fakeSupplier() {
        return new HotelSupplier() {
            public String code() { return CODE; }
            public String displayName() { return "Fake Bed Bank"; }
            public boolean configured() { return true; }
            public String testConnection() { return "ok"; }

            public void fetchHotels(List<String> countries, CityBatchListener listener) {
                listener.onCity("Hammamet", 1, 2, List.of(new StaticHotel("171", "Hotel Test 171", "1", "Hammamet", 4, null)));
                listener.onCity("Sousse", 2, 2, List.of(new StaticHotel("172", "Hotel Test 172", "2", "Sousse", 3, null)));
            }

            public List<Offer> search(SearchQuery q, List<String> ids) {
                RoomRate room = new RoomRate("201", "Double Standard", new BigDecimal("1000.000"), true, true, "01/07/2027", 3);
                return List.of(new Offer("171", "", "", null, null, null, "TND",
                        List.of(new BoardRate("4", "AI", "All Inclusive", List.of(room))), null, SECRET_TOKEN, "local-2"));
            }
        };
    }
}
