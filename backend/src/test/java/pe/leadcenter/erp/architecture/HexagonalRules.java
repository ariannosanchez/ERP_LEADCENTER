package pe.leadcenter.erp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Hexagonal layering rules for every module under {@code <root>.<module>}. The root package is a
 * parameter so the rules can be proven to fail against fixtures outside the real application.
 */
final class HexagonalRules {

    private HexagonalRules() {}

    static ArchRule domainIsPure(String root) {
        return noClasses()
                .that()
                .resideInAPackage(root + ".*.domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        root + ".*.application..",
                        root + ".*.infrastructure..")
                .as(
                        "domain must not depend on Spring, JPA, Hibernate, application or infrastructure")
                .allowEmptyShould(true);
    }

    static ArchRule applicationDoesNotDependOnInfrastructure(String root) {
        return noClasses()
                .that()
                .resideInAPackage(root + ".*.application..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(root + ".*.infrastructure..")
                .as("application must not depend on infrastructure")
                .allowEmptyShould(true);
    }

    static ArchRule inboundAdaptersDoNotDependOnOutboundAdapters(String root) {
        return noClasses()
                .that()
                .resideInAPackage(root + ".*.infrastructure.in..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(root + ".*.infrastructure.out..")
                .as("infrastructure.in must not depend on infrastructure.out")
                .allowEmptyShould(true);
    }
}
