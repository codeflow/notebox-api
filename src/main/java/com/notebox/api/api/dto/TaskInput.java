package com.notebox.api.api.dto;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.ValidPriority;

/**
 * Create/update payload for a task (FR-10): required name and priority (enum name, LOW..CRITICAL),
 * an optional inline card (FR-13) and optional rich-text details (FR-14, sanitized server-side).
 * The status and date components are poison fields — status (BR-06) and dates (BR-07) are derived,
 * and any supplied value is rejected with a localized key, never silently ignored. Replace
 * semantics: an omitted card or details clears it.
 */
public record TaskInput(
        @NotBlank(message = "task.name.required")
                @Size(max = 120, message = "task.name.too_long")
                String name,
        @NotNull(message = "task.priority.required")
                @ValidPriority
                String priority,
        @Null(message = "task.status.not_writable") Integer status,
        @Null(message = "task.dates.not_writable") LocalDate startDate,
        @Null(message = "task.dates.not_writable") LocalDate endDate,
        @Valid CardInput card,
        String details) {
}
