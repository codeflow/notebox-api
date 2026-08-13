package com.notebox.api.domain;

/**
 * The closed set of task priorities (FR-10, D7). Absence is a validation error — there is no
 * default; the client must choose.
 */
public enum Priority {

    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
