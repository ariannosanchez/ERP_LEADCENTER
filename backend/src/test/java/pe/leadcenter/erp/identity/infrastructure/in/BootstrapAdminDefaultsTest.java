package pe.leadcenter.erp.identity.infrastructure.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/** application.yml must never ship default administrator credentials. */
class BootstrapAdminDefaultsTest {

    @Test
    void applicationYmlHasNoDefaultBootstrapCredentials() throws IOException {
        List<PropertySource<?>> sources =
                new YamlPropertySourceLoader()
                        .load("application", new ClassPathResource("application.yml"));
        Map<?, ?> yaml = (Map<?, ?>) sources.getFirst().getSource();

        assertThat(String.valueOf(yaml.get("erp.bootstrap.admin.email")))
                .isEqualTo("${ERP_BOOTSTRAP_ADMIN_EMAIL:}");
        assertThat(String.valueOf(yaml.get("erp.bootstrap.admin.password")))
                .isEqualTo("${ERP_BOOTSTRAP_ADMIN_PASSWORD:}");
    }
}
