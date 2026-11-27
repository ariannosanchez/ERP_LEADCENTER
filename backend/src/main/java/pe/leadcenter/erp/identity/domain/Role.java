package pe.leadcenter.erp.identity.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
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
        if (id == null) {
            throw new BusinessRuleViolationException("role.id_required", "Role id is required");
        }
        this.id = id;
        this.name = validName(name);
        this.description = description; // null is allowed and stored as-is
        this.system = system;
        this.permissions = EnumSet.noneOf(Permission.class);
        this.permissions.addAll(validPermissions(perms));
    }

    public static Role create(String name, String description, Set<Permission> permissions) {
        return new Role(UuidV7.generate(), name, description, false, permissions);
    }

    /** Rebuilds a stored role: validates invariants, no creation side effects. */
    public static Role restore(
            UUID id, String name, String description, boolean system, Set<Permission> permissions) {
        Role role = new Role(id, name, description, system, permissions);
        if (system && !role.permissions.containsAll(EnumSet.allOf(Permission.class))) {
            throw protectedRole("A system role must hold every permission");
        }
        return role;
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
        permissions.addAll(validPermissions(granted == null ? null : Arrays.asList(granted)));
    }

    public void revoke(Permission... revoked) {
        if (system) {
            throw protectedRole("Permissions of a system role cannot be revoked");
        }
        permissions.removeAll(validPermissions(revoked == null ? null : Arrays.asList(revoked)));
    }

    public void replacePermissions(Set<Permission> newPermissions) {
        Set<Permission> valid = validPermissions(newPermissions);
        if (system && !valid.containsAll(EnumSet.allOf(Permission.class))) {
            throw protectedRole("Permissions of a system role cannot be revoked");
        }
        permissions.clear();
        permissions.addAll(valid);
    }

    /** Validates before any mutation: null collection or null element is a domain error. */
    private static Set<Permission> validPermissions(Collection<Permission> source) {
        if (source == null || source.stream().anyMatch(Objects::isNull)) {
            throw new BusinessRuleViolationException(
                    "role.permissions_invalid", "Permissions must be non-null");
        }
        Set<Permission> copy = EnumSet.noneOf(Permission.class);
        copy.addAll(source);
        return copy;
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
