package tn.gezenthi.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads admin accounts from the database for Spring Security. */
@Service
class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository users;

    AdminUserDetailsService(AdminUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        AdminUser u = users.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("unknown"));
        return User.withUsername(u.getEmail())
                .password(u.getPasswordHash())
                .roles(u.getRole())
                .disabled(!u.isEnabled())
                .build();
    }
}
