package pe.leadcenter.erp.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline HTTP security: only the health probe is public; every other
 * endpoint requires authentication. Authentication itself arrives in the
 * identity module.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().authenticated())
				.build();
	}
}
