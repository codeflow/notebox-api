package com.notebox.api.domain.event;

import java.util.UUID;

/**
 * A domain fact about a task's subtask set (AD-10): fired by the application layer after mutating
 * the aggregate, observed synchronously in the same transaction to recompute the parent's derived
 * status (BR-06). Events carry only the task id — observers re-fetch through the tenant-scoped
 * choke point (AD-03). US-4.2's date derivation (BR-07) will observe this same seam.
 */
public sealed interface SubtaskChange
        permits SubtaskAdded, SubtaskCompleted, SubtaskUncompleted, SubtaskRemoved {

    UUID taskId();
}
