# Contract — Item groups & navigation tree (feat-014, US-3.1)

**Consumed by:** feat-015-groups-navigation-notebox-web.
**Bound by:** AD-06 (JSON only), AD-11 (uniform problem shape), NFR-06 (published in this feature),
NFR-08 (pagination), C-01/C-02 (every endpoint authenticated + tenant-scoped).

All endpoints are `@Authenticated`. A foreign tenant's id yields **404**, never 403 — indistinguishable
from a missing id (C-01). Errors use the established `Problem` / `ValidationProblem` shape with a
machine `code` and a catalog-resolved localized `message` (AD-05, AD-11).

## Endpoints — 6 new, 2 extended

| Method | Path | Returns | Notes |
|---|---|---|---|
| `GET` | `/groups?domain=&page=&size=` | `PageDto<GroupDto>` | `domain` **required**; `page` ≥ 0, `size` 1…200 default 50 (NFR-08) |
| `POST` | `/groups` | `201` + `GroupDto` | |
| `GET` | `/groups/{id}` | `GroupDto` | |
| `PUT` | `/groups/{id}` | `GroupDto` | replace; `domain` must equal the stored one |
| `DELETE` | `/groups/{id}` | `204` | un-groups members (OQ-24), audited (C-10) |
| `GET` | `/navigation` | `NavigationTreeDto` | the whole tree for the caller's tenant |
| `GET` | `/annotation-records?typeId=&**group**=&page=&size=` | `PageDto<AnnotationRecordDto>` | **extended** — one new optional param |
| `GET` | `/tasks?**group**=&page=&size=` | `PageDto<TaskListItemDto>` | **extended** — one new optional param |

### The `group` query parameter (both listings)

| Value | Meaning |
|---|---|
| *absent* | no group filter — the shipped behaviour, unchanged |
| `<uuid>` | only items in that group (a foreign/unknown group returns an **empty page**, not 404 — a filter matches nothing rather than leaking existence) |
| `none` | only **ungrouped** items |
| anything else | `400`, `group.filter.invalid` |

Sort order (OQ-20 records / OQ-21 tasks), page defaults, row shape and the `total` count are
**unchanged**; `total` reflects the filtered set.

## DTOs (`api/dto/`, Java records)

```java
/** A tenant-owned group (FR-08). `domain` is fixed at creation. */
public record GroupDto(
        UUID id,
        String name,
        String domain,          // "ANNOTATION" | "TASK"
        Instant createdAt,
        Instant updatedAt) {
    public static GroupDto from(Group group) { … }
}

/**
 * Create/replace payload for a group (FR-08). On PUT the domain must equal the stored one —
 * a group's domain is immutable, and a mismatch is rejected rather than ignored.
 */
public record GroupInput(
        @NotBlank(message = "group.name.required")
                @Size(max = 120, message = "group.name.too_long")
                String name,
        @NotNull(message = "group.domain.required")
                @ValidGroupDomain
                String domain) {
}

/**
 * The Navigator's data (FR-09, OQ-23). Two roots. `annotations` carries one node per annotation
 * type — types are structural, so a type with no records still appears. `tasks` carries the task
 * groups directly. The tree stops at group nodes: no record and no task is ever enumerated.
 */
public record NavigationTreeDto(
        List<NavigationTypeNodeDto> annotations,
        List<NavigationGroupNodeDto> tasks) {
}

/** One annotation type under the Annotations root (C28), with the groups its own records occupy. */
public record NavigationTypeNodeDto(
        UUID typeId,
        String name,
        List<NavigationGroupNodeDto> groups) {
}

/**
 * A group node. `groupId == null` is the synthetic **Ungrouped** node — `name` is null with it,
 * because "Ungrouped" is a user-facing system string and belongs to the web's own catalog
 * (AD-05, AD-06), not to this payload.
 */
public record NavigationGroupNodeDto(
        UUID groupId,
        String name) {
}
```

### Extended shapes — one new component each

```java
TaskInput(          … existing …, @Valid CardInput card, String details, UUID groupId)
TaskDto(            … existing …,                                        UUID groupId)
TaskListItemDto(    … existing …,                                        UUID groupId)
AnnotationRecordInput(… existing …,                                      UUID groupId)
AnnotationRecordDto(  … existing …,                                      UUID groupId)
```

> ⚠️ **Replace semantics apply.** `groupId` is an ordinary optional component: an update that omits
> it **clears** the group, exactly as omitting `card` clears the card (feat-012 precedent). A client
> that PUTs without `groupId` un-groups the item. This is the feat-012 rollout hazard repeating —
> see plan Risk 1.

## Navigation-tree payload — worked example

Tenant has types `Kafka`, `Postgres`, `RabbitMQ`; `RabbitMQ` records occupy groups `alpha`, `gamma`
and include an ungrouped one; `Kafka` has one record in `alpha`; `Postgres` has no records. Tasks:
one in `Q3 Migration`, one ungrouped.

```json
{
  "annotations": [
    { "typeId": "…kafka",    "name": "Kafka",    "groups": [ { "groupId": "…alpha", "name": "alpha" } ] },
    { "typeId": "…postgres", "name": "Postgres", "groups": [] },
    { "typeId": "…rabbit",   "name": "RabbitMQ", "groups": [
        { "groupId": "…alpha", "name": "alpha" },
        { "groupId": "…gamma", "name": "gamma" },
        { "groupId": null,     "name": null    }
    ] }
  ],
  "tasks": [
    { "groupId": "…q3", "name": "Q3 Migration" },
    { "groupId": null,  "name": null }
  ]
}
```

