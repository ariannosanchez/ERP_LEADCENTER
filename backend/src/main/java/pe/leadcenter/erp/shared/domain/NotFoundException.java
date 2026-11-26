package pe.leadcenter.erp.shared.domain;

/** A requested aggregate does not exist (or is soft-deleted). Mapped to HTTP 404. */
public class NotFoundException extends DomainException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
