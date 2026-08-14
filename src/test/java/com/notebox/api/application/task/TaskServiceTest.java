package com.notebox.api.application.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import com.notebox.api.api.dto.SubtaskInput;
import com.notebox.api.api.dto.TaskInput;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.User;
import com.notebox.api.domain.error.SubtaskNotFoundException;
import com.notebox.api.domain.error.TaskNotFoundException;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/**
 * Service-level behavior of FR-10/FR-11: recompute wiring through every subtask mutation kind and
 * both flip directions (BR-06, AD-10), audited irreversible deletes (BR-05, C-10), and the
 * foreign-tenant-behaves-like-missing rule (C-01).
 */
@QuarkusTest
class TaskServiceTest {

    @Inject
    TaskService service;

    @Inject
    TaskRepository repository;

    @Inject
    AuditLogRepository auditLog;

    @Inject
    TestData data;

    @Inject
    EntityManager em;

    @InjectMock
    TenantContext tenantContext;

    private Tenant actAsFreshTenant() {
        Tenant tenant = data.createTenant();
        User member = data.createUser(
                tenant.getId(), UUID.randomUUID() + "@tasks.test", "pw-tasks-1", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenant.getId());
        when(tenantContext.userId()).thenReturn(member.getId());
        return tenant;
    }

    private static SubtaskInput subtask(String name, boolean done) {
        return new SubtaskInput(name, null, null, done);
    }

    @Test
    @TestTransaction
    void create_minimumShape_statusZero() {
        actAsFreshTenant();

        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        em.flush();

        assertEquals(0, task.getStatus());
        assertEquals(Priority.HIGH, task.getPriority());
    }

    @Test
    @TestTransaction
    void updateSubtask_markSecondOfFourDone_fifty() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Ops review", "MEDIUM", null));
        service.addSubtask(task.getId(), subtask("s1", true));
        service.addSubtask(task.getId(), subtask("s2", false));
        service.addSubtask(task.getId(), subtask("s3", false));
        service.addSubtask(task.getId(), subtask("s4", false));
        em.flush();
        assertEquals(25, task.getStatus(), "1 of 4 done before the flip");
        UUID secondId = task.getSubtasks().get(1).getId();

        service.updateSubtask(task.getId(), secondId, subtask("s2", true));
        em.flush();

        assertEquals(50, task.getStatus(), "PRD §4 example: the second done subtask makes 2/4 = 50%");
    }

    @Test
    @TestTransaction
    void addSubtask_toFullyDoneTask_recomputesDownward() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        service.addSubtask(task.getId(), subtask("Inventory", true));
        em.flush();
        assertEquals(100, task.getStatus());

        service.addSubtask(task.getId(), subtask("Validate backups", false));
        em.flush();

        assertEquals(50, task.getStatus());
    }

    @Test
    @TestTransaction
    void updateSubtask_unmarkDone_recomputesDownward() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        service.addSubtask(task.getId(), subtask("a", true));
        service.addSubtask(task.getId(), subtask("b", true));
        em.flush();
        assertEquals(100, task.getStatus());
        UUID firstId = task.getSubtasks().get(0).getId();

        service.updateSubtask(task.getId(), firstId, subtask("a", false));
        em.flush();

        assertEquals(50, task.getStatus());
    }

    @Test
    @TestTransaction
    void removeSubtask_lastDoneOne_zeroAndAudited() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        service.addSubtask(task.getId(), subtask("Inventory", true));
        service.addSubtask(task.getId(), subtask("b", false));
        service.addSubtask(task.getId(), subtask("c", false));
        service.addSubtask(task.getId(), subtask("d", false));
        em.flush();
        assertEquals(25, task.getStatus());
        UUID doneId = task.getSubtasks().get(0).getId();

        service.removeSubtask(task.getId(), doneId);
        em.flush();

        assertEquals(3, task.getSubtasks().size());
        assertEquals(0, task.getStatus());
        assertEquals(1, auditLog.countForTargetAndAction(doneId, "SUBTASK_DELETED"),
                "the subtask deletion is audited (BR-05, C-10)");
    }

    @Test
    @TestTransaction
    void update_nameAndPriority_statusUntouched() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        service.addSubtask(task.getId(), subtask("a", true));
        service.addSubtask(task.getId(), subtask("b", false));
        em.flush();
        assertEquals(50, task.getStatus());

        Task updated = service.update(task.getId(), new TaskInput("Migrate RabbitMQ", "CRITICAL", null));
        em.flush();

        assertEquals("Migrate RabbitMQ", updated.getName());
        assertEquals(Priority.CRITICAL, updated.getPriority());
        assertEquals(50, updated.getStatus(), "an update never recomputes nor assigns status (BR-06)");
    }

    @Test
    @TestTransaction
    void delete_taskWithSubtasks_cascadesAndAudits() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        service.addSubtask(task.getId(), subtask("a", false));
        service.addSubtask(task.getId(), subtask("b", false));
        service.addSubtask(task.getId(), subtask("c", false));
        em.flush();
        UUID taskId = task.getId();

        service.delete(taskId);
        em.flush();
        em.clear();

        assertTrue(repository.findByIdInTenant(taskId).isEmpty(), "the task is gone");
        Number orphanSubtasks = (Number) em.createNativeQuery(
                        "select count(*) from subtask where task_id = ?1")
                .setParameter(1, taskId.toString())
                .getSingleResult();
        assertEquals(0, orphanSubtasks.longValue(), "its subtasks went with it — one explicit act (BR-05)");
        assertEquals(1, auditLog.countForTargetAndAction(taskId, "TASK_DELETED"),
                "the task deletion is audited with who/what/when (C-10)");
    }

    @Test
    @TestTransaction
    void get_foreignTenantTask_behavesLikeMissing() {
        Tenant tenantB = data.createTenant();
        User memberB = data.createUser(
                tenantB.getId(), UUID.randomUUID() + "@tasks.test", "pw-tasks-2", Role.MEMBER);
        when(tenantContext.tenantId()).thenReturn(tenantB.getId());
        when(tenantContext.userId()).thenReturn(memberB.getId());
        Task taskB = service.create(new TaskInput("OwnedByB", "LOW", null));
        em.flush();

        actAsFreshTenant();
        assertThrows(TaskNotFoundException.class, () -> service.get(taskB.getId()),
                "a foreign tenant's task is indistinguishable from a missing one (C-01)");
    }

    @Test
    @TestTransaction
    void updateSubtask_unknownSubtask_throwsSubtaskNotFound() {
        actAsFreshTenant();
        Task task = service.create(new TaskInput("Migrate broker", "HIGH", null));
        em.flush();

        assertThrows(SubtaskNotFoundException.class,
                () -> service.updateSubtask(task.getId(), UUID.randomUUID(), subtask("x", true)));
    }
}
