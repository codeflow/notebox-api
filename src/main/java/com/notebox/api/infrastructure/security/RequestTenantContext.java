package com.notebox.api.infrastructure.security;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;

import com.notebox.api.domain.Role;

/** Request-scoped {@link TenantContext} populated by {@link JwtTenantFilter} from the verified token. */
@RequestScoped
public class RequestTenantContext implements TenantContext {

    private UUID tenantId;
    private UUID userId;
    private Role role;
    private boolean authenticated;

    void populate(UUID tenantId, UUID userId, Role role) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
        this.authenticated = true;
    }

    @Override
    public UUID tenantId() {
        return tenantId;
    }

    @Override
    public UUID userId() {
        return userId;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public boolean authenticated() {
        return authenticated;
    }
}
