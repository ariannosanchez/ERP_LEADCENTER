package pe.leadcenter.erp.identity.infrastructure.out.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.leadcenter.erp.identity.domain.PasswordHash;
import pe.leadcenter.erp.identity.domain.port.out.PasswordHasher;

/** Spring Security delegating encoder (bcrypt by default); the bean is reused by authentication. */
@Configuration(proxyBeanMethods = false)
class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

@Component
@RequiredArgsConstructor
class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return encoder.matches(rawPassword, hash.value());
    }
}
