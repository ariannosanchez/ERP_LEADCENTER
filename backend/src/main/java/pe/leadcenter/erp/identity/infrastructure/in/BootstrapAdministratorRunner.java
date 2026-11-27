package pe.leadcenter.erp.identity.infrastructure.in;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.leadcenter.erp.identity.application.bootstrapadministrator.BootstrapAdministratorCommand;
import pe.leadcenter.erp.identity.application.bootstrapadministrator.BootstrapAdministratorHandler;

/** Runs the administrator bootstrap at start; a failure aborts startup. */
@Component
@EnableConfigurationProperties(BootstrapAdminProperties.class)
@RequiredArgsConstructor
class BootstrapAdministratorRunner implements ApplicationRunner {

    private final BootstrapAdminProperties properties;
    private final BootstrapAdministratorHandler handler;

    @Override
    public void run(ApplicationArguments args) {
        handler.execute(
                new BootstrapAdministratorCommand(
                        properties.email(), properties.password(), properties.fullName()));
    }
}
