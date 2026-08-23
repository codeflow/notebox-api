# Tasks — Item groups and navigation-tree data

**ID:** features/014-groups-navigation-notebox-api · **US:** US-3.1 · **Date:** 2026-08-22
**Source:** plan.md + contracts/groups-navigation.md + data-model.md (approved 2026-08-22)
**Status:** Approved (human approval 2026-08-22)

> Strictly sequential — each task builds the layer the next consumes (AD-01 inward dependencies),
> and T-05/T-06 both edit `TaskRepository` + `AnnotationRecordRepository` while T-03/T-04 both edit
> the message catalogs, so no `parallel: yes` anywhere → no worktree isolation. Every task ships its
> tests; `mvn -B verify` must be green at the end of each. Ordered by dependency, then risk: the
> schema goes first because it carries the two things most likely to invalidate the plan — the
> `GROUP` reserved-word trap and whether `ON DELETE SET NULL` really is the OQ-24 rule — and both
> are cheapest to discover before any service is written.

- [x] **T-01 · Schema + domain — V7 migration, `Group` entity, `GroupDomain`, membership columns** ✔ 2026-08-22, verify green (285 tests, 274 → 285; `a4dfce0`). V7 ↔ entity proven by the existing `@QuarkusTest` suite running Flyway + `hibernate-orm validate`; `item_group` naming and the OQ-24 FK action both pinned by `GroupSchemaTest`.
      - files: `db/migration/V7__groups_navigation.sql` (new), `domain/Group.java` (new), `domain/GroupDomain.java` (new), `domain/AnnotationRecord.java` (+ `groupId`), `domain/Task.java` (+ `groupId`), `test …/domain/GroupTest.java` (new), `test …/infrastructure/persistence/GroupSchemaTest.java` (new, `@QuarkusTest`)
      - covers: data-model I-1…I-7 at schema level, **I-10** (the FK action) · scenarios: "A non-empty group is deleted and its members become ungrouped" and "The same rule holds on the task side" — **storage half**: insert a group plus members, delete the group row, assert every member survives with `group_id IS NULL`. The service-level half (audit, 204) is T-02.
      - depends: — · parallel: no
      - verify: `mvn -B verify` — the existing `@QuarkusTest` suite runs Flyway V7 + `hibernate-orm validate`, so a `V7 ↔ entity` divergence surfaces here rather than four tasks later. This is also where the `item_group` naming is proven: an unquoted `group` table would fail every statement that touches it.

- [ ] **T-02 · Group persistence + service — CRUD, per-domain uniqueness, immutable domain, audited delete**
      - files: `infrastructure/persistence/GroupRepository.java` (new), `application/group/GroupService.java` (new), `test …/application/group/GroupServiceTest.java` (new)
      - covers: FR-08 lifecycle, I-3/I-4/I-5, **I-11** (C-10 audit), BR-05 · scenarios: "The same name is free in the other domain but taken in its own", "Name uniqueness is scoped to the tenant", "A group's domain is fixed at creation", "Renaming keeps the members attached", "The deletion is audited (C-10)", plus the service half of the two un-grouping scenarios
      - depends: T-01 · parallel: no
      - verify: `mvn -B verify` — `@TestTransaction` asserts the `AuditLog` row (`GROUP_DELETED` / `GROUP` / `members=<n>`) and that the member count is taken *before* the delete

- [ ] **T-03 · Group endpoints + edge contract — DTOs, `@ValidGroupDomain`, 10 message keys, `GroupResource`**
      - files: `api/dto/GroupDto.java` (new), `api/dto/GroupInput.java` (new), `api/validation/ValidGroupDomain.java` + `GroupDomainValidator.java` (new pair, `@ValidPriority` mould), `api/GroupResource.java` (new), `messages.properties` + `messages_pt.properties` (+10 line-parallel keys), `test …/api/GroupResourceTest.java` (new), `test …/infrastructure/i18n/GroupMessageCoverageTest.java` (new)
      - covers: FR-08 at the wire, AD-07, **C-02**, **C-09**, NFR-06, NFR-08 · scenarios: "A member creates a group in each domain", "A group cannot be nested" (the contract exposes no parent — assert the field is rejected/absent, not merely unused), "A blank name is rejected", "New validation messages resolve in the caller's locale (C-09)", "A foreign tenant's group stays indistinguishable from a missing one", and the `/groups` half of "Every new endpoint refuses an unauthenticated caller"
      - depends: T-02 · parallel: no
      - verify: `mvn -B verify` — the full validation/error matrix from the contract (10 rows) is asserted at the wire; `GroupMessageCoverageTest` pins en/pt parity for all 10 keys

