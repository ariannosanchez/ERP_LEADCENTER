package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
