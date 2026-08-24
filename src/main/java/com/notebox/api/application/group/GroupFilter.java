package com.notebox.api.application.group;

import java.util.UUID;

import com.notebox.api.domain.error.GroupFilterInvalidException;

/**
 * The optional group filter on an item listing (FR-09 → C30): unfiltered, restricted to one group,
 * or restricted to the ungrouped. Keeping the three shapes in one value keeps each repository to a
 * single list/count method instead of three overloads.
 *
 * <p>A filter on an unknown or foreign group is <em>not</em> an error: it simply matches nothing,
 * which is also what keeps it from disclosing whether that group exists (C-01).
 */
public final class GroupFilter {

    private static final GroupFilter NONE = new GroupFilter(false, null);
    private static final GroupFilter UNGROUPED = new GroupFilter(true, null);

    /** The literal a client sends to ask for items with no group. */
    private static final String UNGROUPED_TOKEN = "none";

    private final boolean restricted;
    private final UUID groupId;

    private GroupFilter(boolean restricted, UUID groupId) {
        this.restricted = restricted;
        this.groupId = groupId;
    }

    /** No filter — the listing behaves exactly as it did before feat-014. */
    public static GroupFilter none() {
        return NONE;
    }

    /** Only items carrying no group. */
    public static GroupFilter ungrouped() {
        return UNGROUPED;
    }

    /** Only items in one group. */
    public static GroupFilter of(UUID groupId) {
        return new GroupFilter(true, groupId);
    }

    /**
     * Parses the wire value of the {@code group} query parameter.
     *
     * @param raw absent/blank for unfiltered, {@code "none"} for ungrouped, otherwise a group id
     * @return the parsed filter
     * @throws GroupFilterInvalidException when the value is neither {@code "none"} nor a valid uuid
     */
    public static GroupFilter parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return none();
        }
        if (UNGROUPED_TOKEN.equalsIgnoreCase(raw)) {
            return ungrouped();
        }
        try {
            return of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new GroupFilterInvalidException();
        }
    }

    /** Whether the listing is restricted at all — false means every item qualifies. */
    public boolean isRestricted() {
        return restricted;
    }

    /** The group to restrict to, or null when restricting to the ungrouped. */
    public UUID groupId() {
        return groupId;
    }
}
