# Plan — Tasks with subtasks and derived progress

**ID:** features/010-tasks-notebox-api
**User Story:** US-4.1
**Version:** v1
**Status:** Approved (human approval 2026-08-13)
**Date:** 2026-08-13

## Origin
- **Spec:** `features/010-tasks-notebox-api/spec.md` (approved 2026-08-13; 21 scenarios; OQ-21/OQ-22 folded in).
- **US / FRs:** US-4.1 — FR-10 (partial: CRUD + priority + derived status), FR-11 (partial: name, dates, done flag).
- **BRs / ADs bound:** BR-05, BR-06; AD-01/02/03 (layers, EntityManager confinement, tenant choke point), AD-07 (Bean Validation at the edge), AD-09 (declarative transactions), **AD-10 (CDI events — this feature inaugurates the mechanism; the AD literally names `SubtaskCompleted`/`SubtaskAdded` → recompute as its example)**, AD-11 (mappers), AD-12 (scopes, constructor injection).
- **Companion artifacts:** `data-model.md` (entities + V5 migration), `contracts/tasks.md` (endpoints, DTOs, validation matrix, message keys, events).

## Approach

Tasks follow the established aggregate-root pattern end to end: a tenant-owned `Task` root with
aggregate-internal `Subtask` children (no `tenant_id` on the child — reached only through the root),
a `TaskRepository` extending the `TenantScopedRepository` choke point (AD-03, zero base-class
changes), a `@Transactional` `TaskService` in `application/task/`, and a `TaskResource` at `/tasks`
behind `@Authenticated`. Bean Validation guards the entire input contract at the edge (AD-07) —
including the spec's status-write rejection, expressed as a `@Null`-constrained `status` component
on `TaskInput` ("poison field") so the violation flows through the existing
`ConstraintViolationMapper` envelope with the catalog key `task.status.not_writable`.

The derived status is a **stored `status` column on `task`, recomputed synchronously in-transaction
via CDI events** — the mechanism AD-10 names for this exact case. Every subtask mutation in
`TaskService` first mutates the aggregate, then fires a domain-fact event (`SubtaskAdded` /
`SubtaskCompleted` / `SubtaskUncompleted` / `SubtaskRemoved` — a sealed `SubtaskChange` hierarchy in
`domain/event/`, each carrying only the `taskId`). `TaskProgressRecalculator` observes
`SubtaskChange` (default `IN_PROGRESS` phase — same transaction, same persistence context),
re-fetches the task through the choke point (a persistence-context hit, no extra SQL) and calls the
domain method `Task.recomputeStatus()`. The BR-06 math itself is a pure static
`Task.percentOf(done, total)` — integer percent, HALF_UP, 0 for an empty task (OQ-22) — unit-testable
with no container. Payoff: the paginated listing stays a single-table page query (no subtask join,
no per-row aggregate), and US-4.2's derived dates (BR-07) plug in as a second observer of the same
events without touching `TaskService`.

Deletion follows the feat-005 audit pattern exactly: task delete removes the root (JPA cascade +
DB `ON DELETE CASCADE` as defence in depth) and writes the `AuditLog` row in the same
`@Transactional` method (`TASK_DELETED`, detail `"subtasks=" + n`); subtask delete removes the child
via `orphanRemoval`, audits (`SUBTASK_DELETED`, detail `"taskId=" + id` — the V4 `fieldId=` style)
and fires `SubtaskRemoved` to recompute. The listing reuses the feat-008 idiom verbatim:
`@QueryParam` Bean Validation (`@DefaultValue("50")`, `@Max(200)` with a catalog key), JPQL
`order by e.createdAt desc, e.id desc` (OQ-21) plus a count query, `PageDto<T>` unchanged.

**Ordering rule the implementation must obey:** mutate the aggregate → fire the event → return. The
observer recomputes from the already-mutated in-memory collection; the dirty `status` flushes at
commit, atomically with the subtask change.

