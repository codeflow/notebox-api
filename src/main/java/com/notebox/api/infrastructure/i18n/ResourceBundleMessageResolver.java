package com.notebox.api.infrastructure.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resolves messages from the {@code messages[_xx].properties} bundles. English is the default bundle,
 * so a key missing in the request locale falls back to English (OQ-07). Bundles are read as UTF-8.
 */
@ApplicationScoped
public class ResourceBundleMessageResolver implements MessageResolver {

    private static final String BASE_NAME = "messages";

    @Override
    public String resolve(String key, Locale locale, Object... args) {
        String template = lookup(key, locale);
        if (args == null || args.length == 0) {
            return template;
        }
        return new MessageFormat(template, locale).format(args);
    }

    private String lookup(String key, Locale locale) {
        try {
            return ResourceBundle.getBundle(BASE_NAME, locale).getString(key);
        } catch (MissingResourceException missingInLocale) {
            return ResourceBundle.getBundle(BASE_NAME, Locale.ENGLISH).getString(key);
        }
    }
}
