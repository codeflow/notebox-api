# Plan — Task dates, card link and rich-text details

**ID:** features/012-task-details-notebox-api
**User Story:** US-4.2
**Version:** v1
**Status:** Approved (human approval 2026-08-15)
**Date:** 2026-08-15

## Origin
- **Spec:** `features/012-task-details-notebox-api/spec.md` (approved 2026-08-15; 25 scenarios; no new OQs — OQ-05/OQ-06 folded in).
- **US / FRs:** US-4.2 — FR-12 (derived dates, min/max by date), FR-13 (inline card VO on task and subtask), FR-14 (sanitized rich-text details).
- **BRs / ADs bound:** BR-07 (dates derived, never assigned — BR-06-style rejection), BR-06 untouched; AD-01/02/03 (layers, EntityManager confinement, tenant choke point), AD-07 (Bean Validation at the edge), AD-09 (declarative transactions), **AD-10 (CDI events — feat-010 inaugurated the `SubtaskChange` seam and its plan names this feature as the second observer)**, AD-11 (mappers), AD-12 (scopes, constructor injection).
- **Companion artifacts:** `data-model.md` (entity deltas + V6 migration with backfill), `contracts/task-details.md` (extended DTOs, validation matrix delta, new event, message keys).

## Approach

**Derived dates (FR-12)** mirror the derived-status design exactly: two **stored nullable `DATE`
columns on `task`**, recomputed synchronously in-transaction by a second AD-10 observer.
`Task.recomputeDates()` is the only writer — `startDate` = min over subtasks' non-null start dates,
`endDate` = max over non-null end dates, null when no subtask contributes (the spec's inverted-pair
case falls out naturally: min and max are independent). A new `TaskDatesRecalculator` observes the
existing `SubtaskChange` events (default `IN_PROGRESS` phase — same TX, same persistence context,
identical to `TaskProgressRecalculator`) and recomputes on **every** change kind: add/remove move
bounds, done-flips recompute harmlessly (idempotent, in-memory). One genuine gap in the seam:
feat-010's `updateSubtask` fires an event **only on a done-flip**, so a date-only reschedule would
recompute nothing. The sealed hierarchy gains a fifth member — **`SubtaskRescheduled`** — fired by
`updateSubtask` when the incoming dates differ from the stored pair. Both poison components
(`@Null LocalDate startDate/endDate` on `TaskInput`, key `task.dates.not_writable`) reject client
writes through the existing `ConstraintViolationMapper`, exactly like `status`. Stored columns keep
the listing a single-table page query — dates ride `TaskListItemDto` for free — and **V6 backfills**
existing tasks from their already-stored subtask dates in the migration itself.

