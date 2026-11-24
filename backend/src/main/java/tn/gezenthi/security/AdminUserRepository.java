package tn.gezenthi.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    @Query("select u from AdminUser u where lower(u.email) = lower(:email)")
    Optional<AdminUser> findByEmail(@Param("email") String email);
}
