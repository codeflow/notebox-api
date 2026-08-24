# Plan — Item groups and navigation-tree data

**ID:** features/014-groups-navigation-notebox-api
**User Story:** US-3.1
**Version:** v1
**Status:** Approved (human approval 2026-08-22)
**Date:** 2026-08-22
**Spec:** [spec.md](spec.md) — approved 2026-08-22, 26 scenarios

## Origin
- **Spec:** `features/014-groups-navigation-notebox-api/spec.md` (approved 2026-08-22).
- **User Story:** US-3.1. **FRs:** FR-08 (group CRUD + at-most-one assignment), FR-09 (navigation-tree data).
- **Decisions this plan implements:** OQ-04 (single-membership, flat, per-domain namespaces),
  **OQ-23** (tree composes type → group; tree stops at group nodes), **OQ-24** (deleting a group
  un-groups its members), **OQ-25** (siblings by name asc, `Ungrouped` last).
- **Architecture bound by:** AD-01 (layering), AD-02 (repositories own `EntityManager`), AD-03
  (tenant choke point), AD-05 (message catalog), AD-06 (JSON only; the UI is the satellite), AD-07
  (Bean Validation at the edge), AD-09 (`@Transactional` on application methods), AD-11 (error
  mapping in providers), AD-12 (explicit CDI scopes).

## Approach

**One `Group` entity, discriminated by domain.** A new tenant-owned aggregate `Group` carries
`name` + `domain ∈ {ANNOTATION, TASK}` and nothing else. OQ-04's "separate namespaces" is expressed
as a discriminator column plus the unique key `(tenant_id, domain, name)` — one table, one CRUD
surface, one repository, and per-domain uniqueness falls out of the constraint rather than out of
service code. `GroupDomain` is a domain enum in the `Priority`/`FieldType`/`BadgeColour` mould, and
`@ValidGroupDomain` mirrors `@ValidPriority` at the edge (AD-07).

**The table is `item_group`, not `group`.** `GROUP` is a reserved word in MySQL; an unquoted `group`
table breaks every statement that touches it. The entity is `Group`, the table is `item_group`.

**The group reference lives on the item, one nullable column each.** `annotation_record.group_id`
and `task.group_id`, both `NULL`-able, both FK → `item_group(id)` **`ON DELETE SET NULL`**. That FK
action *is* the OQ-24 rule: deleting a group is a single `DELETE`, and the storage engine un-groups
every member atomically — no unbounded load of members into the persistence context to null them one
at a time. Cross-domain assignment (a task pointed at an `ANNOTATION` group) is the one rule an FK
cannot express, so the service checks the fetched group's `domain` before assigning and rejects with
a localized key. Assignment rides the **existing** replace-update contract: `groupId` becomes an
ordinary optional component of `TaskInput` / `AnnotationRecordInput`, and omitting it clears the
group exactly as omitting `card` clears the card (feat-012 precedent).

**The tree is assembled in the application from four small tenant-scoped queries**, never from a
join across aggregates (AD-02 keeps each query in its own repository):

1. `AnnotationTypeRepository` — the tenant's types (`id`, `name`). *Structural: always present.*
2. `AnnotationRecordRepository` — `select distinct annotationTypeId, groupId` for the tenant. A
   `NULL` `groupId` in the result set is precisely "this type has ungrouped records" → its
   `Ungrouped` node.
3. `TaskRepository` — `select distinct groupId` for the tenant. Same `NULL` rule.
4. `GroupRepository` — the tenant's groups, to resolve the ids in (2) and (3) to names.

`NavigationService` then joins these four bounded result sets in memory and applies OQ-25: name
ascending, case-insensitive, id tiebreak, with the `Ungrouped` node appended last. The cost is
bounded by *types × groups*, never by record count — which is exactly why OQ-23 stopped the tree at
group nodes.

