package com.notebox.api.api.error;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
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
                localize("validation.failed", locale),
                UUID.randomUUID().toString(),
                violations);
        return Response.status(Response.Status.BAD_REQUEST).entity(problem).build();
    }

    private Violation toViolation(ConstraintViolation<?> violation, Locale locale) {
        String key = violation.getMessage();
        return new Violation(violation.getPropertyPath().toString(), key, localize(key, locale));
    }

    /**
     * Resolves a constraint message key to the request locale, degrading gracefully: a constraint that
     * carries a non-catalog default message (e.g. Bean Validation's own "must not be blank") returns
     * that text as-is instead of throwing, so no validation failure ever becomes a 500.
     */
    private String localize(String key, Locale locale) {
        try {
            return messages.resolve(key, locale);
        } catch (MissingResourceException notACatalogKey) {
            return key;
        }
    }
}
