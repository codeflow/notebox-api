package com.notebox.api.domain.event;

import java.util.UUID;

/**
 * A domain fact about a task's subtask set (AD-10): fired by the application layer after mutating
 * the aggregate, observed synchronously in the same transaction to recompute the parent's derived
 * state — status (BR-06) and dates (BR-07). Events carry only the task id — observers re-fetch
 * through the tenant-scoped choke point (AD-03).
 */
public sealed interface SubtaskChange
        permits SubtaskAdded, SubtaskCompleted, SubtaskUncompleted, SubtaskRemoved, SubtaskRescheduled {

    UUID taskId();
}
