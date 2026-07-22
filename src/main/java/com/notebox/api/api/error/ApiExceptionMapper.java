package com.notebox.api.api.error;

import java.util.UUID;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

/** Maps {@link ApiException} to the uniform {@link Problem} envelope with a localized message (AD-11, BR-08). */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public ApiExceptionMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(ApiException exception) {
        String message = messages.resolve(exception.getCode(), locales.fromHeaders(headers));
        Problem problem = new Problem(exception.getCode(), message, UUID.randomUUID().toString());
        return Response.status(exception.getStatus()).entity(problem).build();
    }
}
