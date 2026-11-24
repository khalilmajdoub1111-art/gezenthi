package tn.gezenthi.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Security rules of the API. Runs in rolled-back transactions. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthSecurityTests {

    private static final String PASSWORD = "correct-horse-battery-42";

    @Autowired MockMvc mvc;
    @Autowired AdminUserRepository users;
    @Autowired PasswordEncoder encoder;

    private String email;

    @BeforeEach
    void createAdmin() {
        email = "test-" + UUID.randomUUID() + "@gezenthi.tn";
        users.save(new AdminUser(email, "Test Admin", encoder.encode(PASSWORD)));
    }

    @Test
    void publicCatalogIsOpenButAdminApiRequiresLogin() throws Exception {
        mvc.perform(get("/api/public/catalog")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/hotels")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/hotels").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void passwordIsStoredHashed() {
        String hash = users.findByEmail(email).orElseThrow().getPasswordHash();
        org.assertj.core.api.Assertions.assertThat(hash).startsWith("{bcrypt}").doesNotContain(PASSWORD);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void writesWithoutCsrfTokenAreRejected() throws Exception {
        mvc.perform(post("/api/admin/hotels").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginThenAccessAdminThenLogout() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/admin/hotels").session(session)).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(users.findByEmail(email).orElseThrow()).extracting("lastLoginAt").isNotNull();

        mvc.perform(post("/api/auth/logout").with(csrf()).session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/hotels").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email, "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou mot de passe incorrect."));
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials("nobody@x.tn", "whatever")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou mot de passe incorrect."));
    }

    @Test
    void accountIsLockedAfterRepeatedFailures() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email, "wrong-" + i)))
                    .andExpect(status().isUnauthorized());
        }
        // Even the right password is refused while locked.
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email, PASSWORD)))
                .andExpect(status().isTooManyRequests());
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
