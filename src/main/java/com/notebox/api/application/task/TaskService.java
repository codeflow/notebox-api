package com.notebox.api.application.task;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.CardInput;
import com.notebox.api.api.dto.SubtaskInput;
import com.notebox.api.api.dto.TaskInput;
import com.notebox.api.application.content.RichTextSanitizer;
import com.notebox.api.application.group.GroupFilter;
import com.notebox.api.application.group.GroupService;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.Card;
import com.notebox.api.domain.GroupDomain;
import com.notebox.api.domain.Priority;
import com.notebox.api.domain.Subtask;
import com.notebox.api.domain.Task;
import com.notebox.api.domain.error.SubtaskNotFoundException;
import com.notebox.api.domain.error.TaskDetailsTooLongException;
import com.notebox.api.domain.error.TaskNotFoundException;
import com.notebox.api.domain.event.SubtaskAdded;
import com.notebox.api.domain.event.SubtaskChange;
import com.notebox.api.domain.event.SubtaskCompleted;
import com.notebox.api.domain.event.SubtaskRemoved;
import com.notebox.api.domain.event.SubtaskRescheduled;
import com.notebox.api.domain.event.SubtaskUncompleted;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for tasks and their subtasks (FR-10..FR-14): CRUD behind the tenant choke point
 * (AD-03), the inline card on task and subtask (FR-13), rich-text details sanitized to the shared
 * dialect before the storage bound (FR-14, C-08), audited irreversible deletes (BR-05, C-10), and
 * the AD-10 event seam — every subtask mutation first mutates the aggregate, then fires the matching
 * {@link SubtaskChange} fact so the recalculators recompute the derived status (BR-06) and dates
 * (BR-07) in the same transaction. Neither is ever assigned here; only the observers' recomputes
 * write them. Updates are replace-semantics: an omitted card or details clears it.
 */
@ApplicationScoped
public class TaskService {

    private static final String TARGET_TASK = "TASK";
    private static final String TARGET_SUBTASK = "SUBTASK";
    private static final String ACTION_TASK_DELETED = "TASK_DELETED";
    private static final String ACTION_SUBTASK_DELETED = "SUBTASK_DELETED";
    /** The TEXT column's capacity in UTF-8 bytes — the same bound feat-005 applies to text values. */
    private static final int DETAILS_MAX_BYTES = 65535;

    private final TaskRepository tasks;
    private final AuditLogRepository auditLog;
    private final Event<SubtaskChange> events;
    private final TenantContext tenant;
    private final RichTextSanitizer sanitizer;
    private final GroupService groups;

    public TaskService(
            TaskRepository tasks,
            AuditLogRepository auditLog,
            Event<SubtaskChange> events,
            TenantContext tenant,
            RichTextSanitizer sanitizer,
            GroupService groups) {
        this.tasks = tasks;
        this.auditLog = auditLog;
        this.events = events;
        this.tenant = tenant;
        this.sanitizer = sanitizer;
        this.groups = groups;
    }

    @Transactional
    public Task create(TaskInput input) {
        Task task = new Task(tenant.tenantId(), input.name(), Priority.valueOf(input.priority()));
        task.setCard(toCard(input.card()));
        task.setDetails(sanitizedDetails(input.details()));
        task.setGroupId(groups.resolveForAssignment(input.groupId(), GroupDomain.TASK));
        return tasks.persistInTenant(task);
    }

    /** Reads one task with its subtasks; a foreign tenant's id behaves like a missing one (C-01). */
    public Task get(UUID id) {
        return tasks.findByIdInTenant(id).orElseThrow(TaskNotFoundException::new);
    }

    /** One page, newest first — createdAt desc, id desc (NFR-08, OQ-21). */
    public List<Task> list(GroupFilter filter, int page, int size) {
        return tasks.listByGroupNewestFirstInTenant(filter, page, size);
    }

    /** Total tasks of the caller's tenant — the pager fact behind the page above (NFR-08). */
    public long count(GroupFilter filter) {
        return tasks.countByGroupInTenant(filter);
    }

    /**
     * Replaces name, priority, card, details and group (PUT — an omitted one clears it); the
     * derived status and dates are untouched by design (BR-06, BR-07).
     */
    @Transactional
    public Task update(UUID id, TaskInput input) {
        Task task = get(id);
        task.setName(input.name());
        task.setPriority(Priority.valueOf(input.priority()));
        task.setCard(toCard(input.card()));
        task.setDetails(sanitizedDetails(input.details()));
        task.setGroupId(groups.resolveForAssignment(input.groupId(), GroupDomain.TASK));
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
        Subtask subtask = new Subtask(
                input.name(), input.startDate(), input.endDate(), input.doneOrFalse());
        subtask.setCard(toCard(input.card()));
        task.addSubtask(subtask);
        events.fire(new SubtaskAdded(taskId));
        return task;
    }

    /**
     * Replaces the subtask's fields (PUT); a done-flag flip fires the matching fact and a change of
     * the (start, end) pair fires {@link SubtaskRescheduled} (AD-10). A name-only update fires nothing.
     */
    @Transactional
    public Task updateSubtask(UUID taskId, UUID subtaskId, SubtaskInput input) {
        Task task = get(taskId);
        Subtask subtask = task.subtask(subtaskId).orElseThrow(SubtaskNotFoundException::new);
        boolean wasDone = subtask.isDone();
        boolean rescheduled = !Objects.equals(subtask.getStartDate(), input.startDate())
                || !Objects.equals(subtask.getEndDate(), input.endDate());
        subtask.setName(input.name());
        subtask.setStartDate(input.startDate());
        subtask.setEndDate(input.endDate());
        subtask.setDone(input.doneOrFalse());
        subtask.setCard(toCard(input.card()));
        if (!wasDone && subtask.isDone()) {
            events.fire(new SubtaskCompleted(taskId));
        } else if (wasDone && !subtask.isDone()) {
            events.fire(new SubtaskUncompleted(taskId));
        }
        if (rescheduled) {
            events.fire(new SubtaskRescheduled(taskId));
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

    /** The inline card value for an input — null input means no card (replace semantics, OQ-06). */
    private static Card toCard(CardInput input) {
        return input == null ? null : new Card(input.code(), input.url());
    }

    /**
     * Sanitizes details to the shared dialect FIRST, then applies the storage bound to what will
     * actually be persisted (C-08 input half; feat-005 ordering) — null stays null.
     *
     * @throws TaskDetailsTooLongException when the sanitized value exceeds the column capacity
     */
    private String sanitizedDetails(String details) {
        if (details == null) {
            return null;
        }
        String sanitized = sanitizer.sanitize(details);
        if (sanitized.getBytes(StandardCharsets.UTF_8).length > DETAILS_MAX_BYTES) {
            throw new TaskDetailsTooLongException();
        }
        return sanitized;
    }

    /**
     * Subtask counts for a page of tasks, keyed by task id. Absence means zero — the query only
     * returns tasks that HAVE subtasks, and flattening that to a stored zero would be a second
     * source of truth for the same fact.
     *
     * @param page the page just listed
     * @return count by task id, missing entries meaning none
     */
    public Map<UUID, Long> subtaskCounts(List<Task> page) {
        List<UUID> ids = page.stream().map(Task::getId).toList();
        return tasks.subtaskCountsFor(ids).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }
}
