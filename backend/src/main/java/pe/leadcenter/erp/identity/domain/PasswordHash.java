package pe.leadcenter.erp.identity.domain;

import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

/** An already-hashed password. Never holds a raw password; never printed. */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleViolationException(
                    "user.password_hash_blank", "Password hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[****]";
    }
}
