package com.notebox.api.infrastructure.security;

import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import com.notebox.api.domain.Role;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Populates the request-scoped {@link RequestTenantContext} from the verified JWT after the security
 * layer has authenticated the request. The tenant and identity come only from token claims (BR-01).
 */
@Provider
@Priority(Priorities.AUTHORIZATION)
public class JwtTenantFilter implements ContainerRequestFilter {

    private final JsonWebToken jwt;
    private final RequestTenantContext tenantContext;

    public JwtTenantFilter(JsonWebToken jwt, RequestTenantContext tenantContext) {
        this.jwt = jwt;
        this.tenantContext = tenantContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (jwt == null || jwt.getName() == null) {
            return;
        }
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID tenantId = UUID.fromString(jwt.getClaim("tenant"));
        Role role = jwt.getGroups().stream().findFirst().map(Role::valueOf).orElse(Role.MEMBER);
        tenantContext.populate(tenantId, userId, role);
    }
}
