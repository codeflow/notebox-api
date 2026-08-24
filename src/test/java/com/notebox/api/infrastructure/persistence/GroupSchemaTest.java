package com.notebox.api.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * The V7 schema half of feat-014: that the group table is reachable at all under its non-reserved
 * name, that the per-domain unique key holds, and — the OQ-24 rule — that deleting a group
 * un-groups its members instead of deleting them. The service-level half (audit, 204) is T-02.
 */
@QuarkusTest
class GroupSchemaTest {

    @Inject
    AnnotationTypeRepository typeRepository;

    @Inject
    EntityManager em;

    @Inject
    TestData data;

    @InjectMock
    TenantContext tenantContext;

    private Group persistGroup(Tenant tenant, String name, GroupDomain domain) {
        Group group = new Group(tenant.getId(), name, domain);
        em.persist(group);
        em.flush();
        return group;
    }

    private AnnotationRecord persistRecord(Tenant tenant, AnnotationType type, String name, Group group) {
        AnnotationRecord record = new AnnotationRecord(tenant.getId(), type.getId(), name);
        record.setGroupId(group == null ? null : group.getId());
        em.persist(record);
        em.flush();
        return record;
    }

    private AnnotationType persistType(Tenant tenant, String name) {
        AnnotationType type = new AnnotationType(tenant.getId(), name);
        type.addField(new TypeField("Port", FieldType.NUMBER));
        typeRepository.persistInTenant(type);
        em.flush();
        return type;
    }

    private Task persistTask(Tenant tenant, String name, Group group) {
        Task task = new Task(tenant.getId(), name, Priority.HIGH);
        task.setGroupId(group == null ? null : group.getId());
        em.persist(task);
        em.flush();
        return task;
    }

    /** The table is item_group — GROUP being reserved, a plain name would fail on every statement. */
    @Test
    @TestTransaction
    void groupRoundTripsUnderItsNonReservedTableName() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        Group group = persistGroup(tenant, "Brokers", GroupDomain.ANNOTATION);
        em.clear();

        Group reloaded = em.find(Group.class, group.getId());
        assertEquals("Brokers", reloaded.getName());
        assertEquals(GroupDomain.ANNOTATION, reloaded.getDomain());
        assertNotNull(reloaded.getCreatedAt(), "@PrePersist stamps the timestamps");
    }

    /** OQ-04: the namespaces are separate, so one name may exist once in each. */
    @Test
    @TestTransaction
    void sameNameIsAllowedOncePerDomain() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        persistGroup(tenant, "Brokers", GroupDomain.ANNOTATION);
        Group taskGroup = persistGroup(tenant, "Brokers", GroupDomain.TASK);

        assertNotNull(taskGroup.getId());
    }

    @Test
    @TestTransaction
    void sameNameTwiceInOneDomainViolatesTheUniqueKey() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        persistGroup(tenant, "Brokers", GroupDomain.ANNOTATION);

        assertThrows(PersistenceException.class,
                () -> persistGroup(tenant, "Brokers", GroupDomain.ANNOTATION));
    }

    /**
     * OQ-24, annotation side: the group goes, every record survives and becomes ungrouped. Read
     * back with a native query so the assertion is about stored rows, not the persistence context.
     */
    @Test
    @TestTransaction
    void deletingAGroupUngroupsItsRecords_andDeletesNone() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = persistType(tenant, "RabbitMQ");
        Group group = persistGroup(tenant, "Brokers", GroupDomain.ANNOTATION);
        AnnotationRecord first = persistRecord(tenant, type, "prod-broker", group);
        AnnotationRecord second = persistRecord(tenant, type, "staging-broker", group);

        em.createNativeQuery("delete from item_group where id = :id")
                .setParameter("id", group.getId().toString())
                .executeUpdate();
        em.clear();

        assertEquals(2L, countRecords(first.getId(), second.getId()), "no record was deleted");
        assertNull(em.find(AnnotationRecord.class, first.getId()).getGroupId());
        assertNull(em.find(AnnotationRecord.class, second.getId()).getGroupId());
        assertNull(em.find(Group.class, group.getId()), "the group itself is gone");
    }

    /** OQ-24, task side: the same rule, so the two domains cannot drift apart. */
    @Test
    @TestTransaction
    void deletingAGroupUngroupsItsTasks_andDeletesNone() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        Group group = persistGroup(tenant, "Q3 Migration", GroupDomain.TASK);
        Task task = persistTask(tenant, "Migrate broker", group);

        em.createNativeQuery("delete from item_group where id = :id")
                .setParameter("id", group.getId().toString())
                .executeUpdate();
        em.clear();

        Task reloaded = em.find(Task.class, task.getId());
        assertNotNull(reloaded, "the task survives its group");
        assertNull(reloaded.getGroupId());
    }

    /** NULL is the one and only representation of ungrouped — nothing else needs to encode it. */
    @Test
    @TestTransaction
    void anItemWithNoGroupStoresNull() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        AnnotationType type = persistType(tenant, "Kafka");

        AnnotationRecord record = persistRecord(tenant, type, "loose-record", null);
        Task task = persistTask(tenant, "loose-task", null);
        em.clear();

        assertNull(em.find(AnnotationRecord.class, record.getId()).getGroupId());
        assertNull(em.find(Task.class, task.getId()).getGroupId());
    }

    private long countRecords(UUID... ids) {
        return em.createQuery(
                        "select count(r) from AnnotationRecord r where r.id in :ids", Long.class)
                .setParameter("ids", List.of(ids))
                .getSingleResult();
    }
}
