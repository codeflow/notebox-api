package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.FieldType;

/** Rejects {@code secret = true} on any field that is not Text or Free text. */
public class SecretAllowedValidator implements ConstraintValidator<SecretAllowedForFieldType, TypeFieldInput> {

    @Override
    public boolean isValid(TypeFieldInput field, ConstraintValidatorContext context) {
        if (!Boolean.TRUE.equals(field.secret())) {
            return true;
        }
        FieldType type = FieldTypes.parseOrNull(field.fieldType());
        return type == null || type.allowsSecret();
    }
}
