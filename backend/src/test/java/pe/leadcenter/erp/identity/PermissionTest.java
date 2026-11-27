package pe.leadcenter.erp.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class PermissionTest {

    @Test
    void catalogHasExactlyTheAgreedCodes() {
        assertThat(Arrays.stream(Permission.values()).map(Permission::code))
                .containsExactlyInAnyOrder(
                        "usuarios.ver",
                        "usuarios.gestionar",
                        "roles.ver",
                        "roles.gestionar",
                        "leads.ver_propios",
                        "leads.ver_todos",
                        "leads.asignar",
                        "leads.gestionar");
    }

    @Test
    void fromCodeFindsValidCode() {
        assertThat(Permission.fromCode("leads.ver_todos")).isEqualTo(Permission.LEADS_VER_TODOS);
    }

    @Test
    void fromCodeRejectsUnknownCode() {
        assertThatThrownBy(() -> Permission.fromCode("nope.nada"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("Unknown permission code: nope.nada")
                .extracting("code")
                .isEqualTo("permission.unknown");
    }
}
