package com.notebox.api.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Every group message key (feat-014) must have its own value in each locale file (C-09, BR-08). */
class GroupMessageCoverageTest {

    private static final List<String> KEYS = List.of(
            "group.name.required",
            "group.name.too_long",
            "group.name.duplicate",
            "group.domain.required",
            "group.domain.invalid",
            "group.domain.not_modifiable",
            "group.not_found",
            "group.domain.mismatch",
            "group.list.size.out_of_bounds",
            "group.filter.invalid");

    @Test
    void everyKey_hasItsOwnValueInEnglishAndPortuguese() throws Exception {
        Properties en = load("/messages.properties");
        Properties pt = load("/messages_pt.properties");
        for (String key : KEYS) {
            assertTrue(en.containsKey(key), "missing English message for " + key);
            assertTrue(pt.containsKey(key), "missing Portuguese message for " + key);
        }
    }

    /** A copy-pasted catalog is worse than a missing one — it ships English to a pt caller. */
    @Test
    void portugueseIsActuallyTranslated_notAnEnglishCopy() throws Exception {
        Properties en = load("/messages.properties");
        Properties pt = load("/messages_pt.properties");
        for (String key : KEYS) {
            assertTrue(!en.getProperty(key).equals(pt.getProperty(key)),
                    "Portuguese value is identical to English for " + key);
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
