package pe.leadcenter.erp.shared.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import pe.leadcenter.erp.TestcontainersConfiguration;

/** Full-context checks of the /v1 prefix scope and the security baseline, with test-only probes. */
@Import({
    TestcontainersConfiguration.class,
    ApiPrefixAndSecurityIT.RestProbe.class,
    ApiPrefixAndSecurityIT.PlainProbe.class
})
@SpringBootTest
@AutoConfigureMockMvc
class ApiPrefixAndSecurityIT {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser
    void restControllersAreUnderV1Only() throws Exception {
        mvc.perform(get("/v1/zprobe/ok")).andExpect(status().isOk());
        mvc.perform(get("/zprobe/ok")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void plainControllersAreNotPrefixed() throws Exception {
        mvc.perform(get("/zprobe/plain")).andExpect(status().isOk());
        mvc.perform(get("/v1/zprobe/plain")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void actuatorIsNotPrefixed() throws Exception {
        mvc.perform(get("/v1/actuator/health")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void authenticationExceptionsAreLeftToSecurity() throws Exception {
        mvc.perform(get("/v1/zprobe/authex")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void invalidRequestParamYieldsFieldError() throws Exception {
        mvc.perform(get("/v1/zprobe/param").param("n", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("n"));
    }

    @Test
    void anonymousOnlyReachesHealth() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator")).andExpect(status().isForbidden());
        mvc.perform(get("/docs")).andExpect(status().isForbidden());
        mvc.perform(get("/v1/zprobe/ok")).andExpect(status().isForbidden());
    }

    @RestController
    static class RestProbe {

        @GetMapping("/zprobe/ok")
        String ok() {
            return "ok";
        }

        @GetMapping("/zprobe/param")
        String param(@RequestParam @Min(1) int n) {
            return "n=" + n;
        }

        @GetMapping("/zprobe/authex")
        String authex() {
            throw new BadCredentialsException("nope");
        }
    }

    @Controller
    static class PlainProbe {

        @GetMapping("/zprobe/plain")
        @ResponseBody
        String plain() {
            return "plain";
        }
    }
}
