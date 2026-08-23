package com.notebox.api.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** The group aggregate's shape (FR-08, OQ-04): a name, a fixed namespace, and no way to nest. */
class GroupTest {

    @Test
    void carriesItsNameAndDomain() {
        UUID tenantId = UUID.randomUUID();

        Group group = new Group(tenantId, "Brokers", GroupDomain.ANNOTATION);

        assertEquals(tenantId, group.getTenantId());
        assertEquals("Brokers", group.getName());
        assertEquals(GroupDomain.ANNOTATION, group.getDomain());
    }

    @Test
    void renamingLeavesTheDomainUntouched() {
        Group group = new Group(UUID.randomUUID(), "Brokers", GroupDomain.ANNOTATION);

        group.setName("Message Brokers");

        assertEquals("Message Brokers", group.getName());
        assertEquals(GroupDomain.ANNOTATION, group.getDomain(),
                "the domain is fixed at creation — no setter exists for it");
    }

    @Test
    void exposesNoParent_soNestingIsUnrepresentable() {
        boolean hasParentAccessor = Arrays.stream(Group.class.getMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("parent"));

        assertTrue(!hasParentAccessor, "OQ-04 fixed groups as flat — a parent accessor would reopen it");
    }

    @Test
    void theDomainSetIsClosedToTwoNamespaces() {
        assertEquals(2, GroupDomain.values().length);
        assertEquals(GroupDomain.ANNOTATION, GroupDomain.valueOf("ANNOTATION"));
        assertEquals(GroupDomain.TASK, GroupDomain.valueOf("TASK"));
    }

    @Test
    void timestampsAreAssignedOnPersistNotConstruction() {
        Group group = new Group(UUID.randomUUID(), "Brokers", GroupDomain.TASK);

        assertNull(group.getCreatedAt());
        assertNull(group.getUpdatedAt());
    }
}
