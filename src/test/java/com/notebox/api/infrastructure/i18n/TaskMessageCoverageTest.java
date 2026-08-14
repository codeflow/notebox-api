package com.notebox.api.infrastructure.i18n;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/** Every feature-010 message key must have its own value in each locale file (C-09, BR-08). */
class TaskMessageCoverageTest {

    private static final List<String> KEYS = List.of(
            "task.name.required",
            "task.name.too_long",
            "task.priority.required",
            "task.priority.invalid",
            "task.status.not_writable",
            "task.not_found",
            "task.subtask.name.required",
            "task.subtask.name.too_long",
            "task.subtask.date.invalid",
            "task.subtask.not_found",
            "task.list.size.out_of_bounds");

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
