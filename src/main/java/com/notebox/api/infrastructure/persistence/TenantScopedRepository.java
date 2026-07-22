package com.notebox.api.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.TenantOwned;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * The single choke point for tenant-owned entities (AD-03, BR-01). Every read and write is filtered by
 * {@link TenantContext#tenantId()}; there is no un-scoped find/query on this base, so a new repository
 * cannot accidentally cross a tenant boundary. Subclasses only declare their entity type.
 */
public abstract class TenantScopedRepository<T extends TenantOwned> {

    @Inject
    EntityManager em;

    @Inject
    TenantContext tenantContext;

    protected abstract Class<T> entityType();

    public Optional<T> findByIdInTenant(UUID id) {
        return em.createQuery(
                        "select e from " + entityName() + " e where e.id = :id and e.tenantId = :tenant",
                        entityType())
                .setParameter("id", id)
                .setParameter("tenant", tenantContext.tenantId())
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<T> listInTenant(int page, int size) {
        return em.createQuery(
                        "select e from " + entityName() + " e where e.tenantId = :tenant order by e.id",
                        entityType())
                .setParameter("tenant", tenantContext.tenantId())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public T persistInTenant(T entity) {
        if (!tenantContext.tenantId().equals(entity.getTenantId())) {
            throw new IllegalStateException("entity tenant does not match the caller's tenant");
        }
        em.persist(entity);
        return entity;
    }

    private String entityName() {
        return entityType().getSimpleName();
    }
}