**`Ungrouped` is a null id, not a string.** The API returns a node with `groupId: null` and
`name: null`; `notebox-web` renders its own localized label. Emitting the literal word "Ungrouped"
would put a user-facing system string in a JSON payload, which AD-05 forbids and AD-06 assigns to
the satellite.

**Tree nodes resolve through the listings that already exist.** `GET /annotation-records` and
`GET /tasks` each gain one optional `group` query parameter accepting a **UUID** or the literal
**`none`** (ungrouped); absent means unfiltered. Sort order, page defaults, row shape and count
semantics are untouched (OQ-20, OQ-21, NFR-08).

## Alternatives rejected

- **A table per domain (`annotation_group` + `task_group`).** Rejected: it doubles the entity, the
  repository, the DTOs, the validation and the endpoint set to express a distinction one
  discriminator column already makes, and it would force the navigation tree to query two tables
  where it now queries one. The unique key `(tenant_id, domain, name)` delivers OQ-04's separate
  namespaces at zero structural cost.
- **A `group_item` join table.** Rejected: a join table models many-to-many, which is the exact
  shape OQ-04 ruled out. Encoding "at most one" as a nullable FK makes the invariant structural —
  a second membership is unrepresentable rather than merely forbidden.
- **Un-grouping members in application code on delete** (load every member, null its `groupId`,
  flush). Rejected: an unbounded read-modify-write to delete a *label*, and it makes deletion cost
  scale with group size. `ON DELETE SET NULL` is atomic and constant-cost in the service. Accepted
  trade: it does not bump members' `updated_at` — documented under Risk.
- **Assembling the whole tree in one SQL statement.** Rejected: it needs a `UNION` across two
  aggregates with different shapes and would put a cross-aggregate query in one repository,
  violating AD-02's "each repository owns its entity's queries". Four indexed, tenant-scoped
  queries are more legible, individually testable, and no slower at the sizes involved.
- **Two endpoints, one per root** (`/navigation/annotations`, `/navigation/tasks`). Rejected: C28
  renders a single sidebar; two round trips to paint one tree is a worse contract for feat-015.
- **Enumerating records/tasks as tree leaves.** Rejected at spec time (OQ-23) — unbounded payload,
  collides with NFR-08, duplicates the listing contract.

## Reversibility

**One-way — these are what the effort went into:**
- **`item_group` table shape and the `(tenant_id, domain, name)` unique key.** Data plus contract;
  changing the uniqueness scope later means a migration over live rows.
- **`group_id` FK columns with `ON DELETE SET NULL`** on `annotation_record` and `task`. The FK
  action *is* the OQ-24 behaviour; changing it later changes what deletion means.
- **The navigation-tree JSON shape.** feat-015 codes against it, and the `groupId: null` convention
  for `Ungrouped` is load-bearing for the web's rendering.
- **The `group` query-parameter convention (`<uuid> | none`).** Public contract on two shipped
  listing endpoints.

**Reversible — decided, not deliberated:**
four-query assembly vs. any other decomposition; `application/group` + `application/navigation`
package placement; the audit `detail` string; index selection (addable later without a contract
change); whether `NavigationService` caches (it does not — NFR-03 is out of scope).

## Blast radius

**New files (11):**
`domain/Group.java`, `domain/GroupDomain.java`, `api/validation/ValidGroupDomain.java` +
`GroupDomainValidator.java`, `api/dto/GroupDto.java`, `api/dto/GroupInput.java`,
`api/dto/NavigationTreeDto.java` (+ `NavigationTypeNodeDto`, `NavigationGroupNodeDto`),
`api/GroupResource.java`, `api/NavigationResource.java`, `application/group/GroupService.java`,
`application/navigation/NavigationService.java`,
`infrastructure/persistence/GroupRepository.java`,
`src/main/resources/db/migration/V7__groups_navigation.sql`.

**Existing files changed (13):**

