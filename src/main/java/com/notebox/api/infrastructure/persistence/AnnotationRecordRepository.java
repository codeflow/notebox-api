package com.notebox.api.infrastructure.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.AnnotationRecord;

/** Tenant-scoped access to annotation records (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class AnnotationRecordRepository extends TenantScopedRepository<AnnotationRecord> {

    @Override
    protected Class<AnnotationRecord> entityType() {
        return AnnotationRecord.class;
    }

    /** Removes a managed record; cascade + orphanRemoval delete its values and selected options. */
    public void remove(AnnotationRecord record) {
        em.remove(record);
    }

    /** Whether the caller's tenant has at least one record of the given type (OQ-14 type-delete guard). */
    public boolean existsByType(UUID annotationTypeId) {
        Long count = em.createQuery(
                        "select count(e) from AnnotationRecord e "
                                + "where e.tenantId = :tenant and e.annotationTypeId = :type",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("type", annotationTypeId)
                .getSingleResult();
        return count > 0;
    }
}
