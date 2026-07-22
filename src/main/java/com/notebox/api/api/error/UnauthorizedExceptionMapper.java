package com.notebox.api.api.error;

import java.util.UUID;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

import io.quarkus.security.UnauthorizedException;

/** Maps a request with no/insufficient authentication to AUTH_REQUIRED in the uniform envelope. */
@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public UnauthorizedExceptionMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(UnauthorizedException exception) {
        String message = messages.resolve("AUTH_REQUIRED", locales.fromHeaders(headers));
        Problem problem = new Problem("AUTH_REQUIRED", message, UUID.randomUUID().toString());
        return Response.status(Response.Status.UNAUTHORIZED).entity(problem).build();
    }
}
