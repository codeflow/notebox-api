package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.notebox.api.application.group.GroupAggregates;
import com.notebox.api.domain.Group;

/**
 * Wire shape of a group (FR-08). The domain is exposed because a client must know which namespace a
 * group belongs to before offering it for assignment; it is fixed for the group's lifetime (OQ-04).
 *
 * <p>The aggregate fields (OQ-27) are domain-shaped: an annotation group reports {@code typesUsed}
 * and never {@code averageStatus}; a task group the reverse. <b>{@code averageStatus} null means the
 * group holds no tasks</b> — distinct from 0, which means it holds tasks and they are all at 0%. A
 * consumer that renders null as 0% reports an empty group as a stalled one.
 */
public record GroupDto(
        UUID id,
        String name,
        String domain,
        long itemCount,
        Long typesUsed,
        Integer averageStatus,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Maps the aggregate to its wire shape, with the aggregates the listing computed for its page.
     *
     * @param group the group
     * @param aggregates that group's aggregates
     * @return the representation
     */
    public static GroupDto from(Group group, GroupAggregates aggregates) {
        return new GroupDto(
                group.getId(),
                group.getName(),
                group.getDomain().name(),
                aggregates.itemCount(),
                aggregates.typesUsed(),
                aggregates.averageStatus(),
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
