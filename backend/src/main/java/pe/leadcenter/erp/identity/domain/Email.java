package pe.leadcenter.erp.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

/** Normalized (trimmed, lowercase) email address. */
public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");

    public Email {
        value = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !SHAPE.matcher(value).matches()) {
            throw new BusinessRuleViolationException("user.email_invalid", "Invalid email address");
        }
    }
}
