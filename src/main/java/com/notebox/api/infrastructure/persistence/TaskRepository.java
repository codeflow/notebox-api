package com.notebox.api.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import com.notebox.api.application.group.GroupFilter;
import com.notebox.api.domain.Task;

/** Tenant-scoped access to tasks (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class TaskRepository extends TenantScopedRepository<Task> {

    @Override
    protected Class<Task> entityType() {
        return Task.class;
    }

    /** Removes a managed task; cascade + orphanRemoval delete its subtasks (BR-05). */
    public void remove(Task task) {
        em.remove(task);
    }

    /** One page of the caller's tenant's tasks, newest first — createdAt desc, id desc (OQ-21). */
    public List<Task> listNewestFirstInTenant(int page, int size) {
        return listByGroupNewestFirstInTenant(GroupFilter.none(), page, size);
    }

    /** Total count behind the page above — the pager fact (NFR-08). */
    public long countInTenant() {
        return countByGroupInTenant(GroupFilter.none());
    }

    /**
     * One page of the tenant's tasks restricted by group (FR-09 → C30). Order and page defaults are
     * the unfiltered listing's (OQ-21, NFR-08) — only the predicate is added.
     *
     * @param filter unfiltered, one group, or the ungrouped
     * @param page zero-based page index
     * @param size page size
     * @return the page's tasks, newest first
     */
    public List<Task> listByGroupNewestFirstInTenant(GroupFilter filter, int page, int size) {
        TypedQuery<Task> query = em.createQuery(
                        "select e from Task e"
                                + " where e.tenantId = :tenant"
                                + groupPredicate(filter)
                                + " order by e.createdAt desc, e.id desc",
                        Task.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setFirstResult(page * size)
                .setMaxResults(size);
        bindGroup(query, filter);
        return query.getResultList();
    }

    /** Total count behind the filtered page — the pager fact over the FILTERED set (NFR-08). */
    public long countByGroupInTenant(GroupFilter filter) {
        TypedQuery<Long> query = em.createQuery(
                        "select count(e) from Task e"
                                + " where e.tenantId = :tenant"
                                + groupPredicate(filter),
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId());
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
     * Which groups the tenant's tasks actually occupy (FR-09, OQ-23). A null element means at least
     * one ungrouped task exists — the Tasks root's Ungrouped node.
     *
     * @return distinct group ids, null included when ungrouped tasks exist
     */
    public List<UUID> distinctGroupIdsInTenant() {
        return em.createQuery(
                        "select distinct e.groupId from Task e where e.tenantId = :tenant",
                        UUID.class)
                .setParameter("tenant", tenantContext.tenantId())
                .getResultList();
    }
}
