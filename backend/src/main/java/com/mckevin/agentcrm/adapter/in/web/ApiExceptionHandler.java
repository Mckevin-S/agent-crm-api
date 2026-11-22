package com.mckevin.agentcrm.adapter.in.web;

import com.mckevin.agentcrm.application.AccessDeniedException;
import com.mckevin.agentcrm.application.InvalidCredentialsException;
import com.mckevin.agentcrm.application.NotFoundException;
import com.mckevin.agentcrm.application.port.CrmUnavailableException;
import com.mckevin.agentcrm.domain.shared.BusinessRuleViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.client.RestClientException;

import java.util.stream.Collectors;

/** Erreurs au format Problem Details (RFC 7807). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Introuvable", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "Accès refusé", e.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolation.class)
    public ProblemDetail businessRule(BusinessRuleViolation e) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Règle métier non respectée", e.getMessage());
        problem.setProperty("code", e.code());
        return problem;
    }

    @ExceptionHandler(CrmUnavailableException.class)
    public ProblemDetail crmUnavailable(CrmUnavailableException e) {
        ProblemDetail problem = problem(HttpStatus.SERVICE_UNAVAILABLE, "CRM indisponible",
                "Le CRM ne répond pas et aucune copie locale de ce client n'est disponible. Réessayez dans un instant.");
        problem.setProperty("code", "CRM_UNAVAILABLE");
        return problem;
    }

    @ExceptionHandler(RestClientException.class)
    public ProblemDetail demoToolUnavailable(RestClientException e) {
        return problem(HttpStatus.BAD_GATEWAY, "Service externe injoignable", "Le simulateur de CRM ne répond pas.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentification refusée", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining(" ; "));
        return problem(HttpStatus.BAD_REQUEST, "Champs invalides", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ProblemDetail badRequest(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "Requête invalide",
                e instanceof IllegalArgumentException ? e.getMessage() : "La requête est mal formée.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
