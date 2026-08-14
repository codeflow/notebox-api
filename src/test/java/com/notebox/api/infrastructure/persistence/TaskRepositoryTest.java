package com.notebox.api.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Subtask;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Persistence of the task aggregate through the tenant-scoped choke point (AD-03, BR-01, OQ-21). */
@QuarkusTest
class TaskRepositoryTest {

    @Inject
    TaskRepository repository;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    @Test
    @TestTransaction
    void persistsAggregate_withSubtasks() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        Task task = new Task(tenant.getId(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Inventory", null, null, true));
        task.addSubtask(new Subtask("Cutover", null, null, false));
        repository.persistInTenant(task);
        em.flush();
        em.clear();

        Task reloaded = repository.findByIdInTenant(task.getId()).orElseThrow();
        assertEquals("Migrate broker", reloaded.getName());
        assertEquals(Priority.HIGH, reloaded.getPriority());
        assertEquals(2, reloaded.getSubtasks().size());
        assertEquals(0, reloaded.getStatus(), "status is whatever was stored — nothing recomputes on read");
    }

    @Test
    @TestTransaction
    void crossTenantReadReturnsEmpty() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        Task taskB = new Task(tenantB.getId(), "OwnedByB", Priority.LOW);
        repository.persistInTenant(taskB);
        em.flush();
        em.clear();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        assertFalse(repository.findByIdInTenant(taskB.getId()).isPresent(),
                "tenant A cannot read tenant B's task (BR-01)");
    }

    @Test
    @TestTransaction
    void listNewestFirstInTenant_ordersNewestFirstWithIdTiebreak() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        // Created in one transaction: timestamps can collide, so the id tiebreak must decide (OQ-21).
        for (int i = 0; i < 5; i++) {
            repository.persistInTenant(new Task(tenant.getId(), "t" + i, Priority.MEDIUM));
            em.flush();
        }
        em.clear();

        List<Task> page = repository.listNewestFirstInTenant(0, 50);

        assertEquals(5, page.size());
        for (int i = 0; i < page.size() - 1; i++) {
            Task a = page.get(i);
            Task b = page.get(i + 1);
            boolean ordered = a.getCreatedAt().isAfter(b.getCreatedAt())
                    || (a.getCreatedAt().equals(b.getCreatedAt())
                            && a.getId().toString().compareTo(b.getId().toString()) > 0);
            assertTrue(ordered, "rows are createdAt desc with id desc tiebreak (OQ-21)");
        }
    }

    @Test
    @TestTransaction
    void listNewestFirstInTenant_pagesAndCounts() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        for (int i = 0; i < 7; i++) {
            repository.persistInTenant(new Task(tenant.getId(), "t" + i, Priority.LOW));
        }
        em.flush();
        em.clear();

        assertEquals(7, repository.countInTenant());
        assertEquals(3, repository.listNewestFirstInTenant(0, 3).size());
        assertEquals(3, repository.listNewestFirstInTenant(1, 3).size());
        assertEquals(1, repository.listNewestFirstInTenant(2, 3).size());
        assertEquals(0, repository.listNewestFirstInTenant(3, 3).size(),
                "a page beyond the end is empty, not an error");
    }

    @Test
    @TestTransaction
    void listNewestFirstInTenant_isTenantScoped() {
        Tenant tenantA = data.createTenant();
        Tenant tenantB = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        repository.persistInTenant(new Task(tenantB.getId(), "OwnedByB", Priority.HIGH));
        em.flush();

        when(tenantContext.tenantId()).thenReturn(tenantA.getId());
        repository.persistInTenant(new Task(tenantA.getId(), "OwnedByA", Priority.HIGH));
        em.flush();
        em.clear();

        List<Task> page = repository.listNewestFirstInTenant(0, 50);
        assertEquals(1, page.size(), "only the caller's tenant's tasks are listed (BR-01/C-01)");
        assertEquals("OwnedByA", page.get(0).getName());
        assertEquals(1, repository.countInTenant());
    }
}
