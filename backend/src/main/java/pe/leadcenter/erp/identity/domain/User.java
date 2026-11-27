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

    private User(UUID id, Email email, String fullName, PasswordHash passwordHash) {
        this.id = required(id, "user.id_required", "User id is required");
        this.email = requireEmail(email);
        this.fullName = validName(fullName);
        this.passwordHash = requireHash(passwordHash);
        this.active = true;
    }

    public UUID id() {
        return id;
    }

    public static User register(Email email, String fullName, PasswordHash passwordHash) {
        return new User(UuidV7.generate(), email, fullName, passwordHash);
    }

    /** Rebuilds a stored user: same invariants as registration, no creation side effects. */
    public static User restore(
            UUID id,
            Email email,
            String fullName,
            PasswordHash passwordHash,
            boolean active,
            Set<UUID> roleIds) {
        User user = new User(id, email, fullName, passwordHash);
        user.active = active;
        required(roleIds, "user.role_required", "Role ids are required").forEach(user::assignRole);
        return user;
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
        this.email = requireEmail(newEmail);
    }

    public void changePasswordHash(PasswordHash newHash) {
        this.passwordHash = requireHash(newHash);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void assignRole(UUID roleId) {
        roleIds.add(requireRole(roleId));
    }

    public void removeRole(UUID roleId) {
        roleIds.remove(requireRole(roleId));
    }

    private static Email requireEmail(Email email) {
        return required(email, "user.email_required", "Email is required");
    }

    private static PasswordHash requireHash(PasswordHash hash) {
        return required(hash, "user.password_hash_required", "Password hash is required");
    }

    private static UUID requireRole(UUID roleId) {
        return required(roleId, "user.role_required", "Role id is required");
    }

    private static <T> T required(T value, String code, String message) {
        if (value == null) {
            throw new BusinessRuleViolationException(code, message);
        }
        return value;
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
