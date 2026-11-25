package tn.gezenthi.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Real flow with a FAKE mail server (captures messages, sends nothing):
 * booking request → customer e-mail + agency alert, WhatsApp skipped (not configured),
 * status CONFIRMED → customer e-mail. Not transactional (sending happens after commit),
 * so the booking it creates is deleted afterwards.
 */
@SpringBootTest(properties = {
        "gezenthi.notifications.email.enabled=true",
        "gezenthi.notifications.email.from=reservation@gezenthi.test",
        "gezenthi.notifications.agency-email=agence@gezenthi.test",
        "gezenthi.notifications.whatsapp.enabled=false"})
@AutoConfigureMockMvc
class BookingNotificationTests {

    static final List<MimeMessage> SENT = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class FakeMailServer {
        @Bean
        JavaMailSender fakeMailSender() {
            return new JavaMailSender() {
                public MimeMessage createMimeMessage() { return new MimeMessage((jakarta.mail.Session) null); }
                public MimeMessage createMimeMessage(InputStream in) { return createMimeMessage(); }
                public void send(MimeMessage... messages) { SENT.addAll(List.of(messages)); }
                public void send(SimpleMailMessage... messages) { }
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private String reference;

    @AfterEach
    void cleanUp() {
        if (reference != null) jdbc.update("delete from booking_request where reference = ?", reference);
        SENT.clear();
    }

    @Test
    void bookingSendsCustomerAndAgencyEmailsAndStatusEmail() throws Exception {
        LocalDate in = LocalDate.now().plusDays(60);
        String search = mvc.perform(get("/api/public/hotels/search").param("city", "Hammamet")
                .param("checkIn", in.toString()).param("checkOut", in.plusDays(3).toString()))
                .andReturn().getResponse().getContentAsString();
        String offerId = ((List<String>) JsonPath.read(search, "$.hotels[?(@.origin=='agence')].offerId")).getFirst();

        String created = mvc.perform(post("/api/public/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"offerId":"%s","board":0,"room":0,
                 "customer":{"civility":"Mrs","firstName":"Amira","lastName":"Notif","email":"amira.notif@example.tn","phone":"+216 20 123 456"},
                 "passengers":[{"type":"ADULT","civility":"Mrs","firstName":"Amira","lastName":"Notif"},
                               {"type":"ADULT","civility":"Mr","firstName":"Sami","lastName":"Notif"}],
                 "consent":true,"whatsappOptIn":true}""".formatted(offerId)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        reference = JsonPath.read(created, "$.reference");
        String id = jdbc.queryForObject("select id::text from booking_request where reference = ?", String.class, reference);

        List<Map<String, Object>> log = awaitLog(id, 3);
        assertThat(log).extracting(r -> r.get("template") + "/" + r.get("status")).containsExactlyInAnyOrder(
                "BOOKING_RECEIVED/SENT", "BOOKING_RECEIVED/SKIPPED", "AGENCY_NEW_BOOKING/SENT");
        assertThat(log).extracting(r -> (String) r.get("recipient")).contains("a***@example.tn", "a***@gezenthi.test");

        assertThat(SENT).hasSize(2);
        assertThat(recipients()).containsExactlyInAnyOrder("amira.notif@example.tn", "agence@gezenthi.test");
        assertThat(SENT.stream().map(this::subject).toList()).allMatch(s -> s.contains(reference));

        mvc.perform(post("/api/admin/bookings/" + id + "/status").with(user("agent@gezenthi.tn").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
        assertThat(awaitLog(id, 4)).extracting(r -> r.get("template")).contains("STATUS_CONFIRMED");
        assertThat(SENT.stream().map(this::subject).toList()).anyMatch(s -> s.startsWith("Réservation confirmée"));
    }

    @Test
    void quoteRequestSendsCustomerAndAgencyEmails() throws Exception {
        String created = mvc.perform(post("/api/public/inquiries").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("""
                {"type":"TRIP","subject":"Istanbul 8 jours","adults":2,
                 "customer":{"civility":"Mr","firstName":"Karim","lastName":"Devis","email":"karim.devis@example.tn","phone":"22 333 444"},
                 "consent":true}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ref = JsonPath.read(created, "$.reference");
        try {
            String id = jdbc.queryForObject("select id::text from inquiry where reference = ?", String.class, ref);
            Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
            List<Map<String, Object>> rows = List.of();
            while (Instant.now().isBefore(deadline) && rows.size() < 2) {
                rows = jdbc.queryForList("select template, status from notification_log where inquiry_id = ?::uuid", id);
                Thread.sleep(100);
            }
            assertThat(rows).extracting(r -> r.get("template") + "/" + r.get("status"))
                    .containsExactlyInAnyOrder("INQUIRY_RECEIVED/SENT", "AGENCY_NEW_INQUIRY/SENT");
            assertThat(SENT.stream().map(this::subject).toList()).anyMatch(s -> s.contains(ref) && s.startsWith("Votre demande de devis"));
        } finally {
            jdbc.update("delete from inquiry where reference = ?", ref);
        }
    }

    private List<Map<String, Object>> awaitLog(String bookingId, int expected) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        List<Map<String, Object>> rows = List.of();
        while (Instant.now().isBefore(deadline)) {
            rows = jdbc.queryForList("select template, status, recipient from notification_log where booking_id = ?::uuid", bookingId);
            if (rows.size() >= expected) return rows;
            Thread.sleep(100);
        }
        return rows;
    }

    private List<String> recipients() {
        return SENT.stream().flatMap(m -> {
            try {
                return java.util.Arrays.stream(m.getAllRecipients()).map(Address::toString);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
    }

    private String subject(MimeMessage m) {
        try {
            return m.getSubject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
