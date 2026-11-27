package pe.leadcenter.erp.shared.security;

import java.util.UUID;

/**
 * Contract for the authentication principal. The identity module's principal implements it so the
 * shared kernel (auditing, history) can learn who is acting without depending on that module.
 */
@FunctionalInterface
public interface AuthenticatedUser {

    UUID id();
}
