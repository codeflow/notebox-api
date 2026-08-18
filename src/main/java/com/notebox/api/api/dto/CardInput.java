package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.AbsoluteHttpUrl;

/**
 * The inline card on a task or subtask (FR-13, OQ-06): a required code and an optional absolute
 * http/https link. A card without a code is unconstructable at the edge, so the entity never sees a
 * url without a code.
 */
public record CardInput(
        @NotBlank(message = "task.card.code.required")
                @Size(max = 60, message = "task.card.code.too_long")
                String code,
        @Size(max = 2048, message = "task.card.url.too_long")
                @AbsoluteHttpUrl
                String url) {
}
