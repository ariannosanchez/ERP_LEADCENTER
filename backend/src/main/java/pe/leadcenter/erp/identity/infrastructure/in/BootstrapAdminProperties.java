package pe.leadcenter.erp.identity.infrastructure.in;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** Initial administrator settings ({@code erp.bootstrap.admin.*}); no default credentials. */
@Validated
@ConfigurationProperties("erp.bootstrap.admin")
public record BootstrapAdminProperties(
        String email,
        String password,
        @DefaultValue("Administrador") @Size(max = 150) String fullName) {

    @Override
    public String toString() {
        return "BootstrapAdminProperties[email=" + email + ", password=****]";
    }
}
