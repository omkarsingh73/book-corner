package com.bookcorner.common.persistence;

import com.bookcorner.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Spring Data JPA Auditing Configuration.
 * Activates AuditingEntityListener to automatically populate created_at, created_by,
 * updated_at, and updated_by on BaseAuditEntity instances using the authenticated
 * Spring Security principal or defaults to 'SYSTEM'.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * Resolves the active user email from the SecurityContext for JPA entity auditing.
     * Falls back to "SYSTEM" when operations occur outside an authenticated user session
     * (e.g. system background jobs, guest sessions, initial database migrations).
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> SecurityUtils.getCurrentUserEmail().or(() -> Optional.of("SYSTEM"));
    }
}
