package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.notebox.api.domain.Task;

/**
 * Full wire shape of a task (FR-10): scalars, the derived status (BR-06, integer percent per
 * OQ-22) and the embedded subtasks — the read-one and mutation-response shape.
 */
public record TaskDto(
        UUID id,
        String name,
        String priority,
        int status,
        Instant createdAt,
        Instant updatedAt,
        List<SubtaskDto> subtasks) {

    /**
     * Maps the aggregate to its full wire shape, subtasks included.
     *
     * @param task the aggregate root
     * @return the wire representation
     */
    public static TaskDto from(Task task) {
        return new TaskDto(
                task.getId(),
                task.getName(),
                task.getPriority().name(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getSubtasks().stream().map(SubtaskDto::from).toList());
    }
}
