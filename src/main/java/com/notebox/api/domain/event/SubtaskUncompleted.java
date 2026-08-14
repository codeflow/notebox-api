package com.notebox.api.domain.event;

import java.util.UUID;

/** A subtask was marked not done again (FR-11) — the parent's status must be recomputed (BR-06). */
public record SubtaskUncompleted(UUID taskId) implements SubtaskChange {
}
