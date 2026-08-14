package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.notebox.api.domain.Task;

/**
 * Listing-row shape of a task (FR-10, NFR-08): scalars only — no subtasks, so the page stays a
 * single-table query. A dedicated shape avoids the null-vs-empty ambiguity of reusing
 * {@link TaskDto}.
 */
public record TaskListItemDto(
        UUID id,
        String name,
        String priority,
        int status,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps the aggregate to its listing-row shape.
     *
     * @param task the aggregate root
     * @return the row representation
     */
    public static TaskListItemDto from(Task task) {
        return new TaskListItemDto(
                task.getId(),
                task.getName(),
                task.getPriority().name(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
