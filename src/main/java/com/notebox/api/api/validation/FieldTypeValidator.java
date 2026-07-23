package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Rejects a field-type string that is null or not a known {@link com.notebox.api.domain.FieldType}. */
public class FieldTypeValidator implements ConstraintValidator<ValidFieldType, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return FieldTypes.parseOrNull(value) != null;
    }
}