**Seams touched (all existing code unmodified):** `TenantScopedRepository` (extended only) ·
`DomainExceptionMapper` / `ConstraintViolationMapper` (new keys/exceptions flow through) · `PageDto`
(reused) · `AuditLog` + `AuditLogRepository` (reused, incl. the V4 `detail` column) ·
`MessageResolver` / locale stack (keys only). **New seam introduced:** the `SubtaskChange` events —
the AD-10 extension point US-4.2 (derived dates) and any future AD-13 cache invalidation will
observe.

## Alternatives rejected

1. **Status computed on read** (COUNT aggregate / loading children): the 50-row listing would need a
   per-row subtask aggregate, it moots AD-10's own named example, and US-4.2's derived dates would
   compound the read-side aggregation. Its real advantage — zero drift risk — is recovered by the
   synchronous in-TX observer (no staleness window) plus per-mutation scenario tests. BR-06's
   "always derived" is honored semantically: no code path ever *assigns* status from input.
2. **Inline `recomputeStatus()` calls in `TaskService` without events:** functionally identical
   today, but it forfeits the constitution-mandated AD-10 seam and forces US-4.2 to edit every
   mutation method instead of adding an observer.
3. **Global `quarkus.jackson.fail-on-unknown-properties=true`** for the status rejection: blast
   radius over every shipped endpoint (feat-003/005/008 clients lose tolerant-reader behavior) and
   the Jackson error bypasses the localized envelope (C-09). The `@Null` poison field is scoped,
   localized, and standard.
4. **Priority as `TINYINT` ordinal:** ordinal couples data to enum declaration order (a reorder
   corrupts rows silently) and contradicts the house `@Enumerated(STRING)` pattern (`role`,
   `field_type`, `badge_colour`).
5. **`@OrderColumn` position on subtask:** no FR names subtask ordering and OQ-05 decided dates
   derive by date, not position. Deterministic read order comes presentationally from
   `@OrderBy("createdAt asc, id asc")` — no schema position, no reorder semantics implied.
6. **One `TaskDto` doing double duty for listing rows** (null/omitted `subtasks`): a `null` vs `[]`
   ambiguity in one shape; the listing row is scalars only, so a dedicated `TaskListItemDto` is the
   cleaner contract (feat-008's argument against a bespoke row DTO — value-rule duplication — does
   not apply here).
7. **Subtask mutations returning `204`/`SubtaskDto`:** every FR-11 scenario asserts the parent's
   recomputed state; returning the parent `TaskDto` saves the client a round trip. Task `DELETE`
   stays `204`.
8. **`DATETIME(6)` subtask dates:** the spec's literals are calendar dates and FR-12's future
   min/max derivation is date-granular → `DATE` / `LocalDate`.

## Reversibility

| Decision | Kind |
|---|---|
| Wire contract: paths, DTO shapes, subtask ops return parent `TaskDto`, error keys, listing order | **one-way** (feat-011 codes against it) |
| V5 schema (tables, `DATE` dates, `VARCHAR(10)` priority, index shape) | **one-way-ish** (Flyway append-only; US-4.2 additions planned as nullable ALTERs) |
| Status stored column + CDI-event recompute | reversible (wire-invisible mechanism; switching to computed-on-read changes no contract) |
| `@Null Integer status` rejection mechanism | reversible mechanism; **one-way behavior** (rejection is the contract) |
| Sealed `SubtaskChange` hierarchy; events carry ids, not entities | reversible |
| `@OrderBy(createdAt, id)` subtask read order | reversible (presentation only) |
| Name bound 120 (OQ-09 refinement, house baseline) | reversible upward |

## Blast radius

- **New (main, 19 files):** `domain/Task.java`, `domain/Subtask.java`, `domain/Priority.java`,
  `domain/event/SubtaskChange.java` + 4 event records, `domain/error/TaskNotFoundException.java`,
  `domain/error/SubtaskNotFoundException.java`, `application/task/TaskService.java`,
  `application/task/TaskProgressRecalculator.java`, `infrastructure/persistence/TaskRepository.java`,
  `api/TaskResource.java`, `api/dto/{TaskInput, SubtaskInput, TaskDto, TaskListItemDto, SubtaskDto}.java`,
  `api/validation/{ValidPriority, PriorityValidator, DateRangeValid, DateRangeValidator}.java`,
  `db/migration/V5__tasks.sql`.
