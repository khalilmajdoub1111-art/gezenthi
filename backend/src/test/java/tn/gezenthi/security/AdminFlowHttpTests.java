package tn.gezenthi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Replays exactly what the Angular admin does over real HTTP, with a real cookie jar:
 * GET /api/auth/csrf → POST /api/auth/login → GET /api/auth/csrf → POST /api/admin/omra → public catalogue.
 * Uses its own throw-away account and removes everything it created.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminFlowHttpTests {

    private static final String PASSWORD = "flow-test-password-42";

    @LocalServerPort int port;
    @Autowired AdminUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    private String email;
    private String title;
    private CookieManager cookies;
    private HttpClient http;

    @BeforeEach
    void setUp() {
        email = "flow-" + UUID.randomUUID() + "@gezenthi.test";
        title = "Flow Omra " + UUID.randomUUID();
        users.save(new AdminUser(email, "Flow Test", encoder.encode(PASSWORD)));
        cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        http = HttpClient.newBuilder().cookieHandler(cookies).build();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from omra_program where title = ?", title);
        jdbc.update("delete from admin_user where email = ?", email);
    }

    @Test
    void angularLikeLoginThenCreateOfferAppearsOnPublicSite() throws Exception {
        assertThat(send("GET", "/api/auth/csrf", null).statusCode()).isEqualTo(204);

        HttpResponse<String> login = send("POST", "/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}");
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);

        assertThat(send("GET", "/api/auth/csrf", null).statusCode()).isEqualTo(204);
        HttpResponse<String> created = send("POST", "/api/admin/omra",
                "{\"published\":true,\"title\":\"" + title + "\",\"durationDays\":15,\"departure\":\"1 juin 2027\","
                        + "\"hotelDistance\":200,\"priceFrom\":5000,\"image\":\"\",\"includes\":[\"Visa\"]}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);

        assertThat(send("GET", "/api/public/catalog", null).body()).contains(title);
    }

    private HttpResponse<String> send(String method, String path, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json != null) b.header("Content-Type", "application/json");
        xsrf().ifPresent(token -> b.header("X-XSRF-TOKEN", token)); // what Angular's HttpClient does
        b.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Optional<String> xsrf() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("XSRF-TOKEN") && !c.hasExpired())
                .map(HttpCookie::getValue).filter(v -> !v.isEmpty()).findFirst();
    }
}
