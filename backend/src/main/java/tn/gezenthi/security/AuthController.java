package tn.gezenthi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** JSON login / logout / current user for the admin panel. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final Logger audit = LoggerFactory.getLogger("tn.gezenthi.audit");

    record LoginRequest(@NotBlank @Size(max = 160) String email, @NotBlank @Size(max = 200) String password) {
    }

    record Me(String email, String displayName, String role) {
        static Me of(AdminUser u) {
            return new Me(u.getEmail(), u.getDisplayName(), u.getRole());
        }
    }

    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepo;
    private final CsrfTokenRepository csrfRepo;
    private final LoginAttemptService attempts;
    private final AdminUserRepository users;

    AuthController(AuthenticationManager authManager, SecurityContextRepository contextRepo,
                   CsrfTokenRepository csrfRepo, LoginAttemptService attempts, AdminUserRepository users) {
        this.authManager = authManager;
        this.contextRepo = contextRepo;
        this.csrfRepo = csrfRepo;
        this.attempts = attempts;
        this.users = users;
    }

    /** Makes sure the XSRF-TOKEN cookie exists before the SPA sends a POST. */
    @GetMapping("/csrf")
    ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken(); // touching the deferred token writes the cookie
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    @Transactional
    ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String email = body.email().trim();
        if (attempts.isLocked(email)) {
            audit.warn("LOGIN_LOCKED email={} ip={}", email, request.getRemoteAddr());
            return problem(HttpStatus.TOO_MANY_REQUESTS, "Trop de tentatives. Réessayez dans 15 minutes.");
        }
        Authentication auth;
        try {
            auth = authManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, body.password()));
        } catch (AuthenticationException ex) {
            attempts.recordFailure(email);
            audit.warn("LOGIN_FAILED email={} ip={}", email, request.getRemoteAddr());
            // Same message whether the email exists or not (no account enumeration).
            return problem(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }
        attempts.recordSuccess(email);

        // New session id after login (session-fixation protection), then store the authenticated context.
        HttpSession existing = request.getSession(false);
        if (existing != null) request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepo.saveContext(context, request, response);
        // Rotate the CSRF token: the frontend fetches a fresh one via /api/auth/csrf.
        csrfRepo.saveToken(null, request, response);

        AdminUser user = users.findByEmail(auth.getName()).orElseThrow();
        user.markLoggedIn();
        audit.info("LOGIN_OK email={} ip={}", user.getEmail(), request.getRemoteAddr());
        return ResponseEntity.ok(Me.of(user));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        if (auth != null) audit.info("LOGOUT email={}", auth.getName());
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        csrfRepo.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return users.findByEmail(auth.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Me.of(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
