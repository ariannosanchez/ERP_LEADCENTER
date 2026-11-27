package pe.leadcenter.erp.shared.domain;

/** A business invariant would be broken by the requested operation. Mapped to HTTP 422. */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(String code, String message) {
        super(code, message);
    }
}
