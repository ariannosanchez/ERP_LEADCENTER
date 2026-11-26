package pe.leadcenter.erp.shared.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefixes every application {@code @RestController} with {@code /v1}. Combined with the servlet
 * context path the public base is {@code /api/v1}. Framework controllers (actuator, springdoc) live
 * outside the application package and are untouched.
 */
@Configuration(proxyBeanMethods = false)
public class ApiVersionConfig implements WebMvcConfigurer {

    public static final String PREFIX = "/v1";
    private static final String APPLICATION_PACKAGE = "pe.leadcenter.erp";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                PREFIX,
                type ->
                        type.getPackageName().startsWith(APPLICATION_PACKAGE)
                                && AnnotatedElementUtils.hasAnnotation(type, RestController.class));
    }
}
