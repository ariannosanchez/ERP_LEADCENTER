package pe.leadcenter.erp.shared.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.domain.DomainException;
import pe.leadcenter.erp.shared.domain.NotFoundException;

@WebMvcTest(GlobalExceptionHandlerTest.ProbeController.class)
@Import({
    GlobalExceptionHandler.class,
    ApiVersionConfig.class,
    GlobalExceptionHandlerTest.ProbeController.class
})
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mvc;

    @Test
    void restControllersAreServedUnderApiV1Only() throws Exception {
        mvc.perform(get("/v1/probe/ok")).andExpect(status().isOk());
        mvc.perform(get("/probe/ok")).andExpect(status().isNotFound());
    }

    @Test
    void validationErrorsListEachInvalidField() throws Exception {
        mvc.perform(
                        post("/v1/probe/validate")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\",\"code\":\"toolong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field=='name')].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[?(@.field=='code')].message").isNotEmpty());
    }

    @Test
    void notFoundMapsTo404() throws Exception {
        mvc.perform(get("/v1/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.code").value("probe.not_found"))
                .andExpect(jsonPath("$.detail").value("Probe 42 does not exist"));
    }

    @Test
    void businessRuleViolationMapsTo422() throws Exception {
        mvc.perform(get("/v1/probe/rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:business-rule-violation"))
                .andExpect(jsonPath("$.code").value("probe.rule"))
                .andExpect(jsonPath("$.detail").value("Rule broken"));
    }

    @Test
    void otherDomainExceptionsMapTo422() throws Exception {
        mvc.perform(get("/v1/probe/domain"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("probe.generic"));
    }

    @Test
    void unexpectedErrorsReturnGeneric500WithoutLeakingInternals() throws Exception {
        mvc.perform(get("/v1/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void responseStatusExceptionsKeepTheirAnnotatedStatus() throws Exception {
        mvc.perform(get("/v1/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Already exists"))
                .andExpect(content().string(not(containsString("secret-conflict-detail"))));
    }

    @Test
    void responseStatusWithoutReasonNeverLeaksTheMessage() throws Exception {
        mvc.perform(get("/v1/probe/conflict-no-reason"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("secret-conflict-detail"))));
    }

    @Test
    void accessDeniedIsNotSwallowedAsA500() throws Exception {
        mvc.perform(get("/v1/probe/denied")).andExpect(status().isForbidden());
    }

    @Test
    void frameworkErrorsKeepTheirStatusAsProblemDetails() throws Exception {
        mvc.perform(
                        post("/v1/probe/validate")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Already exists")
    static class ConflictWithReason extends RuntimeException {
        ConflictWithReason() {
            super("secret-conflict-detail");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    static class ConflictNoReason extends RuntimeException {
        ConflictNoReason() {
            super("secret-conflict-detail");
        }
    }

    record ProbeRequest(@NotBlank String name, @Size(max = 3) String code) {}

    @RestController
    static class ProbeController {

        @GetMapping("/probe/ok")
        String ok() {
            return "ok";
        }

        @PostMapping("/probe/validate")
        String validate(@Valid @RequestBody ProbeRequest request) {
            return request.name();
        }

        @GetMapping("/probe/not-found")
        String notFound() {
            throw new NotFoundException("probe.not_found", "Probe 42 does not exist");
        }

        @GetMapping("/probe/rule")
        String rule() {
            throw new BusinessRuleViolationException("probe.rule", "Rule broken");
        }

        @GetMapping("/probe/domain")
        String domain() {
            throw new DomainException("probe.generic", "Generic") {};
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalStateException("secret-internal-detail");
        }

        @GetMapping("/probe/conflict")
        String conflict() {
            throw new ConflictWithReason();
        }

        @GetMapping("/probe/conflict-no-reason")
        String conflictNoReason() {
            throw new ConflictNoReason();
        }

        @GetMapping("/probe/denied")
        String denied() {
            throw new AccessDeniedException("nope");
        }
    }
}
