package com.notebox.api.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.notebox.api.domain.Tenant;

/**
 * Reads the tenant record itself.
 *
 * <p>This is deliberately NOT a {@link TenantScopedRepository}: the tenant is not a row inside a
 * tenant, it IS the tenant. The isolation guarantee therefore rests on the caller — every use site
 * must pass the id from the authenticated token ({@code TenantContext#tenantId()}) and never an id
 * taken from a request. There is no lookup by name or slug for the same reason: nothing here may
 * become a way to probe which tenants exist (BR-01, BR-02, AD-03).
 */
@ApplicationScoped
public class TenantRepository {

    @PersistenceContext
    EntityManager entityManager;

    /** Finds a tenant by its id. The id must come from the caller's own token. */
    public Optional<Tenant> findById(UUID tenantId) {
        return Optional.ofNullable(entityManager.find(Tenant.class, tenantId));
    }
}
