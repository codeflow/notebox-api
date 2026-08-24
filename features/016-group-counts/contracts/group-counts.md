# Contract — Aggregate counts (feat-016, US-3.1)

**Consumed by:** feat-015-groups-navigation-notebox-web.
**Extends:** `features/014-groups-navigation-notebox-api/contracts/groups-navigation.md`.
**Additive only** — every field feat-014 published keeps its name, type and meaning. A client that
ignores the fields below behaves exactly as it does today.

## Endpoints — no new paths, two extended payloads

| Method | Path | Change |
|---|---|---|
| `GET` | `/navigation` | every group node gains `count` |
| `GET` | `/groups?domain=&page=&size=` | every row gains `itemCount`, plus `typesUsed` (annotation) or `averageStatus` (task) |

Authentication, tenant scoping, ordering, paging and error shapes are unchanged.

## DTO deltas

```java
/**
 * A group node (FR-09). `count` is the number of items the node stands for — which is exactly the
 * `total` of the listing that clicking it opens, so the two are cross-checkable.
 */
public record NavigationGroupNodeDto(
        UUID groupId,
        String name,
        long count) {          // NEW

    /** The synthetic Ungrouped node: null id, null name (the web supplies the label), real count. */
    public static NavigationGroupNodeDto ungrouped(long count) { … }
}

/**
 * Wire shape of a group (FR-08). The aggregate fields are domain-shaped: an annotation group
 * reports `typesUsed` and never `averageStatus`; a task group the reverse. The absent one is null.
 */
public record GroupDto(
        UUID id,
        String name,
        String domain,
        long itemCount,        // NEW — always present
        Long typesUsed,        // NEW — annotation domain only, else null
        Integer averageStatus, // NEW — task domain only, else null
        Instant createdAt,
        Instant updatedAt) { … }
```

## Field semantics

| Field | Meaning | Zero vs null |
|---|---|---|
| `count` | items the node stands for | always a number; `0` cannot occur — a node exists only where a member sits (feat-014 OQ-23 presence rule) |
| `itemCount` | records (annotation) or tasks (task) in the group | `0` for an empty group; never null |
| `typesUsed` | distinct annotation types the group's records span | `0` for an empty annotation group; **`null` on a task group** |
| `averageStatus` | mean derived status, integer percent **HALF_UP** | **`null` = the group has no tasks.** `0` = it has tasks and they are all at 0%. **These are different and must not be collapsed.** |

> ⚠️ **For the consumer:** rendering `averageStatus: null` as `0%` reports an empty group as a
> stalled one. Render it as "no tasks" / blank, never as a zero-length progress bar.

## Worked example

```jsonc
// GET /navigation  — "Infrastructure" holds 12 RabbitMQ records and 6 Runbook records
{
  "annotations": [
    { "typeId": "…rabbit", "name": "RabbitMQ", "groups": [
        { "groupId": "…infra", "name": "Infrastructure", "count": 12 },
        { "groupId": null,     "name": null,             "count": 6  }   // Ungrouped
    ] },
    { "typeId": "…runbook", "name": "Runbook", "groups": [
        { "groupId": "…infra", "name": "Infrastructure", "count": 6 }
    ] }
  ],
  "tasks": [
    { "groupId": "…migration", "name": "Migration", "count": 8 },
    { "groupId": null,         "name": null,        "count": 2 }
  ]
}

// GET /groups?domain=ANNOTATION
{ "items": [
    { "id": "…infra", "name": "Infrastructure", "domain": "ANNOTATION",
      "itemCount": 18, "typesUsed": 2, "averageStatus": null, … },
    { "id": "…scratch", "name": "Scratch", "domain": "ANNOTATION",
      "itemCount": 0, "typesUsed": 0, "averageStatus": null, … }
  ], "page": 0, "size": 50, "total": 2 }

// GET /groups?domain=TASK
{ "items": [
    { "id": "…migration", "name": "Migration", "domain": "TASK",
      "itemCount": 8, "typesUsed": null, "averageStatus": 58, … },
    { "id": "…fresh", "name": "Fresh", "domain": "TASK",
      "itemCount": 2, "typesUsed": null, "averageStatus": 0, … },   // has tasks, all 0%
    { "id": "…empty", "name": "Empty", "domain": "TASK",
      "itemCount": 0, "typesUsed": null, "averageStatus": null, … } // NO tasks
  ], "page": 0, "size": 50, "total": 3 }
```

Note the last two task groups: `0` and `null` are the distinction A-4 exists to preserve.

## Application-layer signatures

```java
// application/group/GroupAggregates.java  — a value, not an entity
public record GroupAggregates(long itemCount, Long typesUsed, Integer averageStatus) {
    /** A group the aggregate query did not return: no members, and therefore no average. */
    public static GroupAggregates empty(GroupDomain domain) { … }
}

// application/group/GroupService.java
Map<UUID, GroupAggregates> aggregatesFor(GroupDomain domain, List<Group> page);

// infrastructure/persistence/AnnotationRecordRepository.java
List<Object[]> typeGroupCountsInTenant();                       // RENAMED (+ count)
List<Object[]> aggregatesByGroupInTenant(Collection<UUID> groupIds);

// infrastructure/persistence/TaskRepository.java
List<Object[]> groupCountsInTenant();                           // RENAMED (+ count)
List<Object[]> aggregatesByGroupInTenant(Collection<UUID> groupIds);
```

Both `aggregatesByGroupInTenant` return **no row** for a group with no members — the caller maps a
missing id to `GroupAggregates.empty(domain)`, which is where the null average comes from.

## Statement budget (NFR-08)

| Read | Before feat-016 | After |
|---|---|---|
| `GET /navigation` | 4 | **4** — `DISTINCT` became `GROUP BY … COUNT` in the same statements |
| `GET /groups` | 2 (page + total) | **3** — one aggregate query per page, bounded by page size; **never per row** |

An empty page issues **no** aggregate query at all (no `IN ()`).

## Documented edges

- **The count is cross-checkable.** A node's `count` and the `total` of the filtered listing it
  opens are the same number by construction, and one scenario asserts it. A count nothing can be
  checked against is a count that goes wrong quietly.
- **`countMembers` is not this path.** `GroupService.delete` keeps its own single-group count for
  the audit detail (feat-014, C-10). Both compute a group's size; they are deliberately separate
  because delete needs one group and this path is page-shaped.
- **No caching.** NFR-03/Redis is out of scope; every read recomputes.
- **No new message key.** This feature emits no user-facing string (C-09 n/a) — the labels beside
  these numbers belong to feat-015's catalogs.
