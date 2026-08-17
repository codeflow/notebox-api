package com.notebox.api.api.validation;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valid iff the value parses as an absolute URI whose scheme is {@code http} or {@code https}
 * (case-insensitive); null passes (BV composition idiom). Anything unparseable, relative, or on
 * another scheme is rejected.
 */
public class AbsoluteHttpUrlValidator implements ConstraintValidator<AbsoluteHttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute()) {
                return false;
            }
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
