package com.notebox.api.api.dto;

import java.util.UUID;

/**
 * A group node in the navigation tree (FR-09). A null {@code groupId} is the synthetic
 * <em>Ungrouped</em> node, and its {@code name} is null with it: "Ungrouped" is a user-facing
 * system string, which belongs to the web's own catalog, not to this payload (AD-05, AD-06).
 */
public record NavigationGroupNodeDto(UUID groupId, String name) {

    /** The synthetic node collecting items that carry no group. */
    public static NavigationGroupNodeDto ungrouped() {
        return new NavigationGroupNodeDto(null, null);
    }
}
