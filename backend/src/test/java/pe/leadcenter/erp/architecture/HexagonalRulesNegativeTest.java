package pe.leadcenter.erp.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Proves each rule fails on a violation, using fixtures under {@code archfixture.badmodule}. */
class HexagonalRulesNegativeTest {

    private static final String ROOT = "archfixture";
    private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages(ROOT);

    @Test
    void domainDependingOnSpringJpaApplicationOrInfrastructureIsRejected() {
        assertThatThrownBy(() -> HexagonalRules.domainIsPure(ROOT).check(FIXTURES))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BadDomain")
                .hasMessageContaining("org.springframework")
                .hasMessageContaining("jakarta.persistence")
                .hasMessageContaining("BadHandler")
                .hasMessageContaining("BadAdapterOut");
    }

    @Test
    void domainDependingOnHibernateIsRejected() {
        assertThatThrownBy(() -> HexagonalRules.domainIsPure(ROOT).check(FIXTURES))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("HibernateDomain")
                .hasMessageContaining("org.hibernate");
    }

    @Test
    void applicationDependingOnInfrastructureIsRejected() {
        assertThatThrownBy(
                        () ->
                                HexagonalRules.applicationDoesNotDependOnInfrastructure(ROOT)
                                        .check(FIXTURES))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BadHandler")
                .hasMessageContaining("BadAdapterOut");
    }

    @Test
    void inboundDependingOnOutboundIsRejected() {
        assertThatThrownBy(
                        () ->
                                HexagonalRules.inboundAdaptersDoNotDependOnOutboundAdapters(ROOT)
                                        .check(FIXTURES))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("BadAdapterIn")
                .hasMessageContaining("BadAdapterOut");
    }

    @Test
    void rulesPassWhenNoClassMatches() {
        JavaClasses none = new ClassFileImporter().importPackages("pe.leadcenter.erp.nonexistent");
        HexagonalRules.domainIsPure("pe.leadcenter.erp.nonexistent").check(none);
    }
}
