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
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@Import(FakeSupplierConfig.class)
class SupplierSearchTests {

    @Autowired MockMvc mvc;
    @Autowired SupplierHotelRepository supplierHotels;

    private SupplierHotel hotel;

    @BeforeEach
    void cacheStaticHotel() {
        hotel = new SupplierHotel(FakeSupplierConfig.CODE, "171");
        hotel.refresh(new HotelSupplier.StaticHotel("171", "Hotel Test 171", "1", "Hammamet", 4,
                "https://cdn.example.com/171/main.jpg"), Instant.now());
        hotel = supplierHotels.save(hotel);
    }

    @Test
    void partnerHotelPageShowsDetailsWithSanitisedHtml() throws Exception {
        mvc.perform(get("/api/public/hotels/p-" + hotel.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hotel Test 171"))
                .andExpect(jsonPath("$.origin").value("partenaire"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.facilities", hasItem("Piscine")))
                .andExpect(jsonPath("$.latitude").value("36.4"))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.containsString("Belle plage")))
                .andExpect(jsonPath("$.description").value(not(org.hamcrest.Matchers.containsString("script"))))
                .andExpect(jsonPath("$.description").value(not(org.hamcrest.Matchers.containsString("javascript"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString("Fake Bed Bank"))));
    }

    @Test
    void partnerHotelAvailabilityAndFeaturedList() throws Exception {
        LocalDate in = LocalDate.now().plusDays(40);
        mvc.perform(get("/api/public/hotels/p-" + hotel.getId() + "/availability")
                        .param("checkIn", in.toString()).param("checkOut", in.plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromPrice").value(1100))
                .andExpect(jsonPath("$.ref").value("p-" + hotel.getId()))
                .andExpect(jsonPath("$.boards[0].rooms[0].id").value("201"));

        // Real DB may hold many synced hotels: check the rules, not a specific hotel.
        mvc.perform(get("/api/public/hotels/partners/featured").param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(6)))
                .andExpect(jsonPath("$[*].ref", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.startsWith("p-"))))
                .andExpect(jsonPath("$[*].image", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void unknownHotelRefIs404() throws Exception {
        mvc.perform(get("/api/public/hotels/p-not-a-uuid")).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/hotels/x-123")).andExpect(status().isNotFound());
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
                .andExpect(content().string(not(org.hamcrest.Matchers.containsString(FakeSupplierConfig.SECRET_TOKEN))))
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
    }
}
