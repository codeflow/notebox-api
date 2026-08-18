package com.notebox.api.api.validation;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valid iff the value parses as an absolute URI whose scheme is {@code http} or {@code https}
 * (case-insensitive) <em>and</em> which carries an authority (host part) — so scheme-only and
 * opaque forms such as {@code https:foo} or {@code https:///p} are rejected along with anything
 * unparseable, relative, or on another scheme; null passes (BV composition idiom). The authority
 * check reads the raw authority so IDN hosts, which {@link URI#getHost()} reports as null, remain
 * valid.
 */
public class AbsoluteHttpUrlValidator implements ConstraintValidator<AbsoluteHttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute() || uri.getRawAuthority() == null || uri.getRawAuthority().isEmpty()) {
                return false;
            }
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
