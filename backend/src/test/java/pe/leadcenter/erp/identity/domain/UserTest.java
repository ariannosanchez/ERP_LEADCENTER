package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class UserTest {

    private static User user() {
        return User.register(new Email("a@b.pe"), "Ana Pérez", new PasswordHash("h1"));
    }

    @Test
    void registerCreatesActiveUserWithoutRoles() {
        User user = user();
        assertThat(user.id()).isNotNull();
        assertThat(user.active()).isTrue();
        assertThat(user.roleIds()).isEmpty();
        assertThat(user.fullName()).isEqualTo("Ana Pérez");
    }

    @Test
    void fullNameMustBeNonBlankAndBounded() {
        assertThatThrownBy(() -> User.register(new Email("a@b.pe"), " ", new PasswordHash("h")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("user.name_invalid");
        User user = user();
        assertThatThrownBy(() -> user.changeFullName("x".repeat(151)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void profileAndCredentialsCanChange() {
        User user = user();
        user.changeFullName("  Luis ");
        user.changeEmail(new Email("L@b.pe"));
        user.changePasswordHash(new PasswordHash("h2"));
        assertThat(user.fullName()).isEqualTo("Luis");
        assertThat(user.email().value()).isEqualTo("l@b.pe");
        assertThat(user.passwordHash().value()).isEqualTo("h2");
    }

    @Test
    void deactivateAndActivate() {
        User user = user();
        user.deactivate();
        assertThat(user.active()).isFalse();
        user.activate();
        assertThat(user.active()).isTrue();
    }

    @Test
    void rolesCanBeAssignedAndRemoved() {
        User user = user();
        UUID role = UUID.randomUUID();
        user.assignRole(role);
        user.assignRole(role);
        assertThat(user.roleIds()).containsExactly(role);
        user.removeRole(role);
        assertThat(user.roleIds()).isEmpty();
    }
}
