package com.notebox.api.application.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import com.notebox.api.domain.Task;
import com.notebox.api.domain.event.SubtaskChange;
import com.notebox.api.infrastructure.persistence.TaskRepository;

/**
 * Recomputes a task's derived status when its subtask set changes (BR-06, AD-10). Observes
 * synchronously in the firing transaction (default IN_PROGRESS phase — deliberately not
 * AFTER_SUCCESS): the task is re-fetched through the tenant-scoped choke point, mutated in the
 * same persistence context, and the dirty status flushes atomically with the subtask change.
 */
@ApplicationScoped
public class TaskProgressRecalculator {

    private final TaskRepository tasks;

    public TaskProgressRecalculator(TaskRepository tasks) {
        this.tasks = tasks;
    }

    /**
     * Recomputes the parent task's status for any subtask change.
     *
     * @param change the domain fact carrying the parent task id
     */
    public void onSubtaskChange(@Observes SubtaskChange change) {
        tasks.findByIdInTenant(change.taskId()).ifPresent(Task::recomputeStatus);
    }
}
