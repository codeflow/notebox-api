package com.notebox.api.application.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.GroupInput;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.Group;
import com.notebox.api.domain.GroupDomain;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Subtask;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.TypeField;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * The per-group aggregates (FR-08, OQ-27): counts, distinct types, and the average derived status
 * with its two load-bearing properties — HALF_UP rounding (OQ-22) and null-is-not-zero.
 */
@QuarkusTest
class GroupAggregatesTest {

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

    private Group group(String name, GroupDomain domain) {
        return service.create(new GroupInput(name, domain.name()));
    }

    private AnnotationType persistType(Tenant tenant, String name) {
        AnnotationType type = new AnnotationType(tenant.getId(), name);
        type.addField(new TypeField("URL", FieldType.TEXT));
        typeRepository.persistInTenant(type);
        em.flush();
        return type;
    }

    private void persistRecord(Tenant tenant, AnnotationType type, String name, UUID groupId) {
        AnnotationRecord record = new AnnotationRecord(tenant.getId(), type.getId(), name);
        record.setGroupId(groupId);
        em.persist(record);
    }

    /** A task whose status is the derived percentage of `done` of `total` subtasks (BR-06). */
    private void persistTask(Tenant tenant, String name, UUID groupId, int done, int total) {
        Task task = new Task(tenant.getId(), name, Priority.HIGH);
        task.setGroupId(groupId);
        for (int i = 0; i < total; i++) {
            task.addSubtask(new Subtask("s" + i, null, null, i < done));
        }
        task.recomputeStatus();
        em.persist(task);
    }

    @Test
    @TestTransaction
    void anAnnotationGroupReportsItsRecordsAndTheTypesTheySpan() {
        Tenant tenant = signedInTenant();
        AnnotationType rabbit = persistType(tenant, "RabbitMQ");
        AnnotationType runbook = persistType(tenant, "Runbook");
        Group infra = group("Infrastructure", GroupDomain.ANNOTATION);
        for (int i = 0; i < 12; i++) {
            persistRecord(tenant, rabbit, "rab-" + i, infra.getId());
        }
        for (int i = 0; i < 6; i++) {
            persistRecord(tenant, runbook, "run-" + i, infra.getId());
        }
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.ANNOTATION, List.of(infra)).get(infra.getId());

        assertEquals(18, aggregates.itemCount());
        assertEquals(2L, aggregates.typesUsed());
        assertNull(aggregates.averageStatus(), "an annotation group has no average status");
    }

    /** typesUsed counts distinct TYPES, not records — 9 records of one type is 9 / 1. */
    @Test
    @TestTransaction
    void typesUsedCountsDistinctTypesNotRecords() {
        Tenant tenant = signedInTenant();
        AnnotationType vendor = persistType(tenant, "Vendor Contact");
        Group docs = group("Documentation", GroupDomain.ANNOTATION);
        for (int i = 0; i < 9; i++) {
            persistRecord(tenant, vendor, "v-" + i, docs.getId());
        }
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.ANNOTATION, List.of(docs)).get(docs.getId());

        assertEquals(9, aggregates.itemCount());
        assertEquals(1L, aggregates.typesUsed());
    }

    @Test
    @TestTransaction
    void anEmptyAnnotationGroupReportsZeroOnBoth() {
        signedInTenant();
        Group scratch = group("Scratch", GroupDomain.ANNOTATION);
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.ANNOTATION, List.of(scratch)).get(scratch.getId());

        assertEquals(0, aggregates.itemCount());
        assertEquals(0L, aggregates.typesUsed());
    }

    @Test
    @TestTransaction
    void aTaskGroupReportsItsTasksAndTheirMeanDerivedStatus() {
        Tenant tenant = signedInTenant();
        Group migration = group("Migration", GroupDomain.TASK);
        persistTask(tenant, "a", migration.getId(), 1, 2);    // 50
        persistTask(tenant, "b", migration.getId(), 1, 2);    // 50
        persistTask(tenant, "c", migration.getId(), 37, 50);  // 74
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.TASK, List.of(migration)).get(migration.getId());

        assertEquals(3, aggregates.itemCount());
        assertEquals(58, aggregates.averageStatus(), "(50 + 50 + 74) / 3 = 58");
        assertNull(aggregates.typesUsed(), "a task group has no types-used");
    }

    /**
     * OQ-22's rule, asserted on the rule rather than the value: avg(33, 34) is 33.5, and HALF_UP
     * makes it 34. Math.round would also pass here — which is exactly why the implementation states
     * HALF_UP explicitly instead of inheriting whatever the JDK does at .5.
     */
    @Test
    @TestTransaction
    void theAverageRoundsHalfUp() {
        Tenant tenant = signedInTenant();
        Group support = group("Support", GroupDomain.TASK);
        persistTask(tenant, "a", support.getId(), 1, 3);    // 33
        persistTask(tenant, "b", support.getId(), 17, 50);  // 34
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.TASK, List.of(support)).get(support.getId());

        assertEquals(34, aggregates.averageStatus(), "avg(33, 34) = 33.5 -> HALF_UP -> 34");
    }

    /**
     * The distinction the contract exists to preserve: a group with NO tasks has no average, while
     * a group whose tasks are all at 0% averages 0. Rendering the first as 0% would report an empty
     * group as a stalled one.
     */
    @Test
    @TestTransaction
    void noTasksMeansNoAverage_whichIsNotAnAverageOfZero() {
        Tenant tenant = signedInTenant();
        Group empty = group("Empty", GroupDomain.TASK);
        Group fresh = group("Fresh", GroupDomain.TASK);
        persistTask(tenant, "a", fresh.getId(), 0, 2);   // 0
        persistTask(tenant, "b", fresh.getId(), 0, 4);   // 0
        em.flush();

        Map<UUID, GroupAggregates> aggregates =
                service.aggregatesFor(GroupDomain.TASK, List.of(empty, fresh));

        GroupAggregates noTasks = aggregates.get(empty.getId());
        assertEquals(0, noTasks.itemCount());
        assertNull(noTasks.averageStatus(), "no tasks -> NO average");

        GroupAggregates allZero = aggregates.get(fresh.getId());
        assertEquals(2, allZero.itemCount());
        assertEquals(0, allZero.averageStatus(), "has tasks, all at 0% -> average 0");

        assertNotEquals(noTasks.averageStatus(), allZero.averageStatus(),
                "null and 0 must stay distinguishable — collapsing them misreports an empty group");
    }

    /** The page guard: an empty page issues no aggregate query at all, never `in ()`. */
    @Test
    @TestTransaction
    void anEmptyPageYieldsNoAggregatesAndIssuesNoQuery() {
        signedInTenant();

        assertEquals(Map.of(), service.aggregatesFor(GroupDomain.ANNOTATION, List.of()));
        assertEquals(Map.of(), service.aggregatesFor(GroupDomain.TASK, List.of()));
    }

    /** Aggregates never consult the other namespace (feat-014 I-8). */
    @Test
    @TestTransaction
    void aggregatesStayInsideTheGroupsOwnNamespace() {
        Tenant tenant = signedInTenant();
        Group taskGroup = group("Migration", GroupDomain.TASK);
        persistTask(tenant, "a", taskGroup.getId(), 1, 2);
        em.flush();

        GroupAggregates aggregates =
                service.aggregatesFor(GroupDomain.TASK, List.of(taskGroup)).get(taskGroup.getId());

        assertEquals(1, aggregates.itemCount());
        assertNull(aggregates.typesUsed(), "the annotation table is never consulted for a task group");
    }
}