- [ ] **T-04 · Item assignment — `groupId` on both item surfaces, domain check, replace-clearing**
      - files: `application/group/GroupService.java` (+ `resolveForAssignment`), `application/task/TaskService.java`, `application/annotation/AnnotationRecordService.java`, `api/dto/TaskInput.java`, `api/dto/TaskDto.java`, `api/dto/TaskListItemDto.java`, `api/dto/AnnotationRecordInput.java`, `api/dto/AnnotationRecordDto.java`, `test …/application/task/TaskServiceTest.java` (extend), `test …/api/AnnotationRecordResourceTest.java` (extend)
      - covers: FR-08 assignment, **I-8** (domain check — the one rule an FK cannot express), **I-9**, C-01 · scenarios: "Assigning a record to an annotation group", "A second assignment replaces the first — never accumulates", "Omitting the group clears it (established replace semantics)", "A task cannot be put in an annotation group", "A foreign tenant's group is indistinguishable from a missing one"
      - depends: T-03 · parallel: no
      - verify: `mvn -B verify` — ⚠️ this is where the **PUT-replace hazard** (plan Risk 1) becomes real: the clearing test is the one that documents it. Existing `TaskInput`/`AnnotationRecordInput` construction sites must be widened (records expose only the canonical constructor — the feat-012 T-03 experience).

- [ ] **T-05 · Group-filtered listings — `GroupFilter`, repository queries, one query param per listing**
      - files: `application/group/GroupFilter.java` (new), `infrastructure/persistence/TaskRepository.java` (filtered list + count), `infrastructure/persistence/AnnotationRecordRepository.java` (same), `api/TaskResource.java` (+ `@QueryParam("group")`), `api/AnnotationRecordResource.java` (same), `test …/api/TaskResourceTest.java` (extend), `test …/api/AnnotationRecordResourceTest.java` (extend)
      - covers: FR-09 → C30 resolution, NFR-08, OQ-20/OQ-21 left intact · scenarios: "Filtering a type's records by a group node", "Filtering to ungrouped items explicitly", "Filtering tasks by a group node"
      - depends: T-04 · parallel: no
      - verify: `mvn -B verify` — assert the three documented edges: `total` reflects the *filtered* set; an unknown/foreign group id yields an **empty page, not 404**; a malformed value yields `group.filter.invalid`. Sort order and page defaults must be asserted unchanged.

- [ ] **T-06 · Navigation tree — distinct projections, `NavigationService`, `GET /navigation`**
      - files: `infrastructure/persistence/AnnotationRecordRepository.java` (+ `distinctTypeGroupPairsInTenant`), `infrastructure/persistence/TaskRepository.java` (+ `distinctGroupIdsInTenant`), `infrastructure/persistence/GroupRepository.java` (+ `allInTenant`), `application/navigation/NavigationService.java` (new), `api/dto/NavigationTreeDto.java` + `NavigationTypeNodeDto.java` + `NavigationGroupNodeDto.java` (new), `api/NavigationResource.java` (new), `test …/application/navigation/NavigationServiceTest.java` (new), `test …/api/NavigationResourceTest.java` (new)
      - covers: **FR-09**, OQ-23 (composition + tree stops at group nodes), OQ-25 (ordering), C-01, C-02, NFR-06 · scenarios: "The tree carries the two roots, types under Annotations, groups under each type", "Sibling ordering is case-insensitive with Ungrouped pinned last", "A type node is structural, a group node is a projection", "A tenant with nothing yet gets both roots and no children", "The tree never crosses tenants (NFR-01)", and the `/navigation` half of "Every new endpoint refuses an unauthenticated caller"
      - depends: T-05 · parallel: no
      - verify: `mvn -B verify` — the ordering test uses `alpha`/`Beta`/`gamma` so a case-*sensitive* comparator fails it; the structural-vs-projection test asserts an unused group appears in `GET /groups` but **not** in the tree; assert no record or task id appears anywhere in the payload (OQ-23), and that `Ungrouped` is `groupId: null` with **no** label string (AD-05/AD-06).

## Coverage check

All **26** spec scenarios are claimed by exactly one task as their primary home:

| Spec `Feature:` block | Scenarios | Task |
|---|---|---|
| FR-08 Group lifecycle | 7 | T-01 (schema), T-02 (service), T-03 (wire) |
| FR-08 Deleting a group | 3 | T-01 (storage half), T-02 (service + audit) |
| FR-08 At most one group | 5 | T-04 |
| FR-09 Navigation tree | 5 | T-06 |
| FR-09 Node → listing | 3 | T-05 |
| Standing guarantees | 3 | T-03 (`/groups`), T-06 (`/navigation`) |

**No scenario is left uncovered**, and no task cites zero scenarios.

Two checks belong to the plan rather than to a spec scenario, and are called out so the audit does
not read them as invention: the **`item_group` naming** (T-01 — mandated by the MySQL reserved-word
list) and the **two new indexes** (T-01 — plan Risk 4, a performance obligation with no behavioural
scenario).
