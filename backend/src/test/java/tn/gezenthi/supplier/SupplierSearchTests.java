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

    @BeforeEach
    void cacheStaticHotel() {
        SupplierHotel h = new SupplierHotel(FakeSupplierConfig.CODE, "171");
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
