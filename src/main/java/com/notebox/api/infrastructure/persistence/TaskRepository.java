package com.notebox.api.infrastructure.persistence;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

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
        return em.createQuery(
                        "select e from Task e"
                                + " where e.tenantId = :tenant"
                                + " order by e.createdAt desc, e.id desc",
                        Task.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** Total count behind the page above — the pager fact (NFR-08). */
    public long countInTenant() {
        return em.createQuery(
                        "select count(e) from Task e where e.tenantId = :tenant",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .getSingleResult();
    }
}
