package pe.leadcenter.erp.identity.application.bootstrapadministrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.PasswordHash;
import pe.leadcenter.erp.identity.domain.Role;
import pe.leadcenter.erp.identity.domain.User;
import pe.leadcenter.erp.identity.domain.port.out.PasswordHasher;
import pe.leadcenter.erp.identity.domain.port.out.RoleRepository;
import pe.leadcenter.erp.identity.domain.port.out.UserRepository;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class BootstrapAdministratorHandlerTest {

    private static final String PASSWORD = "S3cret-Passw0rd!";
    private static final String EMAIL = "admin@leadcenter.pe";

    private final FakeUsers users = new FakeUsers();
    private final PasswordHasher hasher =
            new PasswordHasher() {
                @Override
                public PasswordHash hash(String raw) {
                    return new PasswordHash("hashed:" + raw.length());
                }

                @Override
                public boolean matches(String raw, PasswordHash hash) {
                    return hash(raw).equals(hash);
                }
            };
    private final BootstrapAdministratorHandler handler =
            new BootstrapAdministratorHandler(users, new FakeRoles(), hasher);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger =
            (Logger) LoggerFactory.getLogger(BootstrapAdministratorHandler.class);

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        logger.detachAppender(logs);
    }

    private static BootstrapAdministratorCommand command(String email, String password) {
        return new BootstrapAdministratorCommand(email, password, "Administrador");
    }

    private String allLogs() {
        return String.join(
                "\n", logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private static User userWith(String email, UUID roleId, boolean active) {
        User user = User.register(new Email(email), "Alguien", new PasswordHash("h"));
        user.assignRole(roleId);
        if (!active) {
            user.deactivate();
        }
        return user;
    }

    @Test
    void doesNothingWhenAnActiveAdministratorExists() {
        users.save(userWith("boss@leadcenter.pe", Role.ADMINISTRATOR_ID, true));

        handler.execute(command(EMAIL, PASSWORD));

        assertThat(users.all).hasSize(1);
        assertThat(logs.list)
                .anyMatch(
                        e ->
                                e.getLevel() == Level.INFO
                                        && e.getFormattedMessage()
                                                .contains("administrator already present"));
    }

    @Test
    void createsTheActiveAdministratorFromTheCommand() {
        handler.execute(
                new BootstrapAdministratorCommand(" Admin@LeadCenter.pe ", PASSWORD, "Jefa"));

        assertThat(users.all).hasSize(1);
        User created = users.all.getFirst();
        assertThat(created.email().value()).isEqualTo(EMAIL);
        assertThat(created.fullName()).isEqualTo("Jefa");
        assertThat(created.active()).isTrue();
        assertThat(created.roleIds()).containsExactly(Role.ADMINISTRATOR_ID);
        assertThat(created.passwordHash()).isEqualTo(hasher.hash(PASSWORD));
        assertThat(allLogs()).contains(EMAIL).doesNotContain(PASSWORD);
    }

    @Test
    void isIdempotentWhenRunTwice() {
        handler.execute(command(EMAIL, PASSWORD));
        handler.execute(command(EMAIL, PASSWORD));

        assertThat(users.all).hasSize(1);
    }

    @Test
    void failsWithoutChangesWhenTheEmailBelongsToANonAdministrator() {
        User sales = users.save(userWith(EMAIL, Role.SALES_ID, true));

        assertThatThrownBy(() -> handler.execute(command(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining(EMAIL)
                .hasMessageContaining("resolve")
                .hasMessageNotContaining(PASSWORD);
        assertThat(users.all).containsExactly(sales);
        assertThat(sales.roleIds()).containsExactly(Role.SALES_ID);
        assertThat(allLogs()).doesNotContain(PASSWORD);
    }

    @Test
    void failsWhenTheEmailBelongsToADeactivatedAdministrator() {
        User inactive = users.save(userWith(EMAIL, Role.ADMINISTRATOR_ID, false));

        assertThatThrownBy(() -> handler.execute(command(EMAIL, PASSWORD)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("resolve");
        assertThat(users.all).containsExactly(inactive);
        assertThat(inactive.active()).isFalse();
    }

    @Test
    void warnsAndCreatesNothingWhenVariablesAreMissing() {
        handler.execute(command(null, null));
        handler.execute(command(" ", PASSWORD));
        handler.execute(command(EMAIL, ""));

        assertThat(users.all).isEmpty();
        assertThat(logs.list)
                .filteredOn(e -> e.getLevel() == Level.WARN)
                .hasSize(3)
                .allMatch(
                        e ->
                                e.getFormattedMessage().contains("ERP_BOOTSTRAP_ADMIN_EMAIL")
                                        && e.getFormattedMessage()
                                                .contains("ERP_BOOTSTRAP_ADMIN_PASSWORD"));
        assertThat(allLogs()).doesNotContain(PASSWORD);
    }

    @Test
    void failsOnInvalidEmailWithoutCreatingAUser() {
        assertThatThrownBy(() -> handler.execute(command("not-an-email", PASSWORD)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("ERP_BOOTSTRAP_ADMIN_EMAIL")
                .hasMessageNotContaining(PASSWORD);
        assertThat(users.all).isEmpty();
    }

    @Test
    void failsOnShortBlankOrOverlongPasswordWithoutCreatingAUser() {
        for (String bad : List.of("short-11-ch", "            ", "é".repeat(37))) {
            assertThatThrownBy(() -> handler.execute(command(EMAIL, bad)))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("ERP_BOOTSTRAP_ADMIN_PASSWORD");
        }
        assertThat(users.all).isEmpty();
    }

    @Test
    void acceptsAPasswordOfExactlyTwelveCharacters() {
        handler.execute(command(EMAIL, "twelve-chars"));

        assertThat(users.all).hasSize(1);
    }

    @Test
    void usesTheDefaultNameWhenNoneIsGiven() {
        handler.execute(new BootstrapAdministratorCommand(EMAIL, PASSWORD, null));

        assertThat(users.all.getFirst().fullName()).isEqualTo("Administrador");
    }

    static final class FakeUsers implements UserRepository {
        final List<User> all = new ArrayList<>();

        @Override
        public Optional<User> findById(UUID id) {
            return all.stream().filter(u -> u.id().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(Email email) {
            return all.stream().filter(u -> u.email().equals(email)).findFirst();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(all);
        }

        @Override
        public User save(User user) {
            all.removeIf(u -> u.id().equals(user.id()));
            all.add(user);
            return user;
        }
    }

    static final class FakeRoles implements RoleRepository {
        private final List<Role> seeded =
                List.of(
                        Role.restore(
                                Role.ADMINISTRATOR_ID,
                                "Administrador",
                                null,
                                true,
                                EnumSet.allOf(Permission.class)),
                        Role.restore(
                                Role.SALES_ID,
                                "Comercial / Ventas",
                                null,
                                false,
                                Set.of(Permission.LEADS_VER_PROPIOS, Permission.LEADS_GESTIONAR)));

        @Override
        public Optional<Role> findById(UUID id) {
            return seeded.stream().filter(r -> r.id().equals(id)).findFirst();
        }

        @Override
        public List<Role> findAllById(Collection<UUID> ids) {
            return seeded.stream().filter(r -> ids.contains(r.id())).toList();
        }

        @Override
        public List<Role> findAll() {
            return seeded;
        }

        @Override
        public Role save(Role role) {
            return role;
        }
    }
}
