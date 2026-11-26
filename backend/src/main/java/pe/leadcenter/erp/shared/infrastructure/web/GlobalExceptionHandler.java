package pe.leadcenter.erp.shared.infrastructure.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pe.leadcenter.erp.shared.domain.BusinessRuleViolationException;
import pe.leadcenter.erp.shared.domain.DomainException;
import pe.leadcenter.erp.shared.domain.NotFoundException;

/**
 * Translates every failure into an RFC 9457 Problem Details response. Standard Spring MVC errors
 * are handled by the parent class; this adds validation field details, domain errors and a generic
 * 500 that never exposes internals.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final URI VALIDATION = URI.create("urn:problem-type:validation-error");
    static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");
    static final URI BUSINESS_RULE = URI.create("urn:problem-type:business-rule-violation");
    static final URI DOMAIN_ERROR = URI.create("urn:problem-type:domain-error");
    static final URI INTERNAL = URI.create("urn:problem-type:internal-error");

    /** One invalid field in a validation error. */
    public record FieldViolation(String field, String message) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(
                        error ->
                                errors.add(
                                        new FieldViolation(
                                                error instanceof FieldError fe
                                                        ? fe.getField()
                                                        : error.getObjectName(),
                                                error.getDefaultMessage())));
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        ex.getParameterValidationResults()
                .forEach(
                        result -> {
                            String parameterName = result.getMethodParameter().getParameterName();
                            String name =
                                    parameterName != null
                                            ? parameterName
                                            : "arg"
                                                    + result.getMethodParameter()
                                                            .getParameterIndex();
                            result.getResolvableErrors()
                                    .forEach(
                                            e ->
                                                    errors.add(
                                                            new FieldViolation(
                                                                    name, e.getDefaultMessage())));
                        });
        return handleExceptionInternal(ex, validationProblem(errors), headers, status, request);
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return domainProblem(HttpStatus.NOT_FOUND, "Resource not found", NOT_FOUND, ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return domainProblem(
                HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violation", BUSINESS_RULE, ex);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        return domainProblem(HttpStatus.UNPROCESSABLE_ENTITY, "Domain error", DOMAIN_ERROR, ex);
    }

    /**
     * Security exceptions belong to Spring Security's filter chain (401/403 handling); rethrow so
     * the generic handler below does not turn them into a 500.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void rethrowSecurityExceptions(Exception ex) throws Exception {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal server error");
        problem.setType(INTERNAL);
        return problem;
    }

    private static ProblemDetail validationProblem(List<FieldViolation> errors) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        problem.setTitle("Validation failed");
        problem.setType(VALIDATION);
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail domainProblem(
            HttpStatus status, String title, URI type, DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setTitle(title);
        problem.setType(type);
        problem.setProperty("code", ex.code());
        return problem;
    }
}
