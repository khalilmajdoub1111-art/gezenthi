package tn.gezenthi.catalog;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end API tests against the local PostgreSQL.
 * Each test runs in a transaction that is rolled back, so the dev data is untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "ADMIN")
class CatalogApiTests {

    private static final String HOTEL = """
            {"published": %s, "name": "Test Nabeul Beach", "region": "tunisie", "city": "Nabeul", "stars": 4,
             "rating": 8.6, "reviews": 12, "board": "Tout compris", "pricePerNight": 135, "oldPrice": 160,
             "image": "https://images.unsplash.com/photo-1571896349842-33c89424de2d", "tags": ["Plage", "Famille"]}
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void createUpdateDeleteHotel() throws Exception {
        String body = mvc.perform(post("/api/admin/hotels").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(HOTEL.formatted(true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.tags[1]").value("Famille"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/public/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotels[*].name", hasItem("Test Nabeul Beach")));

        // Unpublish: disappears from the public catalogue, still visible in admin
        mvc.perform(put("/api/admin/hotels/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(HOTEL.formatted(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
        mvc.perform(get("/api/public/catalog"))
                .andExpect(jsonPath("$.hotels[*].name", not(hasItem("Test Nabeul Beach"))));
        mvc.perform(get("/api/admin/hotels/" + id)).andExpect(status().isOk());

        mvc.perform(delete("/api/admin/hotels/" + id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/hotels/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidHotelWithFieldErrors() throws Exception {
        String invalid = """
                {"published": true, "name": "", "region": "mars", "city": "Nabeul", "stars": 7,
                 "board": "Tout compris", "pricePerNight": -5, "image": "javascript:alert(1)"}
                """;
        mvc.perform(post("/api/admin/hotels").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.region").exists())
                .andExpect(jsonPath("$.errors.stars").exists())
                .andExpect(jsonPath("$.errors.pricePerNight").exists())
                .andExpect(jsonPath("$.errors.image").exists());
    }

    @Test
    void everyAdminCollectionIsListable() throws Exception {
        for (String path : new String[] {"hotels", "trips", "omra", "flights", "services"}) {
            mvc.perform(get("/api/admin/" + path)).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        }
    }

    @Test
    void createsEachOtherOfferType() throws Exception {
        create("trips", """
                {"published": true, "name": "Bakou", "country": "Azerbaïdjan", "durationDays": 7, "priceFrom": 2590,
                 "image": "https://images.unsplash.com/photo-1", "noVisa": true, "badge": "Nouveau"}""");
        create("omra", """
                {"published": true, "title": "Omra Test", "durationDays": 15, "departure": "1 avril 2027",
                 "hotelDistance": 200, "priceFrom": 5990, "image": "", "includes": ["Visa"]}""");
        create("flights", """
                {"published": true, "city": "Lyon", "airportCode": "lys", "priceFrom": 640}""");
        create("services", """
                {"published": true, "kind": "Visa", "title": "Visa Turquie", "priceFrom": 90, "description": "e-Visa."}""");

        mvc.perform(get("/api/admin/flights")).andExpect(jsonPath("$[*].airportCode", hasItem("LYS")));
    }

    private void create(String path, String json) throws Exception {
        mvc.perform(post("/api/admin/" + path)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }
}
