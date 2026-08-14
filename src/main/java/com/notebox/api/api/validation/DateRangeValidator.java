package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.SubtaskInput;

/** Valid when either date is absent or start ≤ end (FR-11). */
public class DateRangeValidator implements ConstraintValidator<DateRangeValid, SubtaskInput> {

    @Override
    public boolean isValid(SubtaskInput input, ConstraintValidatorContext context) {
        if (input == null || input.startDate() == null || input.endDate() == null) {
            return true;
        }
        return !input.startDate().isAfter(input.endDate());
    }
}
