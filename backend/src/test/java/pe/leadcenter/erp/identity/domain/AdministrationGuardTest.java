package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class AdministrationGuardTest {

    private final Role admin = Role.administrator();
    private final User first = adminUser("a@b.pe");
    private final User second = adminUser("c@b.pe");

    private User adminUser(String email) {
        User user = User.register(new Email(email), "Admin", new PasswordHash("h"));
        user.assignRole(admin.id());
        return user;
    }

    private void verify(List<User> users, Role... roles) {
        AdministrationGuard.verify(users, List.of(roles));
    }

    private void assertRejected(List<User> users, Role... roles) {
        assertThatThrownBy(() -> verify(users, roles))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("At least one active user must keep user and role administration")
                .extracting("code")
                .isEqualTo("identity.last_administrator");
    }

    @Test
    void acceptsActiveAdministrator() {
        assertThatCode(() -> verify(List.of(first), admin)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDeactivatingLastAdministrator() {
        first.deactivate();
        assertRejected(List.of(first), admin);
    }

    @Test
    void allowsDeactivatingWhenAnotherAdministratorIsActive() {
        first.deactivate();
        assertThatCode(() -> verify(List.of(first, second), admin)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRemovingRoleFromLastAdministrator() {
        first.removeRole(admin.id());
        assertRejected(List.of(first), admin);
    }

    @Test
    void allowsRemovingRoleWhenAnotherAdministratorIsActive() {
        first.removeRole(admin.id());
        assertThatCode(() -> verify(List.of(first, second), admin)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRevokingAdministrationPermissionsFromLastAdministratorRole() {
        Role custom = Role.create("Gestores", null, Set.of(Permission.values()));
        User user = User.register(new Email("x@b.pe"), "X", new PasswordHash("h"));
        user.assignRole(custom.id());
        custom.revoke(Permission.ROLES_GESTIONAR);
        assertRejected(List.of(user), custom);
    }

    @Test
    void permissionsMayComeFromTwoRolesCombined() {
        Role users = Role.create("U", null, Set.of(Permission.USUARIOS_GESTIONAR));
        Role roles = Role.create("R", null, Set.of(Permission.ROLES_GESTIONAR));
        User user = User.register(new Email("x@b.pe"), "X", new PasswordHash("h"));
        user.assignRole(users.id());
        user.assignRole(roles.id());
        assertThatCode(() -> verify(List.of(user), users, roles)).doesNotThrowAnyException();
    }

    private static void assertViolation(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo(code);
    }

    private User holding(Role... roles) {
        User user = User.register(new Email("x@b.pe"), "X", new PasswordHash("h"));
        for (Role role : roles) {
            user.assignRole(role.id());
        }
        return user;
    }

    @Test
    void rejectsUserHoldingOnlyRolesGestionar() {
        Role only = Role.create("R", null, Set.of(Permission.ROLES_GESTIONAR));
        assertRejected(List.of(holding(only)), only);
    }

    @Test
    void rejectsWhenAdministratorRoleIsMissingFromSuppliedRoles() {
        Role other = Role.create("O", null, Set.of());
        assertRejected(List.of(first), other);
    }

    @Test
    void rejectsEmptyStateAndInactiveOnlyAdministrators() {
        assertRejected(List.of(), admin);
        assertRejected(List.of(first), new Role[0]);
        first.deactivate();
        second.deactivate();
        assertRejected(List.of(first, second), admin);
    }

    @Test
    void rejectsAdministrationPermissionsSplitAcrossUsers() {
        Role users = Role.create("U", null, Set.of(Permission.USUARIOS_GESTIONAR));
        Role roles = Role.create("R", null, Set.of(Permission.ROLES_GESTIONAR));
        assertRejected(List.of(holding(users), holding(roles)), users, roles);
    }

    @Test
    void rejectsDuplicateRoleIdsWithDomainException() {
        assertViolation(
                () -> AdministrationGuard.verify(List.of(first), List.of(admin, admin)),
                "identity.role_duplicated");
    }

    @Test
    void rejectsNullCollectionsAndElements() {
        assertViolation(
                () -> AdministrationGuard.verify(null, List.of(admin)),
                "identity.guard_input_required");
        assertViolation(
                () -> AdministrationGuard.verify(List.of(first), null),
                "identity.guard_input_required");
        assertViolation(
                () -> AdministrationGuard.verify(Arrays.asList(first, null), List.of(admin)),
                "identity.guard_input_required");
        assertViolation(
                () -> AdministrationGuard.verify(List.of(first), Arrays.asList(admin, null)),
                "identity.guard_input_required");
    }
}
