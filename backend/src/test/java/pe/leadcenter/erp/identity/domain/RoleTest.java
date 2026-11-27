package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
