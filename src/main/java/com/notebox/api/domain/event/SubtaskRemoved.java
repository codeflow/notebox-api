package com.notebox.api.domain.event;

import java.util.UUID;

/** A subtask was deleted from the task (FR-11, BR-05) — the parent's status must be recomputed (BR-06). */
public record SubtaskRemoved(UUID taskId) implements SubtaskChange {
}
