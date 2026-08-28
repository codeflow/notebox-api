package com.notebox.api.api;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.notebox.api.api.dto.LoginResponse;
import com.notebox.api.api.dto.OrganizationSignup;
import com.notebox.api.application.auth.JwtIssuer;
import com.notebox.api.application.organization.OrganizationService;

/**
 * Creating an organization — the one unauthenticated write in the product.
 *
 * <p>It exists because every other way in requires an administrator, and the first administrator of
 * a new organization has none. A deployment that does not want it sets
 * {@code notebox.signup.enabled=false} and this returns 403.
 */
@Path("/organizations")
public class OrganizationResource {

    private final OrganizationService organizations;

    public OrganizationResource(OrganizationService organizations) {
        this.organizations = organizations;
    }

    /** Creates the organization and returns a token, so the administrator is already signed in. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response signUp(@Valid OrganizationSignup input) {
        JwtIssuer.IssuedToken issued = organizations.signUp(
                input.organizationName(),
                input.slug(),
                input.displayName(),
                input.email(),
                input.password());
        return Response.status(Response.Status.CREATED)
                .entity(new LoginResponse(issued.token(), issued.expiresAt()))
                .build();
    }
}
