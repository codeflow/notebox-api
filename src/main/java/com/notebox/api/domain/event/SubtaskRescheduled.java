package com.notebox.api.domain.event;

import java.util.UUID;

/**
 * A subtask's start or end date changed (FR-11) — the parent's derived dates must be recomputed
 * (BR-07, FR-12). Fired only when the stored (start, end) pair actually changes.
 */
public record SubtaskRescheduled(UUID taskId) implements SubtaskChange {
}
