package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.notebox.api.domain.Group;

/**
 * Wire shape of a group (FR-08). The domain is exposed because a client must know which namespace a
 * group belongs to before offering it for assignment; it is fixed for the group's lifetime (OQ-04).
 */
public record GroupDto(
        UUID id,
        String name,
        String domain,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps the aggregate to its wire shape.
     *
     * @param group the group
     * @return the representation
     */
    public static GroupDto from(Group group) {
        return new GroupDto(
                group.getId(),
                group.getName(),
                group.getDomain().name(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
