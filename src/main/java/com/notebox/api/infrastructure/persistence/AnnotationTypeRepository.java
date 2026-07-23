package com.notebox.api.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.AnnotationType;

/** Tenant-scoped access to annotation types (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class AnnotationTypeRepository extends TenantScopedRepository<AnnotationType> {

    @Override
    protected Class<AnnotationType> entityType() {
        return AnnotationType.class;
    }

    /** Whether the caller's tenant already has a type with this name (per-tenant uniqueness). */
    public boolean existsByName(String name) {
        Long count = em.createQuery(
                        "select count(e) from AnnotationType e where e.tenantId = :tenant and e.name = :name",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }
}
