package com.notebox.api.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.notebox.api.domain.GroupDomain;

/** Rejects strings outside the closed {@link GroupDomain} set (FR-08); null passes (BV composition idiom). */
public class GroupDomainValidator implements ConstraintValidator<ValidGroupDomain, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            GroupDomain.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
