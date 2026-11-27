package pe.leadcenter.erp.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;

class ValueObjectsTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        assertThat(new Email("  Ana@LeadCenter.PE ").value()).isEqualTo("ana@leadcenter.pe");
        assertThat(new Email("A@b.pe")).isEqualTo(new Email("a@B.pe"));
    }

    @Test
    void emailRejectsBadShapeAndLength() {
        for (String bad : new String[] {"", "  ", "no-at", "a@b", "a b@c.pe", "@c.pe"}) {
            assertThatThrownBy(() -> new Email(bad))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessage("Invalid email address")
                    .extracting("code")
                    .isEqualTo("user.email_invalid");
        }
        assertThatThrownBy(() -> new Email("a".repeat(250) + "@b.pe"))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void passwordHashIsNotLeakedByToString() {
        PasswordHash hash = new PasswordHash("$2a$10$secrethash");
        assertThat(hash.value()).isEqualTo("$2a$10$secrethash");
        assertThat(hash.toString()).doesNotContain("secrethash");
    }

    @Test
    void passwordHashRejectsBlank() {
        assertThatThrownBy(() -> new PasswordHash(" "))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("user.password_hash_blank");
        assertThatThrownBy(() -> new PasswordHash(null))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void emailLengthBoundaryIs254() {
        String at254 = "a".repeat(249) + "@b.pe";
        assertThat(at254).hasSize(254);
        assertThat(new Email(at254).value()).isEqualTo(at254);
        assertThatThrownBy(() -> new Email("a" + at254))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("code")
                .isEqualTo("user.email_invalid");
    }
}
