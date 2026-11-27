package pe.leadcenter.erp.identity.application.bootstrapadministrator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.leadcenter.erp.identity.domain.AdministrationGuard;
import pe.leadcenter.erp.identity.domain.Email;
import pe.leadcenter.erp.identity.domain.Role;
import pe.leadcenter.erp.identity.domain.User;
import pe.leadcenter.erp.identity.domain.port.out.PasswordHasher;
import pe.leadcenter.erp.identity.domain.port.out.RoleRepository;
import pe.leadcenter.erp.identity.domain.port.out.UserRepository;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.domain.DomainException;

/**
 * Creates the initial Administrador from configuration when no active administrator exists.
 * Idempotent; never logs the raw password.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BootstrapAdministratorHandler {

    static final int MIN_PASSWORD_LENGTH = 12;
    // bcrypt ignores (or rejects) anything past 72 bytes.
    static final int MAX_PASSWORD_BYTES = 72;
    private static final String DEFAULT_NAME = "Administrador";
    private static final Logger LOG = LoggerFactory.getLogger(BootstrapAdministratorHandler.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordHasher hasher;

    public void execute(BootstrapAdministratorCommand command) {
        List<User> existing = users.findAll();
        List<Role> allRoles = roles.findAll();
        if (administratorExists(existing, allRoles)) {
            LOG.info("Bootstrap administrator skipped: administrator already present");
            return;
        }
        if (isBlank(command.email())
                || command.password() == null
                || command.password().isEmpty()) {
            LOG.warn(
                    "No administrator exists and no initial one is configured. Set"
                            + " ERP_BOOTSTRAP_ADMIN_EMAIL and ERP_BOOTSTRAP_ADMIN_PASSWORD"
                            + " (min {} characters) and restart to create it.",
                    MIN_PASSWORD_LENGTH);
            return;
        }
        Email email = validEmail(command.email());
        String password = validPassword(command.password());
        if (users.existsByEmail(email)) {
            throw new BusinessRuleViolationException(
                    "bootstrap.admin_email_conflict",
                    "A user with email "
                            + email.value()
                            + " already exists but is not an active administrator; resolve it"
                            + " manually (reactivate it, grant the Administrador role, or use"
                            + " another ERP_BOOTSTRAP_ADMIN_EMAIL)");
        }
        User admin =
                User.register(
                        email,
                        isBlank(command.fullName()) ? DEFAULT_NAME : command.fullName(),
                        hasher.hash(password));
        admin.assignRole(Role.ADMINISTRATOR_ID);
        List<User> after = new ArrayList<>(existing);
        after.add(admin);
        AdministrationGuard.verify(after, allRoles);
        users.save(admin);
        LOG.info("Bootstrap administrator created for {}", email.value());
    }

    private static boolean administratorExists(List<User> existing, List<Role> allRoles) {
        try {
            AdministrationGuard.verify(existing, allRoles);
            return true;
        } catch (DomainException e) {
            if ("identity.last_administrator".equals(e.code())) {
                return false;
            }
            throw e;
        }
    }

    private static Email validEmail(String raw) {
        try {
            return new Email(raw);
        } catch (BusinessRuleViolationException e) {
            throw new BusinessRuleViolationException(
                    "bootstrap.admin_email_invalid",
                    "ERP_BOOTSTRAP_ADMIN_EMAIL is not a valid email address");
        }
    }

    private static String validPassword(String raw) {
        if (raw.isBlank()
                || raw.codePointCount(0, raw.length()) < MIN_PASSWORD_LENGTH
                || raw.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BusinessRuleViolationException(
                    "bootstrap.admin_password_invalid",
                    "ERP_BOOTSTRAP_ADMIN_PASSWORD must have at least "
                            + MIN_PASSWORD_LENGTH
                            + " characters and at most "
                            + MAX_PASSWORD_BYTES
                            + " bytes");
        }
        return raw;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
