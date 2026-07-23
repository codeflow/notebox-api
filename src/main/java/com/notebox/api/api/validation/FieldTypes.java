package com.notebox.api.api.validation;

import com.notebox.api.domain.FieldType;

/** Lenient parsing of a wire field-type string; unknown values become {@code null} for the validators. */
final class FieldTypes {

    private FieldTypes() {
    }

    static FieldType parseOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            return FieldType.valueOf(value);
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }
}
