package com.notebox.api.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Every feature-003 message key must have its own value in each locale file (C-09, BR-08). */
class AnnotationTypeMessageCoverageTest {

    private static final List<String> KEYS = List.of(
            "validation.failed",
            "annotation.type.name.required",
            "annotation.type.name.taken",
            "annotation.type.not_found",
            "annotation.field.name.required",
            "annotation.field.option.label.required",
            "annotation.field.type.unknown",
            "annotation.field.options.not_allowed",
            "annotation.field.option.colour.invalid",
            "annotation.field.number.bounds.invalid",
            "annotation.field.secret.not_allowed",
            "annotation.image.too_large",
            "annotation.image.type.unsupported",
            "annotation.image.not_found");

    @Test
    void everyKey_hasItsOwnValueInEnglishAndPortuguese() throws Exception {
        Properties en = load("/messages.properties");
        Properties pt = load("/messages_pt.properties");
        for (String key : KEYS) {
            assertTrue(en.containsKey(key), "missing English message for " + key);
            assertTrue(pt.containsKey(key), "missing Portuguese message for " + key);
        }
    }

    private Properties load(String resource) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
