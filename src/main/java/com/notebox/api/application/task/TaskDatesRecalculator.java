package com.notebox.api.application.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.notebox.api.domain.Task;
import com.notebox.api.domain.event.SubtaskChange;
import com.notebox.api.infrastructure.persistence.TaskRepository;

/**
 * Recomputes a task's derived dates when its subtask set changes (BR-07, FR-12, AD-10) — the second
 * observer of the {@link SubtaskChange} seam, next to {@link TaskProgressRecalculator}. Observes
 * synchronously in the firing transaction (default IN_PROGRESS phase — deliberately not
 * AFTER_SUCCESS): the task is re-fetched through the tenant-scoped choke point, mutated in the same
 * persistence context, and the dirty dates flush atomically with the subtask change. Recomputes on
 * every change kind; a done-flip is a harmless idempotent no-op for dates.
 */
@ApplicationScoped
public class TaskDatesRecalculator {

    private final TaskRepository tasks;

    public TaskDatesRecalculator(TaskRepository tasks) {
        this.tasks = tasks;
    }

    /**
     * Recomputes the parent task's derived dates for any subtask change.
     *
     * @param change the domain fact carrying the parent task id
     */
    public void onSubtaskChange(@Observes SubtaskChange change) {
        tasks.findByIdInTenant(change.taskId()).ifPresent(Task::recomputeDates);
    }
}