- **Modified (main, 2):** `messages.properties` / `messages_pt.properties` (+11 line-parallel keys).
- **New (test, 5):** `api/TaskResourceTest`, `application/task/TaskServiceTest`, `domain/TaskTest`,
  `infrastructure/persistence/TaskRepositoryTest`, `infrastructure/i18n/TaskMessageCoverageTest`.
- **Modified (test, 1):** `api/OpenApiCoverageTest` (+ the four `/api/tasks*` path keys).
- **Explicitly untouched:** `TenantScopedRepository`, all mappers/envelopes, `PageDto`,
  `AuditLog`/`AuditLogRepository`, `application.properties`, every shipped endpoint and migration.
  All changes are additive; no existing test breaks.

## Risk

| Risk | Signal that reveals it |
|---|---|
| V5 ↔ entity divergence (`DATE`/`LocalDate`, `BIT(1)`/boolean, `VARCHAR(10)` enum length) | `%test` runs Flyway + `hibernate-orm validate` — every `@QuarkusTest` fails at startup |
| Event fired before the aggregate mutation (observer reads a stale collection) | FR-11 scenario tests assert exact percents (50/33/0) after each mutation kind |
| Observer phase misconfigured (`AFTER_SUCCESS` recomputes outside the TX, losing the write) | same scenario tests + `TaskServiceTest` `@TestTransaction` reads status inside the TX |
| Recompute drift from a future mutation path that forgets to fire | all subtask mutations choke through 3 `TaskService` methods; each is scenario-tested in both directions |
| Accidental task-name uniqueness | spec requires none — V5 carries no `uq_` on task; a duplicate-name create test pins it |
| `createdAt` ties breaking listing order | `TaskRepositoryTest` forces `DATETIME(6)` collisions and asserts the id tiebreak (feat-008 precedent) |
| First CDI event wiring in the codebase (ArC observer resolution of sealed subtypes) | recompute assertions in `TaskServiceTest` fail immediately if the observer never fires |
| Non-numeric `status` / malformed dates fail at Jackson binding → 400 outside the catalog envelope | accepted, documented edge in `contracts/tasks.md` (spec scenarios supply well-formed JSON) |
| Generated OpenAPI advertises `status` on `TaskInput` (poison field) although clients must never send it | documented in `contracts/tasks.md`; no annotation workaround, per the no-OpenAPI-annotations posture |
| `ix_task_tenant_created` composite replaces a bare `ix_task_tenant` | V5 comment states the leftmost-prefix equivalence so the AD-03 audit doesn't flag it |

## Test plan (21 scenarios → 5 classes + 1 modified)

- `domain/TaskTest` — pure `percentOf` table: 0/0→0, 1/3→33, 2/3→67, 1/8→13, 1/2→50, 3/3→100 (BR-06, OQ-22).
- `application/task/TaskServiceTest` — recompute wiring per mutation kind (add/complete/uncomplete/remove), audit rows (`TASK_DELETED`, `SUBTASK_DELETED`), cascade on task delete, status-guard on update; `@TestTransaction` + `@InjectMock TenantContext`.
- `api/TaskResourceTest` — REST-assured end-to-end for all 21 scenarios' wire shapes: CRUD, validation matrix, pagination (default 50 / 201 rejected / newest-first), auth 401, foreign-tenant 404s (two tenants via `TestData`/`TestTokens`), pt-locale message, duplicate-name allowed.
- `infrastructure/persistence/TaskRepositoryTest` — newest-first + id tiebreak, tenant isolation at the repository seam.
- `infrastructure/i18n/TaskMessageCoverageTest` — the 11 keys present in both catalogs (C-09).
- `api/OpenApiCoverageTest` — extended with the four new path keys (NFR-06).
