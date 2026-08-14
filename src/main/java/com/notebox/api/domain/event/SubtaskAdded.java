package com.notebox.api.domain.event;

import java.util.UUID;

/** A subtask was added to the task (FR-11) — the parent's status must be recomputed (BR-06). */
public record SubtaskAdded(UUID taskId) implements SubtaskChange {
}
