package com.notebox.api.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Every feature-005 message key must have its own value in each locale file (C-09, BR-08). */
class AnnotationRecordMessageCoverageTest {

    private static final List<String> KEYS = List.of(
            "annotation.record.name.required",
            "annotation.record.list.type.required",
            "annotation.record.list.size.out_of_bounds",
            "annotation.record.not_found",
            "annotation.record.field.unknown",
            "annotation.record.value.type_mismatch",
            "annotation.record.value.number.out_of_bounds",
            "annotation.record.value.option.unknown",
            "annotation.record.value.image.not_found",
            "annotation.record.secret.reveal.forbidden",
            "annotation.record.reveal.not_secret",
            "annotation.record.value.duplicate_field",
            "annotation.record.value.required",
            "annotation.record.name.too_long",
            "annotation.record.value.too_long",
            "annotation.type.has_records",
            "annotation.type.field.has_records",
            "annotation.type.field.secret_flip.has_values");

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
