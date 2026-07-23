package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.api.dto.FieldOptionInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldType;

/** Every declared badge colour must be a palette value, and only List fields may carry badge colours. */
public class BadgeColourValidator implements ConstraintValidator<BadgeColourAllowed, TypeFieldInput> {

    @Override
    public boolean isValid(TypeFieldInput field, ConstraintValidatorContext context) {
        FieldType type = FieldTypes.parseOrNull(field.fieldType());
        for (FieldOptionInput option : field.optionsOrEmpty()) {
            if (option == null || option.badgeColour() == null) {
                continue;
            }
            if (!isPaletteColour(option.badgeColour())) {
                return false;
            }
            if (type != null && !type.allowsBadgeColour()) {
                return false;
            }
        }
        return true;
    }

    private boolean isPaletteColour(String value) {
        try {
            BadgeColour.valueOf(value);
            return true;
        } catch (IllegalArgumentException unknown) {
            return false;
        }
    }
}
