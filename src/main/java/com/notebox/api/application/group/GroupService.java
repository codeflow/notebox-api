package com.notebox.api.application.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.GroupInput;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;
import com.notebox.api.domain.error.GroupDomainMismatchException;
import com.notebox.api.domain.error.GroupDomainNotModifiableException;
import com.notebox.api.domain.error.GroupNameTakenException;
import com.notebox.api.domain.error.GroupNotFoundException;
import com.notebox.api.infrastructure.persistence.AnnotationRecordRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.GroupRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for groups (FR-08): CRUD behind the tenant choke point (AD-03), per-tenant-per-domain
 * name uniqueness, a domain fixed at creation, and an audited delete (BR-05, C-10) that removes the
 * label only — the FK's ON DELETE SET NULL un-groups every member, so no record or task is ever
 * deleted here (OQ-24). Also the one place that resolves and domain-checks a group for assignment,
 * since no foreign key can express "of the item's own namespace".
 */
@ApplicationScoped
public class GroupService {

    private static final String ACTION_GROUP_DELETED = "GROUP_DELETED";
    private static final String TARGET_GROUP = "GROUP";

    private final GroupRepository groups;
    private final AuditLogRepository auditLog;
    private final TenantContext tenant;
    private final AnnotationRecordRepository records;
    private final TaskRepository tasks;

    public GroupService(
            GroupRepository groups,
            AuditLogRepository auditLog,
            TenantContext tenant,
            AnnotationRecordRepository records,
            TaskRepository tasks) {
        this.groups = groups;
        this.auditLog = auditLog;
        this.tenant = tenant;
        this.records = records;
        this.tasks = tasks;
    }

    /**
     * Creates a group in a namespace.
     *
     * @param input the requested name and domain
     * @return the persisted group
     * @throws GroupNameTakenException when the tenant already has that name in that domain
     */
    @Transactional
    public Group create(GroupInput input) {
        GroupDomain domain = GroupDomain.valueOf(input.domain());
        if (groups.existsByDomainAndNameInTenant(domain, input.name())) {
            throw new GroupNameTakenException();
        }
        return groups.persistInTenant(new Group(tenant.tenantId(), input.name(), domain));
    }

    /**
     * Reads one group of the caller's tenant.
     *
     * @param id the group id
     * @return the group
     * @throws GroupNotFoundException when it is unknown or belongs to another tenant (C-01)
     */
    public Group get(UUID id) {
        return groups.findByIdInTenant(id).orElseThrow(GroupNotFoundException::new);
    }

    public List<Group> list(GroupDomain domain, int page, int size) {
        return groups.listByDomainInTenant(domain, page, size);
    }

    public long count(GroupDomain domain) {
        return groups.countByDomainInTenant(domain);
    }

    /**
     * Renames a group. The stated domain must match the stored one: the domain is fixed at creation
     * because changing it would silently orphan every member.
     *
     * @param id the group id
     * @param input the new name and the group's own domain
     * @return the updated group
     * @throws GroupDomainNotModifiableException when the stated domain differs from the stored one
     * @throws GroupNameTakenException when the new name is already taken in that domain
     */
    @Transactional
    public Group replace(UUID id, GroupInput input) {
        Group group = get(id);
        if (GroupDomain.valueOf(input.domain()) != group.getDomain()) {
            throw new GroupDomainNotModifiableException();
        }
        if (!group.getName().equals(input.name())
                && groups.existsByDomainAndNameInTenant(group.getDomain(), input.name())) {
            throw new GroupNameTakenException();
        }
        group.setName(input.name());
        return group;
    }

    /**
     * Deletes a group — one explicit, audited act (BR-05, C-10). Its members survive and become
     * ungrouped through the FK's ON DELETE SET NULL (OQ-24); nothing else is removed.
     *
     * @param id the group id
     */
    @Transactional
    public void delete(UUID id) {
        Group group = get(id);
        long members = groups.countMembers(group);
        groups.remove(group);
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_GROUP_DELETED, TARGET_GROUP, id,
                "members=" + members));
    }

    /**
     * Resolves a group id for assignment to an item, enforcing the namespace rule that no foreign
     * key can express (FR-08). A foreign tenant's group is simply not found (C-01).
     *
     * @param groupId the requested group, or null for ungrouped
     * @param expected the namespace the item belongs to
     * @return the same id when it resolves and matches, or null when none was requested
     * @throws GroupNotFoundException when the group is unknown or belongs to another tenant
     * @throws GroupDomainMismatchException when the group belongs to the other namespace
     */
    public UUID resolveForAssignment(UUID groupId, GroupDomain expected) {
        if (groupId == null) {
            return null;
        }
        Group group = get(groupId);
        if (group.getDomain() != expected) {
            throw new GroupDomainMismatchException();
        }
        return group.getId();
    }

    /**
     * The aggregates for a single group — a create, read or replace response. One grouped query for
     * one group: a single-row read that published zeroed aggregates instead would report an empty
     * group where the listing reports 18 (audit F-01). Correctness over a saved statement.
     *
     * @param group the group being returned
     * @return its aggregates, never a zeroed placeholder for a populated group
     */
    public GroupAggregates aggregatesOf(Group group) {
        return aggregatesFor(group.getDomain(), List.of(group)).get(group.getId());
    }

    /**
     * The aggregates for one page of groups (FR-08, OQ-27) — one grouped query for the whole page,
     * never one per row (NFR-08). A group the query returns no row for is mapped to
     * {@link GroupAggregates#empty}, which is where an empty group's zero count and absent average
     * come from.
     *
     * @param domain the namespace being listed; decides which table is consulted (feat-014 I-8)
     * @param page the groups on the current page
     * @return aggregates by group id, one entry per group on the page
     */
    public Map<UUID, GroupAggregates> aggregatesFor(GroupDomain domain, List<Group> page) {
        Map<UUID, GroupAggregates> byGroup = new HashMap<>();
        if (page.isEmpty()) {
            return byGroup;
        }
        List<UUID> ids = page.stream().map(Group::getId).toList();
        if (domain == GroupDomain.ANNOTATION) {
            for (Object[] row : records.aggregatesByGroupInTenant(ids)) {
                byGroup.put((UUID) row[0],
                        GroupAggregates.forAnnotations((Long) row[1], (Long) row[2]));
            }
        } else {
            for (Object[] row : tasks.aggregatesByGroupInTenant(ids)) {
                byGroup.put((UUID) row[0],
                        GroupAggregates.forTasks((Long) row[1], (Double) row[2]));
            }
        }
        for (Group group : page) {
            byGroup.putIfAbsent(group.getId(), GroupAggregates.empty(domain));
        }
        return byGroup;
    }
}
