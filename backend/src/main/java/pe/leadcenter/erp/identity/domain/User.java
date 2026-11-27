package pe.leadcenter.erp.identity.domain;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.domain.UuidV7;

/** A person who can sign in. Cross-aggregate references (roles) are by id. */
public final class User {

    private static final int NAME_MAX = 150;

    private final UUID id;
    private Email email;
    private String fullName;
    private PasswordHash passwordHash;
    private boolean active;
    private final Set<UUID> roleIds = new HashSet<>();

    private User(Email email, String fullName, PasswordHash passwordHash) {
        this.id = UuidV7.generate();
        this.email = email;
        this.fullName = validName(fullName);
        this.passwordHash = passwordHash;
        this.active = true;
    }

    public UUID id() {
        return id;
    }

    public static User register(Email email, String fullName, PasswordHash passwordHash) {
        return new User(email, fullName, passwordHash);
    }

    public Email email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public boolean active() {
        return active;
    }

    public Set<UUID> roleIds() {
        return Set.copyOf(roleIds);
    }

    public void changeFullName(String newName) {
        this.fullName = validName(newName);
    }

    public void changeEmail(Email newEmail) {
        this.email = newEmail;
    }

    public void changePasswordHash(PasswordHash newHash) {
        this.passwordHash = newHash;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void assignRole(UUID roleId) {
        roleIds.add(roleId);
    }

    public void removeRole(UUID roleId) {
        roleIds.remove(roleId);
    }

    private static String validName(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX) {
            throw new BusinessRuleViolationException(
                    "user.name_invalid", "Full name must be 1 to " + NAME_MAX + " characters");
        }
        return trimmed;
    }
}
