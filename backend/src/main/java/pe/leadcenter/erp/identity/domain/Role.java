package pe.leadcenter.erp.identity.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.domain.UuidV7;

/** A named set of permissions. The system role "Administrador" is protected. */
public final class Role {

    public static final UUID ADMINISTRATOR_ID =
            UUID.fromString("00000000-0000-7000-8000-000000000001");
    public static final UUID SALES_ID = UUID.fromString("00000000-0000-7000-8000-000000000002");
    private static final int NAME_MAX = 100;

    private final UUID id;
    private String name;
    private String description;
    private final boolean system;
    private final Set<Permission> permissions;

    private Role(
            UUID id,
            String name,
            String description,
            boolean system,
            Collection<Permission> perms) {
        this.id = id;
        this.name = validName(name);
        this.description = description;
        this.system = system;
        this.permissions = EnumSet.noneOf(Permission.class);
        this.permissions.addAll(perms);
    }

    public static Role create(String name, String description, Set<Permission> permissions) {
        return new Role(UuidV7.generate(), name, description, false, permissions);
    }

    public static Role administrator() {
        return new Role(
                ADMINISTRATOR_ID,
                "Administrador",
                "Full access (protected system role)",
                true,
                EnumSet.allOf(Permission.class));
    }

    public static Role sales() {
        return new Role(
                SALES_ID,
                "Comercial / Ventas",
                "Sales advisor: sees and manages own leads",
                false,
                EnumSet.of(Permission.LEADS_VER_PROPIOS, Permission.LEADS_GESTIONAR));
    }

    public UUID id() {
        return id;
    }

    public boolean system() {
        return system;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Set<Permission> permissions() {
        return Set.copyOf(permissions);
    }

    public void rename(String newName) {
        if (system) {
            throw protectedRole("A system role cannot be renamed");
        }
        this.name = validName(newName);
    }

    public void changeDescription(String newDescription) {
        this.description = newDescription;
    }

    public void grant(Permission... granted) {
        permissions.addAll(Set.of(granted));
    }

    public void revoke(Permission... revoked) {
        if (system) {
            throw protectedRole("Permissions of a system role cannot be revoked");
        }
        Set.of(revoked).forEach(permissions::remove);
    }

    public void replacePermissions(Set<Permission> newPermissions) {
        if (system && !newPermissions.containsAll(EnumSet.allOf(Permission.class))) {
            throw protectedRole("Permissions of a system role cannot be revoked");
        }
        permissions.clear();
        permissions.addAll(newPermissions);
    }

    private static BusinessRuleViolationException protectedRole(String message) {
        return new BusinessRuleViolationException("role.system_protected", message);
    }

    private static String validName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX) {
            throw new BusinessRuleViolationException(
                    "role.name_invalid", "Role name must be 1 to " + NAME_MAX + " characters");
        }
        return trimmed;
    }
}
