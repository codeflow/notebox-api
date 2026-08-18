package com.notebox.api.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.notebox.api.application.content.RichTextSanitizer;
import com.notebox.api.domain.Task;

/**
 * Full wire shape of a task (FR-10): scalars, the derived status (BR-06, integer percent per
 * OQ-22), the derived dates (BR-07, FR-12 — null when no subtask contributes), the inline card
 * (FR-13), the rich-text details sanitized on the way out (FR-14, C-08 output half) and the
 * embedded subtasks — the read-one and mutation-response shape.
 */
public record TaskDto(
        UUID id,
        String name,
        String priority,
        int status,
        LocalDate startDate,
        LocalDate endDate,
        CardDto card,
        String details,
        Instant createdAt,
        Instant updatedAt,
        List<SubtaskDto> subtasks) {

    /**
     * Maps the aggregate to its full wire shape, subtasks included. Details are re-sanitized at this
     * boundary so a row that predates or bypassed the write path never reaches a client hostile; the
     * entity itself is never mutated.
     *
     * @param task the aggregate root
     * @param sanitizer the shared rich-text sanitizer (C-08)
     * @return the wire representation
     */
    public static TaskDto from(Task task, RichTextSanitizer sanitizer) {
        return new TaskDto(
                task.getId(),
                task.getName(),
                task.getPriority().name(),
                task.getStatus(),
                task.getStartDate(),
                task.getEndDate(),
                CardDto.from(task.getCard()),
                sanitizer.sanitize(task.getDetails()),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getSubtasks().stream().map(SubtaskDto::from).toList());
    }
}
