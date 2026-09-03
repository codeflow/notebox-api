package com.notebox.api.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.DateRangeValid;

/**
 * Create/update payload for a subtask (FR-11): required name, each date independently optional
 * (start ≤ end when both present), the writable done flag and an optional inline card (FR-13) —
 * PUT-replace semantics, so an absent done means false and an absent card clears it.
 *
 * <p>The completion moment is a poison field (FR-20): it is recorded by the server at the transition,
 * so a client that supplies one is rejected rather than silently ignored — the same treatment the task
 * status and the task dates already get, so a client learns it was wrong instead of quietly disagreeing.
 */
@DateRangeValid
public record SubtaskInput(
        @NotBlank(message = "task.subtask.name.required")
                @Size(max = 120, message = "task.subtask.name.too_long")
                String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean done,
        @Null(message = "task.subtask.completed_at.not_writable") Instant completedAt,
        @Valid CardInput card) {

    /** The done flag with replace semantics: absent (null) means false. */
    public boolean doneOrFalse() {
        return Boolean.TRUE.equals(done);
    }
}
