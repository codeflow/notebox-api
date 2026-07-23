package com.notebox.api.api.error;

/** One field-level validation failure: the offending field path, its machine code, and a localized message. */
public record Violation(String field, String code, String message) {
}
