package tn.gezenthi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin account at startup from ADMIN_BOOTSTRAP_EMAIL / ADMIN_BOOTSTRAP_PASSWORD
 * (local: %USERPROFILE%/.gezenthi/db.env, server: environment variables).
 * Never overwrites an existing account and never logs the password. No default password exists.
 */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminUserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    AdminBootstrap(AdminUserRepository users, PasswordEncoder encoder,
                   @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
                   @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.isEmpty()) {
            if (users.count() == 0) log.warn("No admin account exists. Set ADMIN_BOOTSTRAP_EMAIL and ADMIN_BOOTSTRAP_PASSWORD to create one.");
            return;
        }
        if (users.findByEmail(email).isPresent()) return;
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.error("ADMIN_BOOTSTRAP_PASSWORD is too short (min {} characters): admin account NOT created.", MIN_PASSWORD_LENGTH);
            return;
        }
        users.save(new AdminUser(email, "Administrateur", encoder.encode(password)));
        log.info("Admin account created for {}", email);
    }
}
