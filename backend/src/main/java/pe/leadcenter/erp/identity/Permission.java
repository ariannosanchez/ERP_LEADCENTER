package pe.leadcenter.erp.identity;

import java.util.Arrays;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

/** Fixed catalog of permissions. Roles (data) grant subsets of it; other modules reference it. */
public enum Permission {
    USUARIOS_VER("usuarios.ver"),
    USUARIOS_GESTIONAR("usuarios.gestionar"),
    ROLES_VER("roles.ver"),
    ROLES_GESTIONAR("roles.gestionar"),
    LEADS_VER_PROPIOS("leads.ver_propios"),
    LEADS_VER_TODOS("leads.ver_todos"),
    LEADS_ASIGNAR("leads.asignar"),
    LEADS_GESTIONAR("leads.gestionar");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Permission fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.code.equals(code))
                .findFirst()
                .orElseThrow(
                        () ->
                                new BusinessRuleViolationException(
                                        "permission.unknown", "Unknown permission code: " + code));
    }
}
