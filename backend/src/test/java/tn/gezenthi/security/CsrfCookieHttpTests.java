package tn.gezenthi.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Real HTTP (not MockMvc, which replaces the CSRF repository): the browser must receive
 * an XSRF-TOKEN cookie that JavaScript can read, and the session cookie must stay HttpOnly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CsrfCookieHttpTests {

    @LocalServerPort
    int port;

    @Test
    void csrfEndpointSetsReadableXsrfCookie() throws Exception {
        HttpResponse<Void> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/csrf")).build(),
                HttpResponse.BodyHandlers.discarding());

        assertThat(res.statusCode()).isEqualTo(204);
        String xsrf = res.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow();
        assertThat(xsrf).doesNotContain("HttpOnly");
    }
}