**Card (FR-13)** becomes the codebase's **first `@Embeddable`** — `domain/Card.java` with
`card_code VARCHAR(60)` / `card_url VARCHAR(2048)` columns, embedded as `@Embedded Card card` on
**both** `Task` and `Subtask` (same column names on both tables, so no attribute overrides). An
all-null embeddable materializes as a null `Card` — which *is* the "no card" state, giving
replace-semantics clearing (omitted card → null → both columns null) with no extra code. On the
edge, `TaskInput` and `SubtaskInput` gain an optional `@Valid CardInput card` nested record: code
`@NotBlank` + `@Size(60)`, url `@Size(2048)` + a new **`@AbsoluteHttpUrl`** constraint (null passes;
otherwise must parse as an absolute URI with scheme `http`/`https` — `javascript:`, `data:` and
relative forms are unstorable, per the spec's XSS posture). House pattern: custom constraint +
catalog key, like `@ValidPriority`.

**Details (FR-14)** reuse feat-007's machinery wholesale — the same `RichTextSanitizer` singleton,
so the project keeps **one dialect** with zero drift. Write side follows `AnnotationRecordService`'s
`applyText` ordering: `TaskService.create/update` sanitize the incoming value **then** enforce the
UTF-8 byte bound of the `TEXT` column (65 535 bytes, key `task.details.too_long`) before storing.
Read side follows `AnnotationValueDto`: `TaskDto.from(...)` gains a `RichTextSanitizer` parameter
and sanitizes on the way out — the entity is never mutated, so a legacy hostile row is cleaned at
the boundary. `TaskListItemDto` deliberately does **not** carry details (spec: listing rows stay
scalar); embedded images remain `data-image-id` references resolved by the shipped, tenant-scoped
`GET /images/{id}` — no binary ever enters the payload.

**Seams touched:** `SubtaskChange` (extended with one sealed member — the seam working as AD-10
intended) · `RichTextSanitizer` (reused unchanged) · `ConstraintViolationMapper` / message catalog
(new keys flow through) · `TaskDto` factories (signature gains the sanitizer, mirroring
`AnnotationValueDto.from`). **No endpoint is added or re-pathed;** the wire delta is purely additive
fields on existing DTOs.

**Ordering rule (unchanged from feat-010):** mutate the aggregate → fire the event → return. Both
observers read the already-mutated in-memory collection; dirty columns flush at commit.

## Alternatives rejected

1. **Dates computed on read:** the 50-row listing would need a per-row min/max over subtasks (join
   or subquery), it breaks symmetry with the stored-status decision feat-010 already argued, and it
   would strand the backfill logic in query code forever. Same rejection as feat-010's alternative 1.
2. **Inline `recomputeDates()` calls in `TaskService` instead of the `SubtaskRescheduled` event:**
   functionally identical today, but it splits the derivation triggers across two mechanisms (events
   for status, inline calls for dates), forfeits the AD-10 seam for future observers (AD-13 cache
   invalidation), and contradicts feat-010's stated design intent.
3. **Hibernate's built-in `@URL` constraint:** validates by regexp with any scheme unless pinned to
   exactly one `protocol` — it cannot express "http **or** https, absolute only" in one use, and the
   house pattern (HANDOFF, constitution 03) is custom constraints with dot-namespaced catalog keys.
4. **Flat `cardCode`/`cardUrl` String fields on both entities:** duplicates the pair and its
   invariant ("no code ⇒ no card") in two places and gives the domain no type for a concept OQ-06
   explicitly named a value object. `@Embeddable` is JPA-standard; being the first in the codebase
   is a reason for care, not avoidance.
5. **`MEDIUMTEXT` for details:** feat-005 stores Free-text values as `TEXT` with a service-level
   UTF-8 byte guard — details follow the precedent. 64 KB of sanitized HTML is generous for a task
   note; raising the type later is a nullable, additive ALTER (reversible upward).
6. **Sanitizing on write only:** C-08 mandates input *and* output, and feat-007's read-side defense
   exists precisely for rows that predate or bypass the write path. Not negotiable, not revisited.
7. **A shared `card` table:** not an alternative — OQ-06 decided inline ownership on 2026-07-22;
   listed only to record that the plan honors a closed decision, not reopening it.

## Reversibility

| Decision | Kind |
|---|---|
| Wire contract: new optional fields on `TaskInput`/`SubtaskInput`/DTOs, poison date components, error keys | **one-way** (feat-013 codes against it) |
| V6 schema: 5 nullable columns on `task`, 2 on `subtask`, `TEXT` details, `VARCHAR(60/2048)` card, backfill UPDATE | **one-way-ish** (Flyway append-only; all additive/nullable) |
| Stored date columns + `TaskDatesRecalculator` observer | reversible (wire-invisible mechanism) |
| `SubtaskRescheduled` as a fifth sealed `SubtaskChange` member | reversible (internal seam) |
| `@Embeddable Card` mapping (vs flat fields) | reversible (wire- and schema-invisible) |
| `@AbsoluteHttpUrl` custom constraint | reversible mechanism; **one-way behavior** (http/https-only is the contract) |
| `TaskDto.from(...)` gaining the sanitizer parameter | reversible (internal signature) |
| Details bound = TEXT physical limit with localized guard | reversible upward (MEDIUMTEXT later is additive) |

## Blast radius

- **New (main, 7):** `domain/Card.java` (@Embeddable), `domain/event/SubtaskRescheduled.java`,
  `application/task/TaskDatesRecalculator.java`, `api/dto/CardInput.java`, `api/dto/CardDto.java`,
  `api/validation/AbsoluteHttpUrl.java` + `AbsoluteHttpUrlValidator.java`,
  `db/migration/V6__task_details.sql`.
- **Modified (main, 10):** `domain/Task.java` (dates + card + details fields, `recomputeDates()`),
  `domain/Subtask.java` (card), `domain/event/SubtaskChange.java` (permits list),
  `application/task/TaskService.java` (details sanitize+bound, card mapping, `SubtaskRescheduled`
  fire in `updateSubtask`), `api/TaskResource.java` (injects `RichTextSanitizer`, passes to DTO
  factories), `api/dto/{TaskInput, SubtaskInput, TaskDto, TaskListItemDto, SubtaskDto}.java`
  (new components; `TaskDto.from` signature), `messages.properties` + `messages_pt.properties`
  (+6 line-parallel keys).
- **Tests — extended (4):** `api/TaskResourceTest` (wire scenarios: dates derivation set, card
  matrix, details sanitization fixtures in feat-007 style, replace-clearing, poison dates,
  pt-locale card key), `application/task/TaskServiceTest` (recompute-dates per mutation kind incl.
  reschedule; sanitize-before-bound ordering; clearing), `domain/TaskTest` (`recomputeDates`
  min/max/null/inverted table), `infrastructure/i18n/TaskMessageCoverageTest` (+6 keys).
  `api/OpenApiCoverageTest` unchanged — no new paths.
- **Explicitly untouched:** `TaskRepository`, `TaskProgressRecalculator`, `RichTextSanitizer` +
  `JsoupRichTextSanitizer` (dialect frozen), `ImageResource`/`ImageService`, all envelopes/mappers,
  `PageDto`, every shipped endpoint path and migration. `Task.recomputeStatus()` and its math are
  frozen (BR-06 out of scope).

## Risk

| Risk | Signal that reveals it |
|---|---|
| Date-only subtask update forgets to fire `SubtaskRescheduled` (the one new trigger) | the spec's "changing a subtask's dates moves the derived dates immediately" scenario fails; `TaskServiceTest` asserts recompute per mutation kind |
| V6 ↔ entity divergence (new columns vs `@Column` mappings) | `%test` runs Flyway + `hibernate-orm validate` — every `@QuarkusTest` fails at startup |
| V6 backfill wrong for all-dateless tasks (must stay NULL, not epoch) | migration uses correlated `MIN`/`MAX` subqueries (NULL over empty set); repository test seeds a dateless task and asserts null dates |
| All-null `Card` embeddable not materializing as null (clearing scenario breaks) | the replace-clears-card scenario asserts `card` absent after omitting it |
| `TaskDto.from` signature change misses a call site | compile-time — the build breaks, not runtime |
| Sanitize/bound ordering inverted (bound before sanitize lets a hostile long value bypass the guard differently than feat-005) | `TaskServiceTest` asserts the feat-005 ordering: sanitized value is what the bound measures and what is stored |
| Generated OpenAPI now advertises poison `startDate`/`endDate` on `TaskInput` | accepted, documented edge (exactly the shipped `status` posture) — `contracts/task-details.md` |
| Oversize details (> 65 535 UTF-8 bytes post-sanitization) | rejected with catalog key `task.details.too_long` before persistence — never a MySQL 1406 500 |
| `VARCHAR(2048)` card URL + utf8mb4 row-size pressure | `TEXT`/`VARCHAR` overflow to off-page storage; V6 comment notes it; Flyway apply in `%test` is the signal |
| Inverted derived pair surprising a consumer | contract documents it as legal (spec scenario); feat-013 renders as-is |

## Test plan (25 scenarios → 4 extended classes)

- `domain/TaskTest` — `recomputeDates()` pure table: min/max across subtasks, one-sided dates,
  all-dateless → null/null, inverted pair (start-only 09-10 + end-only 09-01), single subtask.
- `application/task/TaskServiceTest` — dates recompute per mutation kind (add/reschedule/remove;
  done-flip = no date change), `SubtaskRescheduled` fired only on real date change, card mapping and
  replace-clearing (task + subtask), details sanitize→bound ordering and clearing; `@TestTransaction`.
- `api/TaskResourceTest` — REST-assured wire truth for the 25 scenarios: derivation happy/partial/
  empty/inverted, poison dates rejection, card round-trip/code-only/blank-code/bad-scheme/61-char/
  subtask-card/clearing/inline-independence, details round-trip + hostile fixtures (script, on*,
  `javascript:` href, `src`-stripped image, legacy row via direct write) + listing-row exclusions,
  foreign-tenant 404, pt-locale `task.card.code.required`.
- `infrastructure/i18n/TaskMessageCoverageTest` — the 6 new keys present in both catalogs (C-09).
