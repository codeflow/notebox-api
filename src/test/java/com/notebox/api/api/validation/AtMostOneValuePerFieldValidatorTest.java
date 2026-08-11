package com.notebox.api.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationValueInput;

import org.junit.jupiter.api.Test;

/** Two values for the same field id must fail edge validation, never reach the service (audit F1). */
class AtMostOneValuePerFieldValidatorTest {

    private final AtMostOneValuePerFieldValidator validator = new AtMostOneValuePerFieldValidator();

    private AnnotationValueInput text(UUID fieldId, String text) {
        return new AnnotationValueInput(fieldId, text, null, null, null);
    }

    @Test
    void rejectsTwoValuesForTheSameField() {
        UUID field = UUID.randomUUID();
        AnnotationRecordInput input = new AnnotationRecordInput(
                UUID.randomUUID(), "dup", List.of(text(field, "a"), text(field, "b")));

        assertFalse(validator.isValid(input, null));
    }

    @Test
    void acceptsDistinctFieldsAndMissingValues() {
        AnnotationRecordInput distinct = new AnnotationRecordInput(
                UUID.randomUUID(), "ok",
                List.of(text(UUID.randomUUID(), "a"), text(UUID.randomUUID(), "b")));
        AnnotationRecordInput noValues = new AnnotationRecordInput(UUID.randomUUID(), "ok", null);

        assertTrue(validator.isValid(distinct, null));
        assertTrue(validator.isValid(noValues, null));
    }
}
