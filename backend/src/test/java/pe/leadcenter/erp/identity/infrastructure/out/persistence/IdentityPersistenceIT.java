package pe.leadcenter.erp.identity.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import pe.leadcenter.erp.TestcontainersConfiguration;
import pe.leadcenter.erp.identity.Permission;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.PasswordHash;
import pe.leadcenter.erp.identity.domain.Role;
import pe.leadcenter.erp.identity.domain.User;
import pe.leadcenter.erp.identity.domain.port.out.RoleRepository;
import pe.leadcenter.erp.identity.domain.port.out.UserRepository;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.security.AuthenticatedUser;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class IdentityPersistenceIT {

    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired RoleJpaRepository roleJpa;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static User newUser(String email) {
        return User.register(new Email(email), "Ana Pérez", new PasswordHash("$2a$10$hash"));
    }

    @Test
    void seededRolesExistWithWellKnownIdsAndExactPermissions() {
        Role admin = roles.findById(Role.ADMINISTRATOR_ID).orElseThrow();
        Role sales = roles.findById(Role.SALES_ID).orElseThrow();

        assertThat(admin.name()).isEqualTo("Administrador");
        assertThat(admin.system()).isTrue();
        assertThat(admin.permissions())
                .containsExactlyInAnyOrderElementsOf(EnumSet.allOf(Permission.class));
        assertThat(sales.name()).isEqualTo("Comercial / Ventas");
        assertThat(sales.system()).isFalse();
        assertThat(sales.permissions())
                .containsExactlyInAnyOrder(
                        Permission.LEADS_VER_PROPIOS, Permission.LEADS_GESTIONAR);
        assertThat(
                        jdbc.queryForObject(
                                "select created_by from roles where id = ?",
                                UUID.class,
                                Role.SALES_ID))
                .isNull();
    }

    @Test
    void roleRoundTripsAllFields() {
        Role role =
                Role.create(
                        "Gerencia " + unique(),
                        "Ve todo",
                        Set.of(Permission.LEADS_VER_TODOS, Permission.ROLES_VER));
        roles.save(role);

        Role loaded = roles.findById(role.id()).orElseThrow();
        assertThat(loaded.id()).isEqualTo(role.id());
        assertThat(loaded.name()).isEqualTo(role.name());
        assertThat(loaded.description()).isEqualTo("Ve todo");
        assertThat(loaded.system()).isFalse();
        assertThat(loaded.permissions()).isEqualTo(role.permissions());
        assertThat(roles.findAllById(List.of(role.id()))).hasSize(1);
        assertThat(roles.findAll()).extracting(Role::id).contains(Role.ADMINISTRATOR_ID, role.id());
    }

    @Test
    void userRoundTripsAllFieldsAndSurvivesUpdates() {
        User user = newUser("Ana." + unique() + "@Example.com");
        user.assignRole(Role.ADMINISTRATOR_ID);
        user.assignRole(Role.SALES_ID);
        users.save(user);

        User loaded = users.findById(user.id()).orElseThrow();
        assertThat(loaded.email()).isEqualTo(user.email());
        assertThat(loaded.fullName()).isEqualTo("Ana Pérez");
        assertThat(loaded.passwordHash()).isEqualTo(new PasswordHash("$2a$10$hash"));
        assertThat(loaded.active()).isTrue();
        assertThat(loaded.roleIds())
                .containsExactlyInAnyOrder(Role.ADMINISTRATOR_ID, Role.SALES_ID);
        assertThat(users.findByEmail(new Email(user.email().value().toUpperCase(Locale.ROOT))))
                .isPresent();
        assertThat(users.existsByEmail(user.email())).isTrue();

        loaded.deactivate();
        loaded.removeRole(Role.ADMINISTRATOR_ID);
        loaded.changeFullName("Ana Gómez");
        users.save(loaded);

        User updated = users.findById(user.id()).orElseThrow();
        assertThat(updated.active()).isFalse();
        assertThat(updated.fullName()).isEqualTo("Ana Gómez");
        assertThat(updated.roleIds()).containsExactly(Role.SALES_ID);
        assertThat(users.findAll()).extracting(User::id).contains(user.id());
    }

    @Test
    void duplicateEmailIsRejectedAndLeavesTheExistingRowUnchanged() {
        String local = "dup." + unique();
        User original = users.save(newUser(local + "@example.com"));

        User clash =
                User.register(
                        new Email(local.toUpperCase(Locale.ROOT) + "@EXAMPLE.com"),
                        "Otro",
                        new PasswordHash("x"));
        assertThatThrownBy(() -> users.save(clash))
                .isInstanceOf(DataIntegrityViolationException.class);
        // Raw non-normalized variant is rejected by the database as well.
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into users (id, email, full_name, password_hash,"
                                                + " created_at, updated_at)"
                                                + " values (?, ?, 'x', 'x', now(), now())",
                                        UUID.randomUUID(),
                                        local.toUpperCase(Locale.ROOT) + "@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from users where lower(email) = ?",
                                Integer.class,
                                original.email().value()))
                .isEqualTo(1);
        assertThat(users.findByEmail(original.email()).orElseThrow().fullName())
                .isEqualTo("Ana Pérez");
        assertThat(users.findById(clash.id())).isEmpty();
    }

    @Test
    void duplicateRoleNameIsRejectedCaseInsensitivelyAndLeavesTheExistingRowUnchanged() {
        String name = "Soporte " + unique();
        Role original = roles.save(Role.create(name, "orig", Set.of(Permission.USUARIOS_VER)));

        Role clash =
                Role.create(name.toUpperCase(Locale.ROOT), "otro", Set.of(Permission.ROLES_VER));
        assertThatThrownBy(() -> roles.save(clash))
                .isInstanceOf(DataIntegrityViolationException.class);

        Role loaded = roles.findById(original.id()).orElseThrow();
        assertThat(loaded.name()).isEqualTo(name);
        assertThat(loaded.description()).isEqualTo("orig");
        assertThat(loaded.permissions()).containsExactly(Permission.USUARIOS_VER);
        assertThat(roles.findById(clash.id())).isEmpty();
    }

    @Test
    void staleUpdateFailsWithOptimisticLockAndKeepsTheFirstWrite() {
        Role role =
                roles.save(Role.create("Lock " + unique(), null, Set.of(Permission.USUARIOS_VER)));
        RoleEntity first = tx.execute(s -> roleJpa.findById(role.id()).orElseThrow());
        RoleEntity stale = tx.execute(s -> roleJpa.findById(role.id()).orElseThrow());

        first.setDescription("first");
        roleJpa.saveAndFlush(first);
        stale.setDescription("stale");

        assertThatThrownBy(() -> roleJpa.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(
                        jdbc.queryForObject(
                                "select description from roles where id = ?",
                                String.class,
                                role.id()))
                .isEqualTo("first");
        assertThat(
                        jdbc.queryForObject(
                                "select version from roles where id = ?", Long.class, role.id()))
                .isEqualTo(1L);
    }

    @Test
    void changingRolePermissionsCreatesHistoryWithTheAuthenticatedUser() {
        UUID actor = UUID.randomUUID();
        AuthenticatedUser principal = () -> actor;
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, "n/a", "ROLE_X"));
        Role role =
                roles.save(
                        Role.create(
                                "Hist " + unique(),
                                null,
                                Set.of(Permission.USUARIOS_VER, Permission.ROLES_VER)));
        role.replacePermissions(Set.of(Permission.USUARIOS_VER, Permission.LEADS_ASIGNAR));
        roles.save(role);

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from roles_aud where id = ?",
                                Integer.class,
                                role.id()))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from role_permissions_aud where role_id = ?"
                                        + " and revtype = 2 and permission_code = 'roles.ver'",
                                Integer.class,
                                role.id()))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForList(
                                "select distinct r.modified_by from roles_aud a"
                                        + " join revinfo r on r.rev = a.rev where a.id = ?",
                                UUID.class,
                                role.id()))
                .containsExactly(actor);
    }

    @Test
    void unknownStoredPermissionCodeFailsLoudlyOnLoad() {
        UUID id = UUID.randomUUID();
        tx.executeWithoutResult(
                status -> {
                    // DDL is transactional in PostgreSQL: everything below is rolled back.
                    jdbc.execute(
                            "alter table role_permissions"
                                    + " drop constraint role_permissions_code_check");
                    jdbc.update(
                            "insert into roles (id, name, system, version, created_at, updated_at)"
                                    + " values (?, ?, false, 0, now(), now())",
                            id,
                            "Corrupto " + unique());
                    jdbc.update("insert into role_permissions values (?, 'bogus.code')", id);

                    assertThatThrownBy(() -> roles.findById(id))
                            .isInstanceOf(BusinessRuleViolationException.class)
                            .hasMessageContaining("bogus.code");
                    status.setRollbackOnly();
                });

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from roles where id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void passwordHashesAreNotKeptInHistory() {
        User user = users.save(newUser("hist." + unique() + "@example.com"));
        user.changePasswordHash(new PasswordHash("$2a$10$changed"));
        users.save(user);

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from information_schema.columns"
                                        + " where table_name = 'users_aud'"
                                        + " and column_name = 'password_hash'",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select password_hash from users where id = ?",
                                String.class,
                                user.id()))
                .isEqualTo("$2a$10$changed");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from users_aud where id = ?",
                                Integer.class,
                                user.id()))
                .isEqualTo(2);
    }

    @Test
    void databaseRejectsCodesOutsideTheCatalog() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into role_permissions values (?, 'leads.borrar')",
                                        Role.SALES_ID))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("role_permissions_code_check");
    }

    @Test
    void storedAdministratorMissingAPermissionFailsLoudlyOnLoad() {
        tx.executeWithoutResult(
                status -> {
                    jdbc.update(
                            "delete from role_permissions where role_id = ?"
                                    + " and permission_code = 'roles.gestionar'",
                            Role.ADMINISTRATOR_ID);

                    assertThatThrownBy(() -> roles.findById(Role.ADMINISTRATOR_ID))
                            .isInstanceOf(BusinessRuleViolationException.class)
                            .extracting("code")
                            .isEqualTo("role.system_protected");
                    status.setRollbackOnly();
                });

        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from role_permissions where role_id = ?",
                                Integer.class,
                                Role.ADMINISTRATOR_ID))
                .isEqualTo(Permission.values().length);
    }

    @Test
    void userRoleChangesCreateHistoryWithTheAuthenticatedUser() {
        UUID actor = UUID.randomUUID();
        AuthenticatedUser principal = () -> actor;
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, "n/a", "ROLE_X"));
        User user = newUser("roles." + unique() + "@example.com");
        user.assignRole(Role.SALES_ID);
        users.save(user);
        user.removeRole(Role.SALES_ID);
        user.assignRole(Role.ADMINISTRATOR_ID);
        users.save(user);

        List<String> rows =
                jdbc.query(
                        "select revtype, role_id from user_roles_aud where user_id = ?"
                                + " order by rev, revtype, role_id",
                        (rs, i) -> rs.getInt("revtype") + ":" + rs.getObject("role_id", UUID.class),
                        user.id());
        assertThat(rows)
                .containsExactly(
                        "0:" + Role.SALES_ID, "0:" + Role.ADMINISTRATOR_ID, "2:" + Role.SALES_ID);
        assertThat(
                        jdbc.queryForList(
                                "select distinct r.modified_by from user_roles_aud a"
                                        + " join revinfo r on r.rev = a.rev where a.user_id = ?",
                                UUID.class,
                                user.id()))
                .containsExactly(actor);
    }

    @Test
    void historyTablesReferenceRevinfo() {
        for (String table :
                List.of("roles_aud", "role_permissions_aud", "users_aud", "user_roles_aud")) {
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from information_schema.table_constraints tc"
                                            + " join information_schema.constraint_column_usage cu"
                                            + " on cu.constraint_name = tc.constraint_name"
                                            + " where tc.table_name = ?"
                                            + " and tc.constraint_type = 'FOREIGN KEY'"
                                            + " and cu.table_name = 'revinfo'",
                                    Integer.class,
                                    table))
                    .as(table)
                    .isEqualTo(1);
        }
    }

    @Test
    void seedHasExactlyTwoRolesAndTenPermissionRows() {
        assertThat(
                        jdbc.queryForList(
                                "select permission_code from role_permissions where role_id = ?",
                                String.class,
                                Role.SALES_ID))
                .containsExactlyInAnyOrder("leads.gestionar", "leads.ver_propios");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from role_permissions where role_id = ?",
                                Integer.class,
                                Role.ADMINISTRATOR_ID))
                .isEqualTo(Permission.values().length);
        assertThat(jdbc.queryForList("select id from roles where system = true", UUID.class))
                .containsExactly(Role.ADMINISTRATOR_ID);
    }

    @Test
    void emailNormalizationCheckRejectsSurroundingSpaces() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into users (id, email, full_name, password_hash,"
                                                + " created_at, updated_at)"
                                                + " values (?, ?, 'x', 'x', now(), now())",
                                        UUID.randomUUID(),
                                        " sp." + unique() + "@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_email_normalized_check");
    }
}
