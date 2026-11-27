package pe.leadcenter.erp.identity.infrastructure.out.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pe.leadcenter.erp.identity.domain.PasswordHash;
import pe.leadcenter.erp.identity.domain.port.out.PasswordHasher;

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
