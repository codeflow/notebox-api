package com.notebox.api.application.task;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.SubtaskInput;
import com.notebox.api.api.dto.TaskInput;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Subtask;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.error.SubtaskNotFoundException;
import com.notebox.api.domain.error.TaskNotFoundException;
import com.notebox.api.domain.event.SubtaskAdded;
import com.notebox.api.domain.event.SubtaskChange;
import com.notebox.api.domain.event.SubtaskCompleted;
import com.notebox.api.domain.event.SubtaskRemoved;
import com.notebox.api.domain.event.SubtaskUncompleted;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for tasks and their subtasks (FR-10, FR-11): CRUD behind the tenant choke point
 * (AD-03), audited irreversible deletes (BR-05, C-10), and the AD-10 event seam — every subtask
 * mutation first mutates the aggregate, then fires the matching {@link SubtaskChange} fact so the
 * recalculator recomputes the derived status (BR-06) in the same transaction. Status is never
 * assigned here; only the observer's recompute writes it.
 */
@ApplicationScoped
public class TaskService {

    private static final String TARGET_TASK = "TASK";
    private static final String TARGET_SUBTASK = "SUBTASK";
    private static final String ACTION_TASK_DELETED = "TASK_DELETED";
    private static final String ACTION_SUBTASK_DELETED = "SUBTASK_DELETED";

    private final TaskRepository tasks;
    private final AuditLogRepository auditLog;
    private final Event<SubtaskChange> events;
    private final TenantContext tenant;

    public TaskService(
            TaskRepository tasks,
            AuditLogRepository auditLog,
            Event<SubtaskChange> events,
            TenantContext tenant) {
        this.tasks = tasks;
        this.auditLog = auditLog;
        this.events = events;
        this.tenant = tenant;
    }

    @Transactional
    public Task create(TaskInput input) {
        Task task = new Task(tenant.tenantId(), input.name(), Priority.valueOf(input.priority()));
        return tasks.persistInTenant(task);
    }

    /** Reads one task with its subtasks; a foreign tenant's id behaves like a missing one (C-01). */
    public Task get(UUID id) {
        return tasks.findByIdInTenant(id).orElseThrow(TaskNotFoundException::new);
    }

    /** One page, newest first — createdAt desc, id desc (NFR-08, OQ-21). */
    public List<Task> list(int page, int size) {
        return tasks.listNewestFirstInTenant(page, size);
    }

    /** Total tasks of the caller's tenant — the pager fact behind the page above (NFR-08). */
    public long count() {
        return tasks.countInTenant();
    }

    /** Updates name and priority only — the derived status is untouched by design (BR-06). */
    @Transactional
    public Task update(UUID id, TaskInput input) {
        Task task = get(id);
        task.setName(input.name());
        task.setPriority(Priority.valueOf(input.priority()));
        return task;
    }

    /** Deletes the task and, with it, its subtasks — one explicit, audited act (BR-05, C-10). */
    @Transactional
    public void delete(UUID id) {
        Task task = get(id);
        int subtaskCount = task.getSubtasks().size();
        tasks.remove(task);
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_TASK_DELETED, TARGET_TASK, id,
                "subtasks=" + subtaskCount));
    }

    @Transactional
    public Task addSubtask(UUID taskId, SubtaskInput input) {
        Task task = get(taskId);
        task.addSubtask(new Subtask(
                input.name(), input.startDate(), input.endDate(), input.doneOrFalse()));
        events.fire(new SubtaskAdded(taskId));
        return task;
    }

    /** Replaces the subtask's fields (PUT); a done-flag flip fires the matching fact (AD-10). */
    @Transactional
    public Task updateSubtask(UUID taskId, UUID subtaskId, SubtaskInput input) {
        Task task = get(taskId);
        Subtask subtask = task.subtask(subtaskId).orElseThrow(SubtaskNotFoundException::new);
        boolean wasDone = subtask.isDone();
        subtask.setName(input.name());
        subtask.setStartDate(input.startDate());
        subtask.setEndDate(input.endDate());
        subtask.setDone(input.doneOrFalse());
        if (!wasDone && subtask.isDone()) {
            events.fire(new SubtaskCompleted(taskId));
        } else if (wasDone && !subtask.isDone()) {
            events.fire(new SubtaskUncompleted(taskId));
        }
        return task;
    }

    /** Deletes one subtask — explicit and audited (BR-05, C-10) — and recomputes the parent. */
    @Transactional
    public Task removeSubtask(UUID taskId, UUID subtaskId) {
        Task task = get(taskId);
        if (!task.removeSubtask(subtaskId)) {
            throw new SubtaskNotFoundException();
        }
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_SUBTASK_DELETED, TARGET_SUBTASK, subtaskId,
                "taskId=" + taskId));
        events.fire(new SubtaskRemoved(taskId));
        return task;
    }
}
