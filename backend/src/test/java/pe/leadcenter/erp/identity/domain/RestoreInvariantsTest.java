package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class RestoreInvariantsTest {

    private static final Email EMAIL = new Email("a@example.com");
    private static final PasswordHash HASH = new PasswordHash("$2a$10$hash");

    private static Set<Permission> withNull() {
        Set<Permission> set = new HashSet<>();
        set.add(null);
        return set;
    }

    private static Set<UUID> idsWithNull() {
        Set<UUID> set = new HashSet<>();
        set.add(null);
        return set;
    }

    @Test
    void systemRoleMissingAnyPermissionIsRejectedOnRestore() {
        for (Permission missing : Permission.values()) {
            Set<Permission> perms = EnumSet.allOf(Permission.class);
            perms.remove(missing);
            assertThatThrownBy(
                            () ->
                                    Role.restore(
                                            Role.ADMINISTRATOR_ID,
                                            "Administrador",
                                            null,
                                            true,
                                            perms))
                    .as("missing %s", missing)
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .extracting("code")
                    .isEqualTo("role.system_protected");
        }
        Role full =
                Role.restore(
                        Role.ADMINISTRATOR_ID,
                        "Administrador",
                        null,
                        true,
                        EnumSet.allOf(Permission.class));
        assertThat(full.system()).isTrue();
        assertThat(full.permissions()).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void systemFlagMustMatchTheAdministratorId() {
        Set<Permission> all = EnumSet.allOf(Permission.class);
        assertThatThrownBy(
                        () ->
                                Role.restore(
                                        Role.ADMINISTRATOR_ID, "Administrador", null, false, all))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("role.system_flag_invalid");
        assertThatThrownBy(() -> Role.restore(UUID.randomUUID(), "Otro", null, true, all))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("role.system_flag_invalid");
        assertThat(Role.restore(Role.SALES_ID, "Ventas", null, false, Set.of()).system()).isFalse();
    }

    @Test
    void restoredRoleKeepsDomainValidation() {
        Set<Permission> ok = Set.of(Permission.ROLES_VER);
        assertThatThrownBy(() -> Role.restore(null, "x", null, false, ok))
                .extracting("code")
                .isEqualTo("role.id_required");
        assertThatThrownBy(() -> Role.restore(UUID.randomUUID(), " ", null, false, ok))
                .extracting("code")
                .isEqualTo("role.name_invalid");
        assertThatThrownBy(() -> Role.restore(UUID.randomUUID(), "x", null, false, null))
                .extracting("code")
                .isEqualTo("role.permissions_invalid");
        assertThatThrownBy(() -> Role.restore(UUID.randomUUID(), "x", null, false, withNull()))
                .extracting("code")
                .isEqualTo("role.permissions_invalid");
        assertThat(Role.restore(UUID.randomUUID(), "  Ventas  ", null, false, ok).name())
                .isEqualTo("Ventas");
    }

    @Test
    void restoredUserKeepsDomainValidationAndStoredState() {
        UUID id = UUID.randomUUID();
        Set<UUID> roles = Set.of(Role.SALES_ID);
        assertThatThrownBy(() -> User.restore(null, EMAIL, "Ana", HASH, true, roles))
                .extracting("code")
                .isEqualTo("user.id_required");
        assertThatThrownBy(() -> User.restore(id, null, "Ana", HASH, true, roles))
                .extracting("code")
                .isEqualTo("user.email_required");
        assertThatThrownBy(() -> User.restore(id, EMAIL, "Ana", null, true, roles))
                .extracting("code")
                .isEqualTo("user.password_hash_required");
        assertThatThrownBy(() -> User.restore(id, EMAIL, " ", HASH, true, roles))
                .extracting("code")
                .isEqualTo("user.name_invalid");
        assertThatThrownBy(() -> User.restore(id, EMAIL, "Ana", HASH, true, null))
                .extracting("code")
                .isEqualTo("user.role_required");
        assertThatThrownBy(() -> User.restore(id, EMAIL, "Ana", HASH, true, idsWithNull()))
                .extracting("code")
                .isEqualTo("user.role_required");

        User restored = User.restore(id, EMAIL, "Ana", HASH, false, roles);
        assertThat(restored.id()).isEqualTo(id);
        assertThat(restored.active()).isFalse();
        assertThat(restored.roleIds()).containsExactly(Role.SALES_ID);
        assertThat(Arrays.asList(restored.email(), restored.passwordHash()))
                .containsExactly(EMAIL, HASH);
    }
}
