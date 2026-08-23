package com.notebox.api.application.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.notebox.api.domain.error.GroupFilterInvalidException;

import org.junit.jupiter.api.Test;

/** Parsing the {@code group} query parameter (FR-09 → C30): absent, "none", a uuid, or a rejection. */
class GroupFilterTest {

    @Test
    void absentOrBlankMeansUnfiltered() {
        assertFalse(GroupFilter.parse(null).isRestricted());
        assertFalse(GroupFilter.parse("").isRestricted());
        assertFalse(GroupFilter.parse("   ").isRestricted());
    }

    @Test
    void theNoneTokenMeansUngrouped() {
        GroupFilter filter = GroupFilter.parse("none");

        assertTrue(filter.isRestricted());
        assertNull(filter.groupId(), "ungrouped is a restriction with no group id");
    }

    @Test
    void theNoneTokenIsCaseInsensitive() {
        assertTrue(GroupFilter.parse("NONE").isRestricted());
        assertNull(GroupFilter.parse("None").groupId());
    }

    @Test
    void aUuidRestrictsToThatGroup() {
        UUID id = UUID.randomUUID();

        GroupFilter filter = GroupFilter.parse(id.toString());

        assertTrue(filter.isRestricted());
        assertEquals(id, filter.groupId());
    }

    @Test
    void anythingElseIsRejected() {
        assertThrows(GroupFilterInvalidException.class, () -> GroupFilter.parse("not-a-uuid"));
        assertThrows(GroupFilterInvalidException.class, () -> GroupFilter.parse("null"));
        assertThrows(GroupFilterInvalidException.class, () -> GroupFilter.parse("ungrouped"));
    }
}
