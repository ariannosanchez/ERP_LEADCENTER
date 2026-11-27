package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class RoleTest {

    private static void assertViolation(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo(code);
    }

    @Test
    void createCustomRoleTrimsNameAndStartsWithGivenPermissions() {
        Role role = Role.create("  Soporte ", "desc", Set.of(Permission.USUARIOS_VER));
        assertThat(role.name()).isEqualTo("Soporte");
        assertThat(role.system()).isFalse();
        assertThat(role.id()).isNotNull();
        assertThat(role.permissions()).containsExactly(Permission.USUARIOS_VER);
    }

    @Test
    void roleNameMustBeNonBlankAndBounded() {
        assertViolation(() -> Role.create(" ", null, Set.of()), "role.name_invalid");
        assertViolation(() -> Role.create("x".repeat(101), null, Set.of()), "role.name_invalid");
    }

    @Test
    void customRoleCanBeRenamedAndPermissionsChanged() {
        Role role = Role.create("A", null, Set.of());
        role.rename("B");
        role.changeDescription("nueva");
        role.grant(Permission.LEADS_ASIGNAR, Permission.ROLES_VER);
        role.revoke(Permission.ROLES_VER);
        assertThat(role.name()).isEqualTo("B");
        assertThat(role.description()).isEqualTo("nueva");
        assertThat(role.permissions()).containsExactly(Permission.LEADS_ASIGNAR);
        role.replacePermissions(Set.of(Permission.LEADS_VER_TODOS));
        assertThat(role.permissions()).containsExactly(Permission.LEADS_VER_TODOS);
    }

    @Test
    void permissionsViewCannotBeMutated() {
        Role role = Role.create("A", null, Set.of());
        assertThatThrownBy(() -> role.permissions().add(Permission.ROLES_VER))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void administratorHoldsEveryPermissionAndIsSystem() {
        Role admin = Role.administrator();
        assertThat(admin.id()).isEqualTo(Role.ADMINISTRATOR_ID);
        assertThat(admin.name()).isEqualTo("Administrador");
        assertThat(admin.system()).isTrue();
        assertThat(admin.permissions()).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void administratorIsProtected() {
        Role admin = Role.administrator();
        assertViolation(() -> admin.rename("Otro"), "role.system_protected");
        assertViolation(() -> admin.revoke(Permission.ROLES_VER), "role.system_protected");
        assertViolation(
                () -> admin.replacePermissions(Set.of(Permission.ROLES_VER)),
                "role.system_protected");
        admin.grant(Permission.ROLES_VER);
        admin.replacePermissions(Set.of(Permission.values()));
        assertThat(admin.permissions()).hasSize(Permission.values().length);
        assertThat(admin.name()).isEqualTo("Administrador");
    }

    @Test
    void salesRoleHasAgreedDefaults() {
        Role sales = Role.sales();
        assertThat(sales.id()).isEqualTo(Role.SALES_ID).isNotEqualTo(Role.ADMINISTRATOR_ID);
        assertThat(sales.name()).isEqualTo("Comercial / Ventas");
        assertThat(sales.system()).isFalse();
        assertThat(sales.permissions())
                .containsExactlyInAnyOrder(
                        Permission.LEADS_VER_PROPIOS, Permission.LEADS_GESTIONAR);
    }

    private static Set<Permission> withNull() {
        Set<Permission> set = new HashSet<>(Arrays.asList(Permission.values()));
        set.add(null);
        return set;
    }

    @Test
    void failedReplaceLeavesAdministratorWithEveryPermission() {
        Role admin = Role.administrator();
        assertViolation(() -> admin.replacePermissions(withNull()), "role.permissions_invalid");
        assertViolation(() -> admin.replacePermissions(null), "role.permissions_invalid");
        assertThat(admin.permissions()).containsExactlyInAnyOrder(Permission.values());
    }

    @Test
    void failedReplaceLeavesCustomRoleUnchanged() {
        Role role = Role.create("A", null, Set.of(Permission.ROLES_VER));
        assertViolation(() -> role.replacePermissions(null), "role.permissions_invalid");
        assertViolation(() -> role.replacePermissions(withNull()), "role.permissions_invalid");
        assertThat(role.permissions()).containsExactly(Permission.ROLES_VER);
    }

    @Test
    void duplicateGrantAndRevokeArgumentsAreIdempotent() {
        Role role = Role.create("A", null, Set.of());
        role.grant(Permission.ROLES_VER, Permission.ROLES_VER);
        assertThat(role.permissions()).containsExactly(Permission.ROLES_VER);
        role.revoke(Permission.ROLES_VER, Permission.ROLES_VER);
        assertThat(role.permissions()).isEmpty();
    }

    @Test
    void nullGrantAndRevokeAreRejectedWithoutMutation() {
        Role role = Role.create("A", null, Set.of(Permission.ROLES_VER));
        assertViolation(() -> role.grant((Permission[]) null), "role.permissions_invalid");
        assertViolation(
                () -> role.grant(Permission.LEADS_ASIGNAR, null), "role.permissions_invalid");
        assertViolation(() -> role.revoke((Permission[]) null), "role.permissions_invalid");
        assertViolation(() -> role.revoke(Permission.ROLES_VER, null), "role.permissions_invalid");
        assertThat(role.permissions()).containsExactly(Permission.ROLES_VER);
    }

    @Test
    void createRejectsNullPermissionsAndAllowsNullDescription() {
        assertViolation(() -> Role.create("A", null, null), "role.permissions_invalid");
        assertViolation(() -> Role.create("A", null, withNull()), "role.permissions_invalid");
        assertThat(Role.create("A", null, Set.of()).description()).isNull();
    }
}
