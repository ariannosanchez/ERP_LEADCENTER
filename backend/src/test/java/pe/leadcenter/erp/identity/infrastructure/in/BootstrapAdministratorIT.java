package pe.leadcenter.erp.identity.infrastructure.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.leadcenter.erp.TestcontainersConfiguration;
import pe.leadcenter.erp.identity.application.bootstrapadministrator.BootstrapAdministratorCommand;
import pe.leadcenter.erp.identity.application.bootstrapadministrator.BootstrapAdministratorHandler;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.Role;
import pe.leadcenter.erp.identity.domain.User;
import pe.leadcenter.erp.identity.domain.port.out.PasswordHasher;
import pe.leadcenter.erp.identity.domain.port.out.UserRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "erp.bootstrap.admin.email=Root@LeadCenter.pe",
            "erp.bootstrap.admin.password=Corre-Horse-Battery-9"
        })
class BootstrapAdministratorIT {

    private static final String PASSWORD = "Corre-Horse-Battery-9";

    @Autowired UserRepository users;
    @Autowired PasswordHasher hasher;
    @Autowired PasswordEncoder encoder;
    @Autowired BootstrapAdministratorHandler handler;

    @Test
    void startupCreatesExactlyOneAdministratorAndRerunCreatesNothing() {
        List<User> admins = administrators();
        assertThat(admins).hasSize(1);
        User admin = admins.getFirst();
        assertThat(admin.email()).isEqualTo(new Email("root@leadcenter.pe"));
        assertThat(admin.active()).isTrue();
        assertThat(admin.fullName()).isEqualTo("Administrador");
        assertThat(admin.passwordHash().value()).isNotEqualTo(PASSWORD).startsWith("{bcrypt}");
        assertThat(hasher.matches(PASSWORD, admin.passwordHash())).isTrue();
        assertThat(hasher.matches("wrong-password-123", admin.passwordHash())).isFalse();
        assertThat(encoder.matches(PASSWORD, admin.passwordHash().value())).isTrue();

        handler.execute(new BootstrapAdministratorCommand("other@leadcenter.pe", PASSWORD, "X"));

        assertThat(users.findAll()).hasSize(1);
        assertThat(administrators()).hasSize(1);
        assertThat(users.existsByEmail(new Email("other@leadcenter.pe"))).isFalse();
    }

    private List<User> administrators() {
        return users.findAll().stream()
                .filter(u -> u.roleIds().contains(Role.ADMINISTRATOR_ID))
                .toList();
    }
}
