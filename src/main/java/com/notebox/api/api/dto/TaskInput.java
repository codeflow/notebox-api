package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.ValidPriority;

/**
 * Create/update payload for a task (FR-10): required name and priority (enum name, LOW..CRITICAL).
 * The status component is a poison field — status is derived (BR-06) and any supplied value is
 * rejected with task.status.not_writable, never silently ignored.
 */
public record TaskInput(
        @NotBlank(message = "task.name.required")
                @Size(max = 120, message = "task.name.too_long")
                String name,
        @NotNull(message = "task.priority.required")
                @ValidPriority
                String priority,
        @Null(message = "task.status.not_writable") Integer status) {
}
