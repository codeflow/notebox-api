package com.notebox.api.application.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.GroupInput;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.GroupDomainMismatchException;
import com.notebox.api.domain.error.GroupDomainNotModifiableException;
import com.notebox.api.domain.error.GroupNameTakenException;
import com.notebox.api.domain.error.GroupNotFoundException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * Group use cases (FR-08): per-tenant-per-domain uniqueness, a domain fixed at creation, and the
 * audited delete that removes the label without touching a single member (BR-05, C-10, OQ-24).
 */
@QuarkusTest
class GroupServiceTest {

    @Inject
    GroupService service;

    @Inject
    AnnotationTypeRepository typeRepository;

    @Inject
    EntityManager em;

    @Inject
    TestData data;

    @InjectMock
    TenantContext tenantContext;

    private Tenant signedInTenant() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());
        return tenant;
    }

    private static GroupInput input(String name, GroupDomain domain) {
        return new GroupInput(name, domain.name());
    }

    @Test
    @TestTransaction
    void createsAGroupInEachNamespace() {
        signedInTenant();

        Group annotationGroup = service.create(input("Brokers", GroupDomain.ANNOTATION));
        Group taskGroup = service.create(input("Q3 Migration", GroupDomain.TASK));

        assertEquals(GroupDomain.ANNOTATION, annotationGroup.getDomain());
        assertEquals(GroupDomain.TASK, taskGroup.getDomain());
        assertNotNull(annotationGroup.getId());
    }

    /** OQ-04: the namespaces are separate, so a name taken in one is still free in the other. */
    @Test
    @TestTransaction
    void theSameNameIsFreeInTheOtherDomainButTakenInItsOwn() {
        signedInTenant();
        service.create(input("Brokers", GroupDomain.ANNOTATION));

        Group taskGroup = service.create(input("Brokers", GroupDomain.TASK));
        assertNotNull(taskGroup.getId());

        assertThrows(GroupNameTakenException.class,
                () -> service.create(input("Brokers", GroupDomain.ANNOTATION)));
        assertEquals(1, service.count(GroupDomain.ANNOTATION));
    }

    /** Uniqueness is scoped to the tenant — another tenant's name never collides (BR-01). */
    @Test
    @TestTransaction
    void uniquenessIsScopedToTheTenant() {
        Tenant other = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(other.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());
        service.create(input("Brokers", GroupDomain.ANNOTATION));

        signedInTenant();
        Group mine = service.create(input("Brokers", GroupDomain.ANNOTATION));

        assertNotNull(mine.getId());
    }

    @Test
    @TestTransaction
    void aGroupsDomainIsFixedAtCreation() {
        signedInTenant();
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();

        assertThrows(GroupDomainNotModifiableException.class,
                () -> service.replace(group.getId(), input("Brokers", GroupDomain.TASK)));
        assertEquals(GroupDomain.ANNOTATION, service.get(group.getId()).getDomain());
    }

    @Test
    @TestTransaction
    void renamingKeepsTheMembersAttached() {
        Tenant tenant = signedInTenant();
        AnnotationType type = persistType(tenant, "RabbitMQ");
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        List<UUID> members = List.of(
                persistRecord(tenant, type, "one", group.getId()),
                persistRecord(tenant, type, "two", group.getId()),
                persistRecord(tenant, type, "three", group.getId()));
        em.flush();

        service.replace(group.getId(), input("Message Brokers", GroupDomain.ANNOTATION));
        em.flush();
        em.clear();

        assertEquals("Message Brokers", service.get(group.getId()).getName());
        for (UUID id : members) {
            assertEquals(group.getId(), em.find(AnnotationRecord.class, id).getGroupId());
        }
    }

    /** Renaming a group to its own current name must not trip the uniqueness guard. */
    @Test
    @TestTransaction
    void renamingToItsOwnNameIsNotADuplicate() {
        signedInTenant();
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();

        Group same = service.replace(group.getId(), input("Brokers", GroupDomain.ANNOTATION));

        assertEquals("Brokers", same.getName());
    }

    @Test
    @TestTransaction
    void renamingOntoAnExistingNameIsRejected() {
        signedInTenant();
        service.create(input("Brokers", GroupDomain.ANNOTATION));
        Group second = service.create(input("Datastores", GroupDomain.ANNOTATION));
        em.flush();

        assertThrows(GroupNameTakenException.class,
                () -> service.replace(second.getId(), input("Brokers", GroupDomain.ANNOTATION)));
    }

    /**
     * OQ-24 at the service level: the label goes, every member survives and is ungrouped. Two
     * groups, one per namespace — a single group holding both a record and a task is a state the
     * API rejects, so using one here would prove the FK against data that cannot exist (audit
     * F-02, second instance).
     */
    @Test
    @TestTransaction
    void deletingAGroupUngroupsItsMembersAndDeletesNone() {
        Tenant tenant = signedInTenant();
        AnnotationType type = persistType(tenant, "RabbitMQ");
        Group annotationGroup = service.create(input("Brokers", GroupDomain.ANNOTATION));
        Group taskGroup = service.create(input("Q3 Migration", GroupDomain.TASK));
        UUID recordId = persistRecord(tenant, type, "prod-broker", annotationGroup.getId());
        UUID taskId = persistTask(tenant, "Migrate broker", taskGroup.getId());
        em.flush();

        service.delete(annotationGroup.getId());
        service.delete(taskGroup.getId());
        em.flush();
        em.clear();

        assertNotNull(em.find(AnnotationRecord.class, recordId), "the record survives its group");
        assertNull(em.find(AnnotationRecord.class, recordId).getGroupId());
        assertNotNull(em.find(Task.class, taskId), "the task survives its group");
        assertNull(em.find(Task.class, taskId).getGroupId());
        assertThrows(GroupNotFoundException.class, () -> service.get(annotationGroup.getId()));
        assertThrows(GroupNotFoundException.class, () -> service.get(taskGroup.getId()));
    }

    /**
     * C-10: who, what, when — and the member count taken BEFORE the rows are un-grouped. The
     * fixture stays inside one namespace, as the API enforces: a group holding both records and
     * tasks is exactly what {@code resolveForAssignment} rejects, so counting it here would assert
     * a state that cannot occur (audit F-02). Cross-domain membership is GroupAssignmentTest's.
     */
    @Test
    @TestTransaction
    void deletingAGroupIsAudited() {
        Tenant tenant = signedInTenant();
        UUID actor = UUID.randomUUID();
        when(tenantContext.userId()).thenReturn(actor);
        AnnotationType type = persistType(tenant, "RabbitMQ");
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        persistRecord(tenant, type, "one", group.getId());
        persistRecord(tenant, type, "two", group.getId());
        persistRecord(tenant, type, "three", group.getId());
        em.flush();
        UUID groupId = group.getId();

        service.delete(groupId);
        em.flush();

        AuditLog entry = em.createQuery(
                        "select a from AuditLog a where a.targetId = :id and a.action = 'GROUP_DELETED'",
                        AuditLog.class)
                .setParameter("id", groupId)
                .getSingleResult();
        assertEquals(actor, entry.getActorUserId(), "the entry records WHO deleted");
        assertEquals("GROUP", entry.getTargetType());
        assertNotNull(entry.getAt(), "the entry records WHEN");
        assertEquals("members=3", entry.getDetail(),
                "the count is taken before the delete — afterwards every member reads as ungrouped");
    }

    @Test
    @TestTransaction
    void anEmptyGroupAuditsZeroMembers() {
        signedInTenant();
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();
        UUID groupId = group.getId();

        service.delete(groupId);
        em.flush();

        AuditLog entry = em.createQuery(
                        "select a from AuditLog a where a.targetId = :id and a.action = 'GROUP_DELETED'",
                        AuditLog.class)
                .setParameter("id", groupId)
                .getSingleResult();
        assertEquals("members=0", entry.getDetail());
    }

    /** C-01: another tenant's group is indistinguishable from one that never existed. */
    @Test
    @TestTransaction
    void aForeignTenantsGroupIsNotFound() {
        Tenant other = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(other.getId());
        when(tenantContext.userId()).thenReturn(UUID.randomUUID());
        Group theirs = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();

        signedInTenant();

        assertThrows(GroupNotFoundException.class, () -> service.get(theirs.getId()));
        assertThrows(GroupNotFoundException.class, () -> service.delete(theirs.getId()));
    }

    @Test
    @TestTransaction
    void resolveForAssignmentAcceptsAGroupOfTheItemsOwnDomain() {
        signedInTenant();
        Group group = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();

        assertEquals(group.getId(),
                service.resolveForAssignment(group.getId(), GroupDomain.ANNOTATION));
        assertNull(service.resolveForAssignment(null, GroupDomain.ANNOTATION),
                "no group requested stays ungrouped");
    }

    /** I-8 — the one rule a foreign key cannot express. */
    @Test
    @TestTransaction
    void resolveForAssignmentRejectsTheOtherDomain() {
        signedInTenant();
        Group annotationGroup = service.create(input("Brokers", GroupDomain.ANNOTATION));
        em.flush();

        assertThrows(GroupDomainMismatchException.class,
                () -> service.resolveForAssignment(annotationGroup.getId(), GroupDomain.TASK));
    }

    @Test
    @TestTransaction
    void listingIsOrderedByNameCaseInsensitively() {
        signedInTenant();
        service.create(input("gamma", GroupDomain.ANNOTATION));
        service.create(input("Beta", GroupDomain.ANNOTATION));
        service.create(input("alpha", GroupDomain.ANNOTATION));
        em.flush();

        List<String> names = service.list(GroupDomain.ANNOTATION, 0, 50).stream()
                .map(Group::getName)
                .toList();

        assertEquals(List.of("alpha", "Beta", "gamma"), names);
    }

    @Test
    @TestTransaction
    void listingIsScopedToOneDomain() {
        signedInTenant();
        service.create(input("Brokers", GroupDomain.ANNOTATION));
        service.create(input("Q3 Migration", GroupDomain.TASK));
        em.flush();

        assertEquals(1, service.list(GroupDomain.ANNOTATION, 0, 50).size());
        assertEquals(1, service.count(GroupDomain.TASK));
    }

    private AnnotationType persistType(Tenant tenant, String name) {
        AnnotationType type = new AnnotationType(tenant.getId(), name);
        type.addField(new TypeField("Port", FieldType.NUMBER));
        typeRepository.persistInTenant(type);
        em.flush();
        return type;
    }

    private UUID persistRecord(Tenant tenant, AnnotationType type, String name, UUID groupId) {
        AnnotationRecord record = new AnnotationRecord(tenant.getId(), type.getId(), name);
        record.setGroupId(groupId);
        em.persist(record);
        return record.getId();
    }

    private UUID persistTask(Tenant tenant, String name, UUID groupId) {
        Task task = new Task(tenant.getId(), name, Priority.HIGH);
        task.setGroupId(groupId);
        em.persist(task);
        return task.getId();
    }
}
