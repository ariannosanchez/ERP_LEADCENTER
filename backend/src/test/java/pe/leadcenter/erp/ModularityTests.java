package pe.leadcenter.erp;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Fails when a module reaches into another module's internals or modules form a cycle. */
class ModularityTests {

	@Test
	void moduleBoundariesAreRespected() {
		ApplicationModules.of(ErpApiApplication.class).verify();
	}
}
