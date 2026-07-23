package com.notebox.api.api.error;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

/**
 * Maps a Bean Validation failure to the {@link ValidationProblem} envelope: HTTP 400 with a localized
 * per-field violation list (constitution §Errors, AD-07, C-09). Each constraint's message is a
 * dot-namespaced key resolved to the request locale through the catalog.
 */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public ConstraintViolationMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Locale locale = locales.fromHeaders(headers);
        List<Violation> violations = exception.getConstraintViolations().stream()
                .map(violation -> toViolation(violation, locale))
                .toList();
        ValidationProblem problem = new ValidationProblem(
                "validation.failed",
                messages.resolve("validation.failed", locale),
                UUID.randomUUID().toString(),
                violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(problem).build();
    }

    private Violation toViolation(ConstraintViolation<?> violation, Locale locale) {
        String key = violation.getMessage();
        return new Violation(violation.getPropertyPath().toString(), key, messages.resolve(key, locale));
    }
}
