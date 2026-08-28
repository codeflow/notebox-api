package com.notebox.api.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.notebox.api.domain.Task;

/**
 * Listing-row shape of a task (FR-10, NFR-08): scalars only — the derived status and dates plus the
 * two short card scalars, but no subtasks and no details (unbounded text), so the page stays a
 * single-table query. {@code subtaskCount} is the one exception, and it comes from a grouped
 * count for the whole page rather than from the aggregate — the design's list shows how many
 * subtasks a task has, and loading them to find out would defeat the shape. A dedicated shape
 * avoids the null-vs-empty ambiguity of reusing
 * {@link TaskDto}.
 */
public record TaskListItemDto(
        UUID id,
        String name,
        String priority,
        int status,
        LocalDate startDate,
        LocalDate endDate,
        CardDto card,
        UUID groupId,
        long subtaskCount,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps the aggregate to its listing-row shape.
     *
     * @param task the aggregate root
     * @return the row representation
     */
    public static TaskListItemDto from(Task task, long subtaskCount) {
        return new TaskListItemDto(
                task.getId(),
                task.getName(),
                task.getPriority().name(),
                task.getStatus(),
                task.getStartDate(),
                task.getEndDate(),
                CardDto.from(task.getCard()),
                task.getGroupId(),
                subtaskCount,
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
