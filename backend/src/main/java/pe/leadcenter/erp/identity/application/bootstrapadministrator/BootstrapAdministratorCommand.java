package pe.leadcenter.erp.identity.application.bootstrapadministrator;

/** Raw configuration values; {@code email} and {@code password} may be null or blank (unset). */
public record BootstrapAdministratorCommand(String email, String password, String fullName) {

    @Override
    public String toString() {
        return "BootstrapAdministratorCommand[email=" + email + ", password=****]";
    }
}
