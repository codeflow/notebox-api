package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A selectable option on a choice field (FR-03). {@code badgeColour} is a
 * {@link com.notebox.api.domain.BadgeColour} name (List fields only), validated on the owning field.
 */
public record FieldOptionInput(
        @NotBlank(message = "annotation.field.option.label.required") String label,
        String badgeColour) {
}
