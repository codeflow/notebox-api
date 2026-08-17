package com.notebox.api.api.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.DateRangeValid;

/**
 * Create/update payload for a subtask (FR-11): required name, each date independently optional
 * (start ≤ end when both present), the writable done flag and an optional inline card (FR-13) —
 * PUT-replace semantics, so an absent done means false and an absent card clears it.
 */
@DateRangeValid
public record SubtaskInput(
        @NotBlank(message = "task.subtask.name.required")
                @Size(max = 120, message = "task.subtask.name.too_long")
                String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean done,
        @Valid CardInput card) {

    /** The done flag with replace semantics: absent (null) means false. */
    public boolean doneOrFalse() {
        return Boolean.TRUE.equals(done);
    }
}
