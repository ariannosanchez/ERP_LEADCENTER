/**
 * Shared kernel: id generation, base exceptions, error format, persistence conventions and security
 * primitives used by every module. Declared OPEN so any module may use all of its packages; it must
 * never depend on a business module.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package pe.leadcenter.erp.shared;

import org.springframework.modulith.ApplicationModule;
