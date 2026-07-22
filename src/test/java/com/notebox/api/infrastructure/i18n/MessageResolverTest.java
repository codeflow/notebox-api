package com.notebox.api.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Locale;

import jakarta.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MessageResolverTest {

    @Inject
    MessageResolver resolver;

    @Test
    void resolvesEnglish() {
        assertEquals("Authentication is required.", resolver.resolve("AUTH_REQUIRED", Locale.ENGLISH));
    }

    @Test
    void resolvesPortuguese() {
        String pt = resolver.resolve("AUTH_REQUIRED", Locale.forLanguageTag("pt"));
        assertEquals("Autenticação é obrigatória.", pt);
        assertNotEquals(resolver.resolve("AUTH_REQUIRED", Locale.ENGLISH), pt);
    }

    @Test
    void fallsBackToEnglishForUnsupportedLocale() {
        assertEquals("Authentication is required.",
                resolver.resolve("AUTH_REQUIRED", Locale.FRENCH));
    }
}
