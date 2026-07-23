package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.FieldType;

/** A non-choice field carrying options is rejected; an unknown field type is left to {@link ValidFieldType}. */
public class OptionsAllowedValidator implements ConstraintValidator<OptionsAllowedForFieldType, TypeFieldInput> {

    @Override
    public boolean isValid(TypeFieldInput field, ConstraintValidatorContext context) {
        FieldType type = FieldTypes.parseOrNull(field.fieldType());
        if (type == null) {
            return true;
        }
        return field.optionsOrEmpty().isEmpty() || type.allowsOptions();
    }
}