| File | Change |
|---|---|
| `domain/Task.java` | `groupId` field + accessors |
| `domain/AnnotationRecord.java` | `groupId` field + accessors |
| `api/dto/TaskInput.java` | `+ UUID groupId` (replace semantics) |
| `api/dto/TaskDto.java`, `TaskListItemDto.java` | `+ groupId` on both shapes |
| `api/dto/AnnotationRecordInput.java`, `AnnotationRecordDto.java` | `+ groupId` |
| `api/TaskResource.java` | `@QueryParam("group")` on `list` |
| `api/AnnotationRecordResource.java` | `@QueryParam("group")` on `list` |
| `application/task/TaskService.java` | resolve + domain-check + assign group |
| `application/annotation/AnnotationRecordService.java` | same |
| `infrastructure/persistence/TaskRepository.java` | filtered list + count, `distinct groupId` |
| `infrastructure/persistence/AnnotationRecordRepository.java` | filtered list + count, `distinct (typeId, groupId)` |
| `messages.properties` + `messages_pt.properties` | **10 new keys each** |

**Consumers:** feat-015 (the Navigator, not yet started) and — the one that matters — **the
already-shipped `notebox-web` task and record forms**, see Risk 1.

**Not touched:** BR-06 status derivation, BR-07 date derivation, OQ-20/OQ-21 sort orders, the
feat-007 rich-text dialect, FR-18/AD-14 secret handling, the image store, Redis (NFR-03).

## Risk

1. **PUT-replace × legacy web silently un-groups items — the feat-012 hazard, repeated.** Once this
   ships, any client that PUTs a task or record **without** `groupId` clears the group, because the
   contract is replace-not-patch. Between this feature merging and feat-015 shipping, the live web
   forms do exactly that. *Signal:* an item's group vanishes after an unrelated edit. *Mitigation:*
   promote the pair together, as US-4.2 did; recorded in the catalog when this merges.
2. **`GROUP` is reserved in MySQL.** *Signal:* V7 fails at migration, or Hibernate emits unquoted
   `group` and every query errors. *Mitigation:* the table is `item_group`; the entity name `Group`
   never reaches SQL.
3. **`ON DELETE SET NULL` does not bump members' `updated_at`.** A client caching on `updated_at`
   can show a stale group after someone deletes it. *Signal:* stale group on a cached row.
   *Accepted* — the alternative is an unbounded write per delete; the tree read is authoritative.
4. **The tree's `DISTINCT` scales with record count if unindexed.** *Signal:* `/navigation` latency
   grows with tenant size. *Mitigation:* `ix_record_tenant_type_group (tenant_id,
   annotation_type_id, group_id)` and `ix_task_tenant_group (tenant_id, group_id)` make both
   projections index-only scans.
5. **Cross-domain assignment is a service check, not a constraint.** A code path that assigns
   without going through `GroupService` would bypass it. *Signal:* the wrong-domain scenario
   failing. *Mitigation:* both services resolve the group through the same helper; the scenario is
   a test on each of the two surfaces.
6. **Hibernate staleness after the FK cascade.** A `Task` loaded in the same persistence context
   before a group delete keeps its stale `groupId`. *Signal:* none in practice — delete is its own
   `@Transactional` use case. Noted so the auditor does not re-derive it.

## Test plan (26 scenarios → 5 classes)

| Class | Covers | Scenarios |
|---|---|---|
| `GroupResourceTest` | lifecycle, per-domain uniqueness, blank name, immutable domain, rename, 404/401 | 9 |
| `GroupDeletionTest` | un-grouping on both sides, survival of members, audit entry | 3 |
| `GroupAssignmentTest` | assign, replace, clear-by-omission, wrong domain, foreign tenant | 5 |
| `NavigationTreeTest` | shape, ordering + `Ungrouped` last, structural-vs-projection, empty tenant, cross-tenant | 5 |
| `GroupFilteredListingTest` | filter by group, `none`, tasks by group, order/page defaults intact | 4 |

Cross-tenant coverage is asserted per new endpoint (NFR-01), and the pt-locale assertion rides
`GroupResourceTest` (C-09).
