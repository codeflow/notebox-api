package com.notebox.api.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.DateRangeValid;

/**
 * Create/update payload for a subtask (FR-11): required name, each date independently optional
 * (start ≤ end when both present), and the writable done flag — PUT-replace semantics, so an
 * absent done means false.
 */
@DateRangeValid
public record SubtaskInput(
        @NotBlank(message = "task.subtask.name.required")
                @Size(max = 120, message = "task.subtask.name.too_long")
                String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean done) {

    /** The done flag with replace semantics: absent (null) means false. */
    public boolean doneOrFalse() {
        return Boolean.TRUE.equals(done);
    }
}
