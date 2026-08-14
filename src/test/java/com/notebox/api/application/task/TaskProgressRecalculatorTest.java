package com.notebox.api.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Subtask;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.event.SubtaskChange;
import com.notebox.api.domain.event.SubtaskCompleted;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * Proves the AD-10 wiring end to end — the first CDI event/observer pair in the codebase: firing a
 * {@link SubtaskChange} inside a transaction recomputes the stored status (BR-06) in the same
 * persistence context.
 */
@QuarkusTest
class TaskProgressRecalculatorTest {

    @Inject
    Event<SubtaskChange> events;

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
    void onSubtaskChange_firedInTransaction_recomputesStoredStatus() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        Task task = new Task(tenant.getId(), "Migrate broker", Priority.HIGH);
        task.addSubtask(new Subtask("Inventory", null, null, true));
        task.addSubtask(new Subtask("Cutover", null, null, false));
        repository.persistInTenant(task);
        em.flush();
        assertEquals(0, task.getStatus(), "constructor status — nothing has recomputed yet");

        events.fire(new SubtaskCompleted(task.getId()));
        em.flush();
        em.clear();

        Task reloaded = repository.findByIdInTenant(task.getId()).orElseThrow();
        assertEquals(50, reloaded.getStatus(),
                "the observer recomputed 1-of-2-done in the firing transaction (AD-10, BR-06)");
    }
}
