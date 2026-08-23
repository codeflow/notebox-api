package com.notebox.api.application.navigation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** Case-insensitive by name, then by id — the OQ-25 order, shared by both node kinds. */
    private static final Comparator<NavigationGroupNodeDto> BY_NAME =
            Comparator.comparing((NavigationGroupNodeDto n) -> n.name().toLowerCase())
                    .thenComparing(n -> n.groupId().toString());

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

        Map<UUID, Set<UUID>> groupsByType = new HashMap<>();
        Set<UUID> typesWithUngrouped = new LinkedHashSet<>();
        for (Object[] pair : records.distinctTypeGroupPairsInTenant()) {
            UUID typeId = (UUID) pair[0];
            UUID groupId = (UUID) pair[1];
            if (groupId == null) {
                typesWithUngrouped.add(typeId);
            } else {
                groupsByType.computeIfAbsent(typeId, k -> new LinkedHashSet<>()).add(groupId);
            }
        }

        List<NavigationTypeNodeDto> annotations = new ArrayList<>();
        for (AnnotationType type : types.listAllInTenantByName()) {
            annotations.add(new NavigationTypeNodeDto(
                    type.getId(),
                    type.getName(),
                    nodes(groupsByType.getOrDefault(type.getId(), Set.of()),
                            typesWithUngrouped.contains(type.getId()),
                            groupNames)));
        }

        List<UUID> taskGroupIds = tasks.distinctGroupIdsInTenant();
        Set<UUID> namedTaskGroups = new LinkedHashSet<>();
        boolean ungroupedTasks = false;
        for (UUID groupId : taskGroupIds) {
            if (groupId == null) {
                ungroupedTasks = true;
            } else {
                namedTaskGroups.add(groupId);
            }
        }

        return new NavigationTreeDto(annotations, nodes(namedTaskGroups, ungroupedTasks, groupNames));
    }

    /**
     * Turns an occupied-group id set into ordered nodes, appending Ungrouped last when asked.
     *
     * <p>A group id with no name in the map cannot occur — membership and the group live in the
     * same tenant — but it is skipped rather than emitted nameless, since a nameless node would be
     * indistinguishable from the Ungrouped node on the wire.
     */
    private static List<NavigationGroupNodeDto> nodes(
            Set<UUID> groupIds, boolean withUngrouped, Map<UUID, String> groupNames) {
        List<NavigationGroupNodeDto> nodes = new ArrayList<>();
        for (UUID groupId : groupIds) {
            String name = groupNames.get(groupId);
            if (name != null) {
                nodes.add(new NavigationGroupNodeDto(groupId, name));
            }
        }
        nodes.sort(BY_NAME);
        if (withUngrouped) {
            nodes.add(NavigationGroupNodeDto.ungrouped());
        }
        return nodes;
    }
}
