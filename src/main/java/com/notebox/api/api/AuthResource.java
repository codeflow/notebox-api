package com.notebox.api.api;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.api.dto.LoginRequest;
import com.notebox.api.api.dto.LoginResponse;
import com.notebox.api.application.auth.AuthService;
import com.notebox.api.application.auth.JwtIssuer;

/** Public authentication endpoint. Issues a stateless JWT on valid credentials. */
@Path("/auth")
public class AuthResource {

    private final AuthService authService;

    public AuthResource(AuthService authService) {
        this.authService = authService;
    }

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public LoginResponse login(@Valid LoginRequest request) {
        JwtIssuer.IssuedToken issued = authService.authenticate(request.email(), request.password());
        return new LoginResponse(issued.token(), issued.expiresAt());
    }
}
