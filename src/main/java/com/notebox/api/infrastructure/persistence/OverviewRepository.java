package com.notebox.api.infrastructure.persistence;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.notebox.api.infrastructure.security.TenantContext;

/**
 * The counts behind the workspace summary (OQ-31).
 *
 * <p>Every query is filtered by the caller's tenant from {@link TenantContext} — the choke point
 * AD-03 requires. None of these accept a caller-supplied filter, so there is no shape of request
 * that could widen them.
 */
@ApplicationScoped
public class OverviewRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Inject
    TenantContext tenantContext;

    private long count(String jpql, Object... params) {
        var query = entityManager.createQuery(jpql, Long.class)
                .setParameter("tenant", tenantContext.tenantId());
        for (int i = 0; i < params.length; i += 2) {
            query.setParameter((String) params[i], params[i + 1]);
        }
        return query.getSingleResult();
    }

    public long typesDefined() {
        return count("select count(t) from AnnotationType t where t.tenantId = :tenant");
    }

    public long records() {
        return count("select count(r) from AnnotationRecord r where r.tenantId = :tenant");
    }

    /** Records holding at least one image value — the design's "With images". */
    public long recordsWithImages() {
        return count("select count(distinct r.id) from AnnotationRecord r join r.values v"
                + " where r.tenantId = :tenant and v.imageId is not null");
    }

    /** Fields flagged secret across the tenant's types, which is what C-12 governs. */
    public long secretFields() {
        return count("select count(f) from AnnotationType t join t.fields f"
                + " where t.tenantId = :tenant and f.secret = true");
    }

    /** A task is "open" until its derived status reaches 100 (BR-06). */
    public long openTasks() {
        return count("select count(t) from Task t where t.tenantId = :tenant and t.status < 100");
    }

    public long subtasksDone() {
        return count("select count(s) from Task t join t.subtasks s"
                + " where t.tenantId = :tenant and s.done = true");
    }

    public long subtasksTotal() {
        return count("select count(s) from Task t join t.subtasks s where t.tenantId = :tenant");
    }

    /** Open tasks whose derived end date falls inside the window the caller's clock defines. */
    public long tasksEndingBetween(LocalDate from, LocalDate to) {
        return count("select count(t) from Task t where t.tenantId = :tenant"
                        + " and t.status < 100 and t.endDate >= :from and t.endDate <= :to",
                "from", from, "to", to);
    }
}
