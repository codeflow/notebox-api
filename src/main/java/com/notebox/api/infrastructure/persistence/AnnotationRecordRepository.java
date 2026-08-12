package com.notebox.api.infrastructure.persistence;

import java.util.List;
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

    /** True when any record of the caller's tenant holds a value for the given field (OQ-18). */
    public boolean existsValueForField(UUID typeFieldId) {
        return em.createQuery(
                        "select count(v) from AnnotationRecord r join r.values v"
                                + " where r.tenantId = :tenant and v.typeFieldId = :field",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("field", typeFieldId)
                .getSingleResult() > 0;
    }

    /** One page of the caller's tenant's records of a type, newest first — createdAt desc, id desc (OQ-20). */
    public List<AnnotationRecord> listByTypeInTenant(UUID annotationTypeId, int page, int size) {
        return em.createQuery(
                        "select e from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.annotationTypeId = :type"
                                + " order by e.createdAt desc, e.id desc",
                        AnnotationRecord.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("type", annotationTypeId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** Total count behind the page above — the grid's pager fact (NFR-08). */
    public long countByTypeInTenant(UUID annotationTypeId) {
        return em.createQuery(
                        "select count(e) from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.annotationTypeId = :type",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("type", annotationTypeId)
                .getSingleResult();
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
