package com.notebox.api.api.error;

import java.util.UUID;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.domain.error.DomainException;
import com.notebox.api.domain.error.ErrorCategory;
import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

/**
 * Maps any {@link DomainException} to the uniform {@link Problem} envelope: the domain
 * {@link ErrorCategory} becomes the HTTP status and the message key is resolved to the request
 * locale (constitution §Errors, AD-11, BR-08).
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public DomainExceptionMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(DomainException exception) {
        String message =
                messages.resolve(exception.getMessageKey(), locales.fromHeaders(headers), exception.getArgs());
        Problem problem = new Problem(exception.getMessageKey(), message, UUID.randomUUID().toString());
        return Response.status(statusOf(exception.getCategory())).entity(problem).build();
    }

    private int statusOf(ErrorCategory category) {
        return switch (category) {
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case INVALID -> 400;
            case FORBIDDEN -> 403;
        };
    }
}
