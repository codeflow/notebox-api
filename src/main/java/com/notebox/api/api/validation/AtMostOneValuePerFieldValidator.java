package com.notebox.api.api.validation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationValueInput;

/** Rejects a record request holding two values for the same field id. */
public class AtMostOneValuePerFieldValidator
        implements ConstraintValidator<AtMostOneValuePerField, AnnotationRecordInput> {

    @Override
    public boolean isValid(AnnotationRecordInput input, ConstraintValidatorContext context) {
        if (input == null || input.values() == null) {
            return true;
        }
        Set<UUID> seen = new HashSet<>();
        for (AnnotationValueInput value : input.values()) {
            if (value != null && !seen.add(value.fieldId())) {
                return false;
            }
        }
        return true;
    }
}
