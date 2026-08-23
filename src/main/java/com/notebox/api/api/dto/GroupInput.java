package com.notebox.api.api.dto;

/**
 * Create/replace payload for a group (FR-08). On replace the domain must equal the stored one — a
 * group's domain is immutable, and a mismatch is rejected rather than ignored (OQ-04).
 *
 * <p>Edge validation annotations are added with the resource in T-03; the service already treats
 * both components as required.
 */
public record GroupInput(String name, String domain) {
}
