package com.notebox.api.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.notebox.api.domain.Subtask;

/**
 * Wire shape of a subtask (FR-11) — name, optional date pair, done flag, optional inline card
 * (FR-13) and timestamps.
 */
public record SubtaskDto(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean done,
        CardDto card,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps the entity to its wire shape.
     *
     * @param subtask the entity
     * @return the wire representation
     */
    public static SubtaskDto from(Subtask subtask) {
        return new SubtaskDto(
                subtask.getId(),
                subtask.getName(),
                subtask.getStartDate(),
                subtask.getEndDate(),
                subtask.isDone(),
                CardDto.from(subtask.getCard()),
                subtask.getCreatedAt(),
                subtask.getUpdatedAt());
    }
}
