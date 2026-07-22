package com.notebox.api.api.error;

import java.util.Locale;
import java.util.UUID;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.infrastructure.i18n.LocaleResolver;
import com.notebox.api.infrastructure.i18n.MessageResolver;

import io.quarkus.security.AuthenticationFailedException;

/**
 * Maps a present-but-invalid token to the uniform envelope, distinguishing an expired token
 * (AUTH_TOKEN_EXPIRED) from an otherwise invalid one (AUTH_TOKEN_INVALID) by inspecting the cause chain.
 */
@Provider
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    private final MessageResolver messages;
    private final LocaleResolver locales;

    @Context
    HttpHeaders headers;

    public AuthenticationFailedExceptionMapper(MessageResolver messages, LocaleResolver locales) {
        this.messages = messages;
        this.locales = locales;
    }

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        String code = isExpired(exception) ? "AUTH_TOKEN_EXPIRED" : "AUTH_TOKEN_INVALID";
        Locale locale = locales.fromHeaders(headers);
        Problem problem = new Problem(code, messages.resolve(code, locale), UUID.randomUUID().toString());
        return Response.status(Response.Status.UNAUTHORIZED).entity(problem).build();
    }

    private boolean isExpired(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("expir")) {
                return true;
            }
        }
        return false;
    }
}
