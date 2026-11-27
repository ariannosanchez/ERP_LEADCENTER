package pe.leadcenter.erp.shared.domain;

/**
 * Base type of every business error. The {@code code} is a stable, machine-readable identifier (for
 * example {@code user.email_taken}) exposed to API clients; the message is human-readable.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
