package com.notebox.api.api.error;

import java.util.UUID;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

/** Maps Bean Validation failures at the edge (AD-07) to VALIDATION_FAILED in the uniform envelope. */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public ValidationExceptionMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = messages.resolve("VALIDATION_FAILED", locales.fromHeaders(headers));
        Problem problem = new Problem("VALIDATION_FAILED", message, UUID.randomUUID().toString());
        return Response.status(Response.Status.BAD_REQUEST).entity(problem).build();
    }
}
