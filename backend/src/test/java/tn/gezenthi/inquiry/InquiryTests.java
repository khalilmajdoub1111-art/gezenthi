package tn.gezenthi.inquiry;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Quote requests: creation, validation, admin workflow (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InquiryTests {

    private static final RequestPostProcessor ADMIN = user("agent@gezenthi.tn").roles("ADMIN");
    @Autowired MockMvc mvc;

    @Test
    void customerSendsQuoteRequestAndAdminProcessesIt() throws Exception {
        String created = mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("OMRA", "Omra Confort", LocalDate.now().plusDays(90).toString(), "true")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value(startsWith("DV-")))
                .andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(created, "$.reference");

        String list = mvc.perform(get("/api/admin/inquiries").with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].reference", hasItem(reference))).andReturn().getResponse().getContentAsString();
        String id = ((List<String>) JsonPath.read(list, "$[?(@.reference=='" + reference + "')].id")).getFirst();

        mvc.perform(get("/api/admin/inquiries/" + id).with(ADMIN)).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("OMRA"))
                .andExpect(jsonPath("$.customerEmail").value("leila@example.tn"))
                .andExpect(jsonPath("$.events[0].action").value("CREATED"));

        mvc.perform(post("/api/admin/inquiries/" + id + "/status").with(ADMIN).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"note\":\"Devis envoyé par e-mail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.events[1].actor").value("agent@gezenthi.tn"));
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        String future = LocalDate.now().plusDays(30).toString();
        mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("TRIP", "Istanbul", future, "false"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.consent").exists());
        mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("TRIP", "Istanbul", future, "null"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("CASINO", "x", future, "true"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(body("VISA", "Visa Schengen", LocalDate.now().minusDays(3).toString(), "true"))).andExpect(status().isBadRequest());
    }

    @Test
    void adminInquiryApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/admin/inquiries")).andExpect(status().isUnauthorized());
    }

    private static String body(String type, String subject, String dateFrom, String consent) {
        return """
                {"type":"%s","subject":"%s","offerRef":"omra-confort","dateFrom":"%s","adults":2,"children":1,
                 "message":"Chambre double proche du Haram",
                 "customer":{"civility":"Mrs","firstName":"Leila","lastName":"Ben Salah","email":"Leila@example.tn","phone":"+216 22 333 444"},
                 "whatsappOptIn":true,"consent":%s}""".formatted(type, subject, dateFrom, consent);
    }
}
