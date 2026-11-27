package pe.leadcenter.erp.identity.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

/**
 * Invariant: the system always keeps an active user holding both {@code usuarios.gestionar} and
 * {@code roles.gestionar}. Evaluate it on the state AFTER the intended change (all users, all
 * roles).
 */
public final class AdministrationGuard {

    private AdministrationGuard() {}

    public static void verify(Collection<User> users, Collection<Role> roles) {
        Map<UUID, Role> byId =
                roles.stream().collect(Collectors.toMap(Role::id, Function.identity()));
        boolean hasAdministrator =
                users.stream().filter(User::active).anyMatch(user -> administers(user, byId));
        if (!hasAdministrator) {
            throw new BusinessRuleViolationException(
                    "identity.last_administrator",
                    "At least one active user must keep user and role administration");
        }
    }

    private static boolean administers(User user, Map<UUID, Role> rolesById) {
        Set<Permission> held = EnumSet.noneOf(Permission.class);
        user.roleIds().stream()
                .map(rolesById::get)
                .filter(Objects::nonNull)
                .forEach(role -> held.addAll(role.permissions()));
        return held.contains(Permission.USUARIOS_GESTIONAR)
                && held.contains(Permission.ROLES_GESTIONAR);
    }
}
