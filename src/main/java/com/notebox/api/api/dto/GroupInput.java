package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.ValidGroupDomain;

/**
 * Create/replace payload for a group (FR-08). On replace the domain must equal the stored one — a
 * group's domain is immutable, and a mismatch is rejected rather than ignored (OQ-04). The record
 * exposes no parent component, so nesting is unrepresentable at the wire (OQ-04).
 */
public record GroupInput(
        @NotBlank(message = "group.name.required")
                @Size(max = 120, message = "group.name.too_long")
                String name,
        @NotNull(message = "group.domain.required")
                @ValidGroupDomain
                String domain) {
}
