package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for the per-field-type design-time rules (BR-04, D2, FR-03, Secret decision). */
class FieldTypeTest {

    @Test
    void values_theClosedSet_hasExactlySeven() {
        assertEquals(7, FieldType.values().length);
    }

    @Test
    void defaultVisibleForViewing_textListNumber_isTrue() {
        Set<FieldType> visibleByDefault = EnumSet.of(FieldType.TEXT, FieldType.LIST, FieldType.NUMBER);
        for (FieldType type : FieldType.values()) {
            assertEquals(
                    visibleByDefault.contains(type),
                    type.defaultVisibleForViewing(),
                    "default visibility of " + type);
        }
    }

    @Test
    void allowsOptions_choiceKinds_isTrue() {
        Set<FieldType> withOptions =
                EnumSet.of(FieldType.LIST, FieldType.SINGLE_CHOICE, FieldType.MULTIPLE_CHOICE);
        for (FieldType type : FieldType.values()) {
            assertEquals(withOptions.contains(type), type.allowsOptions(), "options on " + type);
        }
    }

    @Test
    void allowsBadgeColour_listOnly_isTrue() {
        for (FieldType type : FieldType.values()) {
            assertEquals(type == FieldType.LIST, type.allowsBadgeColour(), "badge colour on " + type);
        }
    }

    @Test
    void allowsSecret_textAndFreeText_isTrue() {
        Set<FieldType> secretable = EnumSet.of(FieldType.TEXT, FieldType.FREE_TEXT);
        for (FieldType type : FieldType.values()) {
            assertEquals(secretable.contains(type), type.allowsSecret(), "secret on " + type);
        }
    }
}
