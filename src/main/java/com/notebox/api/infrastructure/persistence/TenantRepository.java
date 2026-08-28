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

    /**
     * Whether a slug is already taken.
     *
     * <p>The one lookup here that is NOT by the caller's own id, and it exists only so signup can
     * refuse a duplicate address cleanly instead of failing on the unique constraint. It returns a
     * boolean and never the tenant, so it cannot become a way to read another organization.
     */
    public boolean slugExists(String slug) {
        return entityManager.createQuery(
                        "select count(t) from Tenant t where t.slug = :slug", Long.class)
                .setParameter("slug", slug)
                .getSingleResult() > 0;
    }

    /** Persists a brand new organization. */
    public Tenant persist(Tenant tenant) {
        entityManager.persist(tenant);
        return tenant;
    }

    /**
     * Persists the first administrator of a brand new organization.
     *
     * <p>This bypasses {@code UserRepository}'s tenant scoping deliberately and is the ONLY place
     * that may: at signup there is no caller tenant to scope to. Every other write to a user goes
     * through the scoped repository.
     */
    public void persistUser(com.notebox.api.domain.User admin) {
        entityManager.persist(admin);
    }
}
