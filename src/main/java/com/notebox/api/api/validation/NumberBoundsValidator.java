package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.TypeFieldInput;

/** Rejects a Number field whose declared minimum is greater than its maximum. */
public class NumberBoundsValidator implements ConstraintValidator<NumberBoundsValid, TypeFieldInput> {

    @Override
    public boolean isValid(TypeFieldInput field, ConstraintValidatorContext context) {
        if (field.numberMin() == null || field.numberMax() == null) {
            return true;
        }
        return field.numberMin().compareTo(field.numberMax()) <= 0;
    }
}
