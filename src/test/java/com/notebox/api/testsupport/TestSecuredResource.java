package com.notebox.api.testsupport;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.notebox.api.infrastructure.security.TenantContext;

import io.quarkus.security.Authenticated;

/**
 * Test-only protected endpoint (never shipped) used to exercise the security layer and the
 * request-scoped TenantContext. The {@code tenantOverride} query param proves that the effective
 * tenant is taken from the token, not from request input.
 */
@Path("/test/whoami")
@Authenticated
public class TestSecuredResource {

    @Inject
    TenantContext tenantContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public WhoAmI whoami(@QueryParam("tenantOverride") String tenantOverride) {
        return new WhoAmI(
                tenantContext.tenantId().toString(),
                tenantContext.userId().toString(),
                tenantContext.role().name());
    }

    public record WhoAmI(String tenantId, String userId, String role) {
    }
}
