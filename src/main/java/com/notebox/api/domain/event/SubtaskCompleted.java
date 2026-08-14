package com.notebox.api.domain.event;

import java.util.UUID;

/** A subtask was marked done (FR-11) — the parent's status must be recomputed (BR-06). */
public record SubtaskCompleted(UUID taskId) implements SubtaskChange {
}
