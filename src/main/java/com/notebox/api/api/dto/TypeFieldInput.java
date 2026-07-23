package com.notebox.api.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.notebox.api.api.validation.BadgeColourAllowed;
import com.notebox.api.api.validation.NumberBoundsValid;
import com.notebox.api.api.validation.OptionsAllowedForFieldType;
import com.notebox.api.api.validation.SecretAllowedForFieldType;
import com.notebox.api.api.validation.ValidFieldType;

/**
 * A field declaration in a create/replace request (FR-02, FR-03). {@code fieldType} is a
 * {@link com.notebox.api.domain.FieldType} name; the class-level constraints enforce the cross-field
 * rules (options/colour/secret only where allowed, Number bounds ordered). {@code visibleForViewing}
 * and {@code secret} are nullable — a null takes the per-field-type default at the service.
 */
@OptionsAllowedForFieldType
@BadgeColourAllowed
@NumberBoundsValid
@SecretAllowedForFieldType
public record TypeFieldInput(
        @NotBlank(message = "annotation.field.name.required") String name,
        @ValidFieldType String fieldType,
        UUID iconImageId,
        Boolean visibleForViewing,
        Boolean secret,
        BigDecimal numberMin,
        BigDecimal numberMax,
        @Valid List<FieldOptionInput> options) {

    /** Never-null option view for callers and validators. */
    public List<FieldOptionInput> optionsOrEmpty() {
        return options == null ? List.of() : options;
    }
}
