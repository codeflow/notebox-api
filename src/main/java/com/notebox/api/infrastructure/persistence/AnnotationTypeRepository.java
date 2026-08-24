package com.notebox.api.infrastructure.persistence;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.AnnotationType;

/** Tenant-scoped access to annotation types (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class AnnotationTypeRepository extends TenantScopedRepository<AnnotationType> {

    @Override
    protected Class<AnnotationType> entityType() {
        return AnnotationType.class;
    }

    /** Removes a managed type; cascade + orphanRemoval delete its fields and options. */
    public void remove(AnnotationType type) {
        em.remove(type);
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

    /**
     * Every type of the caller's tenant, name ascending (OQ-25). Types are the tree's structural
     * axis (C28), so a type with no records still appears as a node.
     *
     * @return the tenant's types, ordered case-insensitively by name with an id tiebreak
     */
    public List<AnnotationType> listAllInTenantByName() {
        return em.createQuery(
                        "select e from AnnotationType e where e.tenantId = :tenant"
                                + " order by lower(e.name) asc, e.id asc",
                        AnnotationType.class)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultList();
    }
}
