package com.notebox.api.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import com.notebox.api.application.group.GroupFilter;
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
        return listByTypeAndGroupInTenant(annotationTypeId, GroupFilter.none(), page, size);
    }

    /** Total count behind the page above — the grid's pager fact (NFR-08). */
    public long countByTypeInTenant(UUID annotationTypeId) {
        return countByTypeAndGroupInTenant(annotationTypeId, GroupFilter.none());
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

    /**
     * One page of a type's records restricted by group (FR-09 → C30). Order, page defaults and row
     * shape are the unfiltered listing's (OQ-20, NFR-08) — only the predicate is added.
     *
     * @param annotationTypeId the type whose records are listed
     * @param filter unfiltered, one group, or the ungrouped
     * @param page zero-based page index
     * @param size page size
     * @return the page's records, newest first
     */
    public List<AnnotationRecord> listByTypeAndGroupInTenant(
            UUID annotationTypeId, GroupFilter filter, int page, int size) {
        TypedQuery<AnnotationRecord> query = em.createQuery(
                        "select e from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.annotationTypeId = :type"
                                + groupPredicate(filter)
                                + " order by e.createdAt desc, e.id desc",
                        AnnotationRecord.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("type", annotationTypeId)
                .setFirstResult(page * size)
                .setMaxResults(size);
        bindGroup(query, filter);
        return query.getResultList();
    }

    /** Total count behind the filtered page — the pager fact over the FILTERED set (NFR-08). */
    public long countByTypeAndGroupInTenant(UUID annotationTypeId, GroupFilter filter) {
        TypedQuery<Long> query = em.createQuery(
                        "select count(e) from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.annotationTypeId = :type"
                                + groupPredicate(filter),
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("type", annotationTypeId);
        bindGroup(query, filter);
        return query.getSingleResult();
    }

    /** Unfiltered adds nothing; ungrouped is "is null"; one group binds a parameter. */
    private static String groupPredicate(GroupFilter filter) {
        if (!filter.isRestricted()) {
            return "";
        }
        return filter.groupId() == null ? " and e.groupId is null" : " and e.groupId = :group";
    }

    private static void bindGroup(TypedQuery<?> query, GroupFilter filter) {
        if (filter.isRestricted() && filter.groupId() != null) {
            query.setParameter("group", filter.groupId());
        }
    }

    /**
     * Which groups each type's records occupy, and how many records sit in each (FR-09, OQ-23,
     * OQ-27). A null second element means that type has ungrouped records — its Ungrouped node, and
     * the count is that bucket's size. Grouping replaces the former DISTINCT: same rows, same
     * single statement, with the size retained instead of discarded. Bounded by types x groups,
     * never by record count.
     *
     * @return (annotationTypeId, groupId-or-null, count) triples for the caller's tenant
     */
    public List<Object[]> typeGroupCountsInTenant() {
        return em.createQuery(
                        "select e.annotationTypeId, e.groupId, count(e) from AnnotationRecord e"
                                + " where e.tenantId = :tenant"
                                + " group by e.annotationTypeId, e.groupId",
                        Object[].class)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultList();
    }

    /**
     * Per-group aggregates for one page of annotation groups (FR-08, OQ-27): how many records each
     * holds and how many distinct types those records span. One grouped statement for the whole
     * page — never one per row (NFR-08).
     *
     * <p>A group with no records returns <b>no row</b>; the caller maps the missing id to
     * {@link com.notebox.api.application.group.GroupAggregates#empty}. That absence is where the
     * zero comes from, so no special case is needed.
     *
     * @param groupIds the page's group ids; must not be empty (the caller skips the call instead)
     * @return (groupId, recordCount, distinctTypeCount) triples
     */
    public List<Object[]> aggregatesByGroupInTenant(Collection<UUID> groupIds) {
        return em.createQuery(
                        "select e.groupId, count(e), count(distinct e.annotationTypeId)"
                                + " from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.groupId in :groups"
                                + " group by e.groupId",
                        Object[].class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("groups", groupIds)
                .getResultList();
    }

    /**
     * Record counts per annotation type across the caller's tenant — ONE grouped query, so the types
     * list can show how much each type actually holds without paging through the records.
     *
     * @return rows of [annotationTypeId, count]; a type with no records does not appear
     */
    public List<Object[]> recordCountsByTypeInTenant() {
        return em.createQuery(
                        "select e.annotationTypeId, count(e) from AnnotationRecord e"
                                + " where e.tenantId = :tenant"
                                + " group by e.annotationTypeId",
                        Object[].class)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultList();
    }
}
