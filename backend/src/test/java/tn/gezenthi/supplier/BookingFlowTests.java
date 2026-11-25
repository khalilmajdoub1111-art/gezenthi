package tn.gezenthi.supplier;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking requests end to end (fake supplier, rolled back): price from the server-side offer,
 * passenger checks, consent, admin workflow, supplier pre-booking, real booking refused.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(FakeSupplierConfig.class)
class BookingFlowTests {

    private static final RequestPostProcessor ADMIN = user("agent@gezenthi.tn").roles("ADMIN");

    @Autowired MockMvc mvc;
    @Autowired SupplierHotelRepository supplierHotels;

    private final LocalDate in = LocalDate.now().plusDays(50);

    @BeforeEach
    void partnerHotel() {
        SupplierHotel h = new SupplierHotel(FakeSupplierConfig.CODE, "171");
        h.refresh(new HotelSupplier.StaticHotel("171", "Hotel Test 171", "1", "Hammamet", 4, null), Instant.now());
        supplierHotels.save(h);
    }

    @Test
    void customerBooksOwnOfferAndAdminSeesItWithHistory() throws Exception {
        String offerId = offerId("agence");
        mvc.perform(get("/api/public/offers/" + offerId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.adults").value(2)).andExpect(jsonPath("$.available").value(true));

        String created = mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(booking(offerId, true, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(startsWith("GZ-")))
                .andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(created, "$.reference");

        String list = mvc.perform(get("/api/admin/bookings").with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].reference", hasItem(reference))).andReturn().getResponse().getContentAsString();
        String id = idOf(list, reference);

        mvc.perform(get("/api/admin/bookings/" + id).with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers.length()").value(2))
                .andExpect(jsonPath("$.events[0].action").value("CREATED"))
                .andExpect(jsonPath("$.customerEmail").value("amira@example.tn"));

        mvc.perform(post("/api/admin/bookings/" + id + "/status").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"note\":\"Client rappelé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.events[1].actor").value("agent@gezenthi.tn"));
    }

    @Test
    void requestIsRejectedWhenOfferUnknownPassengersWrongOrNoConsent() throws Exception {
        mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(booking("does-not-exist", true, 2))).andExpect(status().isGone());

        String offerId = offerId("agence");
        mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(offerId, true, 1))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(booking(offerId, false, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.consent").exists());
    }

    @Test
    void partnerBookingCanBePriceCheckedButRealBookingIsRefused() throws Exception {
        String offerId = offerId("partenaire");
        String created = mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(booking(offerId, true, 2))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(created, "$.reference");
        String id = idOf(mvc.perform(get("/api/admin/bookings").with(ADMIN)).andReturn().getResponse().getContentAsString(), reference);

        mvc.perform(post("/api/admin/bookings/" + id + "/supplier/check").with(ADMIN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.supplierPrice").value(1000.0))
                .andExpect(jsonPath("$.sellPrice").value(1100.0));
        mvc.perform(get("/api/admin/bookings/" + id).with(ADMIN)).andExpect(jsonPath("$.status").value("PRICE_CHECKED"));

        // FakeSupplier.book() throws if ever called: the service must refuse before reaching it.
        mvc.perform(post("/api/admin/bookings/" + id + "/supplier/book").with(ADMIN).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/admin/bookings/" + id).with(ADMIN))
                .andExpect(jsonPath("$.events[*].action", hasItem("SUPPLIER_BOOK_REFUSED")))
                .andExpect(jsonPath("$.supplierBookingEnabled").value(false));
    }

    @Test
    void adminBookingApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/bookings")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ helpers

    private static String idOf(String listJson, String reference) {
        List<String> ids = JsonPath.read(listJson, "$[?(@.reference=='" + reference + "')].id");
        return ids.getFirst();
    }

    private String offerId(String origin) throws Exception {
        String body = mvc.perform(get("/api/public/hotels/search").param("city", "Hammamet")
                        .param("checkIn", in.toString()).param("checkOut", in.plusDays(4).toString()).param("adults", "2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return (String) ((List<?>) JsonPath.read(body, "$.hotels[?(@.origin=='" + origin + "')].offerId")).getFirst();
    }

    private static String booking(String offerId, boolean consent, int adults) {
        StringBuilder pax = new StringBuilder();
        for (int i = 0; i < adults; i++) {
            if (i > 0) pax.append(',');
            pax.append("{\"type\":\"ADULT\",\"civility\":\"Mrs\",\"firstName\":\"Amira").append(i).append("\",\"lastName\":\"Ben Ali\"}");
        }
        return """
                {"offerId":"%s","board":0,"room":0,
                 "customer":{"civility":"Mrs","firstName":"Amira","lastName":"Ben Ali","email":"Amira@Example.tn","phone":"+216 20 000 000"},
                 "passengers":[%s],"comment":"Chambre calme","consent":%s}""".formatted(offerId, pax, consent);
    }
}
