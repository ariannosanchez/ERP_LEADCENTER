package pe.leadcenter.erp.identity.domain.port.out;

import pe.leadcenter.erp.identity.domain.PasswordHash;

/** Hashes raw passwords and checks raw passwords against stored hashes. */
public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
