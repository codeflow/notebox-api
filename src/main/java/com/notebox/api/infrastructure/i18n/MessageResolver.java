package com.notebox.api.infrastructure.i18n;

import java.util.Locale;

/** Resolves a message key to the request locale, falling back to English; never returns a raw key (BR-08). */
public interface MessageResolver {

    String resolve(String key, Locale locale, Object... args);
}
