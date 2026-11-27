package pe.leadcenter.erp.shared.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import pe.leadcenter.erp.shared.security.CurrentUser;

/** Enables JPA auditing with the authenticated user as auditor and UTC instants. */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(
        auditorAwareRef = "securityAuditorAware",
        dateTimeProviderRef = "utcDateTimeProvider")
class JpaAuditingConfig {

    @Bean
    AuditorAware<UUID> securityAuditorAware() {
        return CurrentUser::id;
    }

    @Bean
    DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
