package com.notebox.api.domain;

/**
 * The closed set of seven annotation-type field kinds (BR-04). No kind exists outside this
 * enumeration; each carries the design-time rules that govern how a field of that kind is declared.
 */
public enum FieldType {
    TEXT,
    LIST,
    NUMBER,
    FREE_TEXT,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    IMAGE;

    /**
     * Default value of a field's "visible for viewing" flag when the declaration omits it (D2):
     * {@code true} for TEXT, LIST and NUMBER; {@code false} for the rest.
     *
     * @return the per-field-type default visibility
     */
    public boolean defaultVisibleForViewing() {
        return this == TEXT || this == LIST || this == NUMBER;
    }

    /**
     * Whether a field of this kind carries a predefined option list (FR-03): the three choice kinds.
     *
     * @return {@code true} for LIST, SINGLE_CHOICE and MULTIPLE_CHOICE
     */
    public boolean allowsOptions() {
        return this == LIST || this == SINGLE_CHOICE || this == MULTIPLE_CHOICE;
    }

    /**
     * Whether an option of this kind may carry a badge colour: only List renders options as badges.
     *
     * @return {@code true} only for LIST
     */
    public boolean allowsBadgeColour() {
        return this == LIST;
    }

    /**
     * Whether a field of this kind may be marked "Secret" (human decision 2026-07-23): free-form
     * text only. The flag is declared here; value encryption at rest is US-2.1 (OQ-15).
     *
     * @return {@code true} for TEXT and FREE_TEXT
     */
    public boolean allowsSecret() {
        return this == TEXT || this == FREE_TEXT;
    }
}
