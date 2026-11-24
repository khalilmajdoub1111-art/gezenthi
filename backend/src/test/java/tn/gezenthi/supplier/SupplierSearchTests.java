package tn.gezenthi.supplier;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unified search with a FAKE supplier (no real API call): own offers + supplier offers,
 * markup applied, supplier tokens never sent to the browser. Rolled back after each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SupplierSearchTests {

    static final String SECRET_TOKEN = "SUPPLIER-SECRET-TOKEN";

    @TestConfiguration
    static class FakeSupplierConfig {
        @Bean
        HotelSupplier fakeSupplier() {
            return new HotelSupplier() {
                public String code() { return "fake"; }
                public String displayName() { return "Fake Bed Bank"; }
                public boolean configured() { return true; }
                public String testConnection() { return "ok"; }
                public List<StaticHotel> fetchHotels(List<String> countries) {
                    return List.of(new StaticHotel("171", "Hotel Test 171", "1", "Hammamet", 4, null));
                }
                public List<Offer> search(SearchQuery q, List<String> ids) {
                    RoomRate room = new RoomRate("201", "Double Standard", new BigDecimal("1000.000"), true, true, "01/07/2027", 3);
                    return List.of(new Offer("171", "", "", null, null, null, "TND",
                            List.of(new BoardRate("4", "AI", "All Inclusive", List.of(room))), null, SECRET_TOKEN, "local-2"));
                }
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired SupplierHotelRepository supplierHotels;

    @BeforeEach
    void cacheStaticHotel() {
        SupplierHotel h = new SupplierHotel("fake", "171");
        h.refresh(new HotelSupplier.StaticHotel("171", "Hotel Test 171", "1", "Hammamet", 4, null), Instant.now());
        supplierHotels.save(h);
    }

    @Test
    void searchMergesOwnAndSupplierOffersWithMarkupAndHidesTokens() throws Exception {
        LocalDate in = LocalDate.now().plusDays(40);
        mvc.perform(get("/api/public/hotels/search").param("city", "hammamet")
                        .param("checkIn", in.toString()).param("checkOut", in.plusDays(7).toString()).param("adults", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nights").value(7))
                .andExpect(jsonPath("$.hotels[*].origin", hasItem("agence")))      // demo hotels in Hammamet
                .andExpect(jsonPath("$.hotels[*].origin", hasItem("partenaire")))  // fake supplier
                .andExpect(jsonPath("$.hotels[?(@.origin=='partenaire')].name", hasItem("Hotel Test 171"))) // from cache
                .andExpect(jsonPath("$.hotels[?(@.origin=='partenaire')].fromPrice", hasItem(1100))) // 1000 + 10 %
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString(SECRET_TOKEN))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Fake Bed Bank"))));
    }

    @Test
    void searchValidatesInput() throws Exception {
        mvc.perform(get("/api/public/hotels/search").param("city", "Tunis")
                        .param("checkIn", LocalDate.now().minusDays(1).toString()).param("checkOut", LocalDate.now().plusDays(2).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supplierAdminRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/suppliers")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminSeesSuppliersAndUnconfiguredTestFailsSafely() throws Exception {
        mvc.perform(get("/api/admin/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("tunisiabeds")))
                .andExpect(jsonPath("$[?(@.code=='tunisiabeds')].bookingEnabled", hasItem(false)))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("password"))));

        mvc.perform(post("/api/admin/suppliers/fake/sync").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }
}
