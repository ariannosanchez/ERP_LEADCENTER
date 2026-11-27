package pe.leadcenter.erp.identity.infrastructure.in;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BootstrapAdminPropertiesTest {

    @Test
    void toStringDoesNotContainThePassword() {
        String password = "S3cret-Passw0rd!";

        String text =
                new BootstrapAdminProperties("admin@leadcenter.pe", password, "Administrador")
                        .toString();

        assertThat(text).doesNotContain(password).contains("****").contains("admin@leadcenter.pe");
    }
}
