package com.notebox.api.api.dto;

import java.util.UUID;

/**
 * A group node in the navigation tree (FR-09). A null {@code groupId} is the synthetic
 * <em>Ungrouped</em> node, and its {@code name} is null with it: "Ungrouped" is a user-facing
 * system string, which belongs to the web's own catalog, not to this payload (AD-05, AD-06).
 *
 * <p>{@code count} is the number of items the node stands for — which is exactly the {@code total}
 * of the listing that clicking it opens (OQ-27), so the two are cross-checkable rather than a
 * number nobody can verify.
 */
public record NavigationGroupNodeDto(UUID groupId, String name, long count) {

    /**
     * The synthetic node collecting items that carry no group.
     *
     * @param count how many ungrouped items the node stands for
     * @return the Ungrouped node
     */
    public static NavigationGroupNodeDto ungrouped(long count) {
        return new NavigationGroupNodeDto(null, null, count);
    }
}
