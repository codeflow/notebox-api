package com.notebox.api.infrastructure.persistence;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;

/** Tenant-scoped access to groups (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class GroupRepository extends TenantScopedRepository<Group> {

    @Override
    protected Class<Group> entityType() {
        return Group.class;
    }

    /** Removes a managed group; the FK's ON DELETE SET NULL un-groups its members (OQ-24). */
    public void remove(Group group) {
        em.remove(group);
    }

    /**
     * One page of the tenant's groups in a domain, ordered by name (OQ-25).
     *
     * @param domain the namespace to list
     * @param page zero-based page index
     * @param size page size
     * @return the page's groups, name ascending with an id tiebreak
     */
    public List<Group> listByDomainInTenant(GroupDomain domain, int page, int size) {
        return em.createQuery(
                        "select e from Group e"
                                + " where e.tenantId = :tenant and e.domain = :domain"
                                + " order by lower(e.name) asc, e.id asc",
                        Group.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("domain", domain)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    /** Total count behind the page above — the pager fact (NFR-08). */
    public long countByDomainInTenant(GroupDomain domain) {
        return em.createQuery(
                        "select count(e) from Group e"
                                + " where e.tenantId = :tenant and e.domain = :domain",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("domain", domain)
                .getSingleResult();
    }

    /**
     * Whether a name is already taken in one namespace. Uniqueness is per tenant AND per domain, so
     * the same name may be free in the other one (OQ-04).
     *
     * @param domain the namespace to check
     * @param name the candidate name
     * @return true when the tenant already has a group of that name in that domain
     */
    public boolean existsByDomainAndNameInTenant(GroupDomain domain, String name) {
        Long count = em.createQuery(
                        "select count(e) from Group e"
                                + " where e.tenantId = :tenant and e.domain = :domain and e.name = :name",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("domain", domain)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }

    /** How many annotation records and tasks reference a group — the audit's members fact (C-10). */
    public long countMembers(Group group) {
        Long records = em.createQuery(
                        "select count(e) from AnnotationRecord e"
                                + " where e.tenantId = :tenant and e.groupId = :group",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("group", group.getId())
                .getSingleResult();
        Long tasks = em.createQuery(
                        "select count(e) from Task e"
                                + " where e.tenantId = :tenant and e.groupId = :group",
                        Long.class)
                .setParameter("tenant", tenantContext.tenantId())
                .setParameter("group", group.getId())
                .getSingleResult();
        return records + tasks;
    }
}
