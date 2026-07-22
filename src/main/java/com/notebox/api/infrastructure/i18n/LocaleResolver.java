package com.notebox.api.infrastructure.i18n;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Chooses the request locale: the first {@code Accept-Language} that is supported, else English
 * (OQ-07). User-preference resolution applies where an authenticated user context exists; error
 * responses fall back to the header.
 */
@ApplicationScoped
public class LocaleResolver {

    private final Set<String> supported;
    private final Locale defaultLocale;

    public LocaleResolver(
            @ConfigProperty(name = "notebox.i18n.supported-locales") List<String> supported,
            @ConfigProperty(name = "notebox.i18n.default-locale") String defaultLocale) {
        this.supported = Set.copyOf(supported);
        this.defaultLocale = Locale.forLanguageTag(defaultLocale);
    }

    public Locale fromHeaders(HttpHeaders headers) {
        if (headers == null) {
            return defaultLocale;
        }
        for (Locale candidate : headers.getAcceptableLanguages()) {
            if (supported.contains(candidate.getLanguage())) {
                return Locale.forLanguageTag(candidate.getLanguage());
            }
        }
        return defaultLocale;
    }
}
