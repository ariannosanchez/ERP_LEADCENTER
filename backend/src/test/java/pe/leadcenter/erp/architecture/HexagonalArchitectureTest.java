package pe.leadcenter.erp.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces the hexagonal layout on the real code base (passes while no module exists). */
@AnalyzeClasses(
        packages = "pe.leadcenter.erp",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String ROOT = "pe.leadcenter.erp";

    @ArchTest static final ArchRule domainIsPure = HexagonalRules.domainIsPure(ROOT);

    @ArchTest
    static final ArchRule applicationIndependentOfInfrastructure =
            HexagonalRules.applicationDoesNotDependOnInfrastructure(ROOT);

    @ArchTest
    static final ArchRule inboundIndependentOfOutbound =
            HexagonalRules.inboundAdaptersDoNotDependOnOutboundAdapters(ROOT);
}