**Ordering (OQ-25).** Sibling nodes — types and groups alike — are ordered by **name ascending,
case-insensitive, ties broken by id**; the `groupId: null` node is **appended last**, after every
named group, regardless of collation.

**Presence rule (OQ-23).** A **type** node is *structural* — always present. A **group** node and
the **Ungrouped** node are *projections* — present only when that type (or, for tasks, the tenant)
actually has a member there. A group nobody has used appears in `GET /groups` but **not** in the tree.

## Validation / error matrix

| Condition | HTTP | `code` / message key |
|---|---|---|
| blank/missing group name | 400 | `group.name.required` |
| name > 120 chars | 400 | `group.name.too_long` |
| duplicate name in the same tenant+domain | 409 | `group.name.duplicate` |
| missing domain | 400 | `group.domain.required` |
| domain not in the closed set | 400 | `group.domain.invalid` |
| PUT states a different domain | 409 | `group.domain.not_modifiable` |
| group id unknown **or** foreign tenant | 404 | `group.not_found` |
| item assigned a group of the other domain | 409 | `group.domain.mismatch` |
| `page`/`size` out of bounds on `/groups` | 400 | `group.list.size.out_of_bounds` |
| `group` query param neither a UUID nor `none` | 400 | `group.filter.invalid` |

## Message keys (10 new — en shown; `messages_pt.properties` line-parallel)

```properties
group.name.required=The group name is required.
group.name.too_long=The group name must be at most 120 characters.
group.name.duplicate=A group with this name already exists in this domain.
group.domain.required=The group domain is required.
group.domain.invalid=The domain must be either ANNOTATION or TASK.
group.domain.not_modifiable=The group domain cannot be changed after creation.
group.not_found=The group was not found.
group.domain.mismatch=The group belongs to a different domain than this item.
group.list.size.out_of_bounds=The page size must be between 1 and 200
group.filter.invalid=The group filter must be a group id or "none".
```

## Application-layer signatures

```java
// application/group/GroupService.java   @ApplicationScoped
Group  create(GroupInput input);                       // @Transactional
Group  get(UUID id);                                   // 404 via group.not_found
List<Group> list(GroupDomain domain, int page, int size);
long   count(GroupDomain domain);
Group  replace(UUID id, GroupInput input);             // @Transactional — domain must match
void   delete(UUID id);                                // @Transactional — FK un-groups; audits

/** Resolves and domain-checks a group for assignment. null id → null (ungrouped). */
UUID   resolveForAssignment(UUID groupId, GroupDomain expected);   // used by both item services

// application/navigation/NavigationService.java   @ApplicationScoped
NavigationTreeDto tree();                              // @Transactional(readOnly by convention)
```

```java
// infrastructure/persistence/GroupRepository.java   extends TenantScopedRepository<Group>
List<Group> listByDomainInTenant(GroupDomain domain, int page, int size);  // name asc, id asc
long        countByDomainInTenant(GroupDomain domain);
List<Group> allInTenant();                       // tree name resolution, both domains
boolean     existsByDomainAndNameInTenant(GroupDomain domain, String name);
void        remove(Group group);

// AnnotationRecordRepository — 3 new
List<AnnotationRecord> listByTypeAndGroupInTenant(UUID typeId, GroupFilter f, int page, int size);
long                   countByTypeAndGroupInTenant(UUID typeId, GroupFilter f);
List<Object[]>         distinctTypeGroupPairsInTenant();   // (annotationTypeId, groupId|null)

// TaskRepository — 3 new
List<Task> listByGroupNewestFirstInTenant(GroupFilter f, int page, int size);
long       countByGroupInTenant(GroupFilter f);
List<UUID> distinctGroupIdsInTenant();                     // null element ⇒ ungrouped tasks exist
```

`GroupFilter` is a small application-layer value: `NONE` (unfiltered), `UNGROUPED`, or `of(uuid)` —
so the three query shapes stay one method instead of three overloads per repository.

## Audit (C-10)

`GroupService.delete` writes one `AuditLog` through `AuditLogRepository.persistInTenant`, in the
`TaskService` mould:

| Field | Value |
|---|---|
| `action` | `GROUP_DELETED` |
| `targetType` | `GROUP` |
| `targetId` | the group id |
| `detail` | `members=<n>` — counted before the delete, mirroring `subtasks=<n>` |

Create and rename are reversible and are **not** audited.

## Documented edges

- **`ON DELETE SET NULL` does not bump members' `updated_at`.** A row un-grouped by a group deletion
  keeps its previous `updated_at`. Deliberate — see plan Risk 3.
- **A filter on an unknown or foreign group returns an empty page, not 404.** A filter that matched
  nothing and a filter on something that does not exist are the same observable outcome, which is
  what C-01 wants.
- **The tree carries no type icon.** The spec limits a node to what routes a click; `notebox-web`
  already has the type listing (feat-003) if it wants icons.
- **The tree is uncached.** NFR-03/Redis is out of scope for this feature; every call reads MySQL.
- **No `Ungrouped` label in the payload.** `groupId: null` only — the web owns the localized string.
