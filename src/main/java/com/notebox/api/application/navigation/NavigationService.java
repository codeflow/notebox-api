package com.notebox.api.application.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.api.dto.NavigationGroupNodeDto;
import com.notebox.api.api.dto.NavigationTreeDto;
import com.notebox.api.api.dto.NavigationTypeNodeDto;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.Group;
import com.notebox.api.infrastructure.persistence.AnnotationRecordRepository;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.GroupRepository;
import com.notebox.api.infrastructure.persistence.TaskRepository;

/**
 * Assembles the navigation tree (FR-09, OQ-23) from four bounded, tenant-scoped queries — the
 * tenant's types, which groups each type's records occupy, which groups its tasks occupy, and the
 * group names to resolve those ids. Nothing here reads a record or a task row: the tree stops at
 * group nodes, so its cost is types x groups rather than the collection size (NFR-08).
 *
 * <p>Node presence follows one rule: a <b>type</b> node is structural and always present (C28); a
 * <b>group</b> node and the <b>Ungrouped</b> node are projections, present only where a member
 * actually sits. Ordering is name ascending, case-insensitive, id tiebreak, with Ungrouped pinned
 * last regardless of collation (OQ-25).
 */
@ApplicationScoped
public class NavigationService {

    /**
     * The OQ-25 order: Ungrouped last, then case-insensitive by name with an id tiebreak. Null-safe
     * on both components, so the Ungrouped node sorts correctly even if it is ever added to the list
     * before sorting rather than appended after it — its position must not depend on call order.
     */
    private static final Comparator<NavigationGroupNodeDto> BY_NAME =
            Comparator.comparing((NavigationGroupNodeDto n) -> n.groupId() == null)
                    .thenComparing(n -> n.name() == null ? "" : n.name().toLowerCase())
                    .thenComparing(n -> n.groupId() == null ? "" : n.groupId().toString());

    private final AnnotationTypeRepository types;
    private final AnnotationRecordRepository records;
    private final TaskRepository tasks;
    private final GroupRepository groups;

    public NavigationService(
            AnnotationTypeRepository types,
            AnnotationRecordRepository records,
            TaskRepository tasks,
            GroupRepository groups) {
        this.types = types;
        this.records = records;
        this.tasks = tasks;
        this.groups = groups;
    }

    /**
     * Builds the whole tree for the caller's tenant.
     *
     * @return the two roots, their nodes ordered per OQ-25
     */
    public NavigationTreeDto tree() {
        Map<UUID, String> groupNames = new HashMap<>();
        for (Group group : groups.allInTenant()) {
            groupNames.put(group.getId(), group.getName());
        }

        Map<UUID, Map<UUID, Long>> groupsByType = new HashMap<>();
        Map<UUID, Long> ungroupedByType = new HashMap<>();
        for (Object[] row : records.typeGroupCountsInTenant()) {
            UUID typeId = (UUID) row[0];
            UUID groupId = (UUID) row[1];
            long count = (Long) row[2];
            if (groupId == null) {
                ungroupedByType.put(typeId, count);
            } else {
                groupsByType.computeIfAbsent(typeId, k -> new LinkedHashMap<>()).put(groupId, count);
            }
        }

        List<NavigationTypeNodeDto> annotations = new ArrayList<>();
        for (AnnotationType type : types.listAllInTenantByName()) {
            annotations.add(new NavigationTypeNodeDto(
                    type.getId(),
                    type.getName(),
                    nodes(groupsByType.getOrDefault(type.getId(), Map.of()),
                            ungroupedByType.get(type.getId()),
                            groupNames)));
        }

        Map<UUID, Long> taskGroups = new LinkedHashMap<>();
        Long ungroupedTasks = null;
        for (Object[] row : tasks.groupCountsInTenant()) {
            UUID groupId = (UUID) row[0];
            long count = (Long) row[1];
            if (groupId == null) {
                ungroupedTasks = count;
            } else {
                taskGroups.put(groupId, count);
            }
        }

        return new NavigationTreeDto(annotations, nodes(taskGroups, ungroupedTasks, groupNames));
    }

    /**
     * Turns an occupied-group map into ordered nodes, including Ungrouped when it has members.
     * Ungrouped is added <em>before</em> the sort and lands last because {@link #BY_NAME} puts it
     * there — its position is the comparator's guarantee, not a side effect of appending it after.
     *
     * <p>A group id with no name in the map cannot occur — membership and the group live in the
     * same tenant — but it is skipped rather than emitted nameless, since a nameless node would be
     * indistinguishable from the Ungrouped node on the wire.
     *
     * @param countsByGroup occupied group ids mapped to the number of items each holds
     * @param ungroupedCount how many ungrouped items exist, or null when there are none
     * @param groupNames tenant-wide id to name resolution
     * @return the ordered nodes, Ungrouped last
     */
    private static List<NavigationGroupNodeDto> nodes(
            Map<UUID, Long> countsByGroup, Long ungroupedCount, Map<UUID, String> groupNames) {
        List<NavigationGroupNodeDto> nodes = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : countsByGroup.entrySet()) {
            String name = groupNames.get(entry.getKey());
            if (name != null) {
                nodes.add(new NavigationGroupNodeDto(entry.getKey(), name, entry.getValue()));
            }
        }
        if (ungroupedCount != null) {
            nodes.add(NavigationGroupNodeDto.ungrouped(ungroupedCount));
        }
        nodes.sort(BY_NAME);
        return nodes;
    }
}
