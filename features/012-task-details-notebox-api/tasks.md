# Tasks — Task dates, card link and rich-text details

**ID:** features/012-task-details-notebox-api · **US:** US-4.2 · **Date:** 2026-08-15
**Source:** plan.md + contracts/task-details.md + data-model.md (approved 2026-08-15)
**Status:** Approved (human approval 2026-08-15)

> Strictly sequential — each task builds the layer the next one consumes (AD-01 inward
> dependencies), and T-02/T-04 share `TaskService` + `TaskServiceTest` while T-03/T-05 share
> `TaskResourceTest`, so no `parallel: yes` anywhere → no worktree isolation. Every task ships its
> tests; `mvn -B verify` (the harness command) must be green at the end of each task. Ordered by
> dependency, then risk: the schema (V6 ↔ entity validate) and the one new event trigger (date-only
> reschedule) — the two things most likely to invalidate the plan — go first.

- [x] **T-01 · Schema + domain — V6 migration with backfill, `Card` embeddable, `Task.recomputeDates()`** ✔ 2026-08-15, verify green (223 tests; V6 ↔ entity `hibernate validate` proven by the existing `@QuarkusTest`s, first `@Embeddable` mapped)
      - files: `db/migration/V6__task_details.sql` (new), `domain/Card.java` (new, first `@Embeddable`), `domain/Task.java` (`startDate`/`endDate`/`card`/`details` fields + `recomputeDates()`), `domain/Subtask.java` (`card`), `test …/domain/TaskTest.java` (extend)
      - covers: FR-12 math (BR-07), FR-13 shape (OQ-06), data-model INV-1/INV-3 · scenarios: "Start is the earliest start and end the latest end, regardless of insertion order", "A subtask missing one date simply does not participate in that bound", "A task with no dated subtasks has no dates", "Disjoint one-sided subtasks may inverse the derived pair" — as a pure `recomputeDates()` table (min/max, one-sided, all-dateless → null/null, inverted, single subtask)
      - depends: — · parallel: no
      - verify: `mvn -B verify` — the existing `@QuarkusTest` suite runs Flyway V6 + `hibernate-orm validate` on this task's own verify, so V6 ↔ entity divergence surfaces here, not later. Backfill SQL is reviewed for NULL-over-empty-set semantics (correlated MIN/MAX); the runtime invariant it mirrors is pinned in T-02.

- [x] **T-02 · Derived dates at runtime — `SubtaskRescheduled` event + `TaskDatesRecalculator` observer** ✔ 2026-08-15, verify green (231 tests). Note: a test-only `testsupport/SubtaskChangeRecorder` observer was added to assert which facts fire (reschedule = exactly one `SubtaskRescheduled`; name-only = none; done-flip = only the flip fact).
      - files: `domain/event/SubtaskRescheduled.java` (new), `domain/event/SubtaskChange.java` (permits +1), `application/task/TaskDatesRecalculator.java` (new, `@Observes SubtaskChange`, IN_PROGRESS), `application/task/TaskService.java` (`updateSubtask` fires `SubtaskRescheduled` on real date change only), `test …/application/task/TaskServiceTest.java` (extend)
      - covers: FR-12/BR-07 at runtime, AD-10 (second observer of the seam) · scenarios: "Changing a subtask's dates moves the derived dates immediately", "Deleting the boundary subtask recomputes the bound", plus recompute on add; `SubtaskRescheduled` fired only when the stored (start, end) pair changes; done-flip = dates unchanged; dateless-subtask task stays null/null (INV-1 — the backfill's runtime twin)
      - depends: T-01 · parallel: no
      - verify: `mvn -B verify` (`@TestTransaction` reads the recomputed dates inside the TX — catches an `AFTER_SUCCESS` misphase immediately)

- [x] **T-03 · Input contract at the edge — `CardInput`, `@AbsoluteHttpUrl`, poison dates, message keys** ✔ 2026-08-17, verify green (242 tests). The 21 `TaskInput`/`SubtaskInput` construction sites in `TaskServiceTest` were widened for the new components (records have only the canonical constructor).
      - files: `api/dto/CardInput.java` (new), `api/validation/AbsoluteHttpUrl.java` + `AbsoluteHttpUrlValidator.java` (new pair), `api/dto/TaskInput.java` (+ `@Null startDate/endDate`, `@Valid card`, `details`), `api/dto/SubtaskInput.java` (+ `@Valid card`), `messages.properties` + `messages_pt.properties` (+6 line-parallel keys), `test …/api/TaskResourceTest.java` (extend — validation matrix delta), `test …/infrastructure/i18n/TaskMessageCoverageTest.java` (+6 keys)
      - covers: BR-07 write-rejection, FR-13 bounds (OQ-09 refinements), C-09 · scenarios: "A client cannot write task dates", "A card without a code is rejected", "A card URL that is not absolute http or https is rejected" (`javascript:`, `data:`, relative), "A card code above 60 characters is rejected", "New validation messages resolve in the caller's locale (C-09)"
      - depends: T-02 · parallel: no
      - verify: `mvn -B verify` — the extended inputs are wire-testable through the existing endpoints before the service consumes them (rejections happen at the edge, AD-07); `task.details.too_long` lands in the catalogs here, its behaviour in T-04

- [x] **T-04 · Service accepts card + details — mapping, sanitize → bound → store, replace clearing** ✔ 2026-08-17, verify green (253 tests). Note: the plan's oversize guard implies one file its list omitted — `domain/error/TaskDetailsTooLongException` (INVALID → 400, key `task.details.too_long`), the constitution §Errors mechanism and the `AnnotationRecordValueTooLongException` precedent. The all-null-embeddable-is-null assumption is pinned by the two clearing tests (reload from DB).
      - files: `application/task/TaskService.java` (ctor gains `RichTextSanitizer`; `create`/`update` sanitize details then enforce the 65 535-byte UTF-8 guard; `CardInput → Card` on task and subtask; omitted card/details clear), `test …/application/task/TaskServiceTest.java` (extend)
      - covers: FR-13 (OQ-06 inline copies), FR-14 write side (C-08), INV-3/INV-4 · scenarios: "A task card with code and URL round-trips" / "A card may carry only its code" / "A subtask carries a card under the same contract" (service-level, entity assertions), "An update omitting the card clears it" (also pins the all-null-embeddable-is-null assumption), "Cards are inline copies, never a shared entity", "Hostile markup is stripped on write" (stored value is the sanitized form), "Details are optional and cleared by an update that omits them" · plus the plan-mandated guard (no spec scenario — the spec delegates storage sizing to the plan): oversize post-sanitization details rejected with `task.details.too_long`, and the feat-005 ordering (bound measures the *sanitized* value)
      - depends: T-03 · parallel: no
      - verify: `mvn -B verify`

- [x] **T-05 · Wire read side — output DTOs, read-side sanitization, resource wiring, full wire truth** ✔ 2026-08-17, verify green (268 tests — the `implement` step's `verify_green` gate; feature total +56 over feat-010's 212)
      - files: `api/dto/CardDto.java` (new), `api/dto/TaskDto.java` (`startDate`/`endDate`/`card`/`details`; `from(...)` gains `RichTextSanitizer`), `api/dto/TaskListItemDto.java` (dates + card, **no** details), `api/dto/SubtaskDto.java` (`card`), `api/TaskResource.java` (injects the sanitizer, passes it to the factories), `test …/api/TaskResourceTest.java` (extend — the remaining wire scenarios)
      - covers: FR-12/FR-13/FR-14 at the wire, C-08 read side, C-01/C-02, NFR-06 · scenarios: "Start is the earliest…" (over the wire, insertion order reversed), "Listing rows carry the derived dates", "A task card with code and URL round-trips" / "A card may carry only its code" / "A subtask carries a card under the same contract" (wire), "A dialect-clean details value round-trips unchanged" (byte-identical), "A javascript: link loses its href but keeps its text", "An embedded image is a reference, never a fetched binary" (`src` stripped, `data-image-id` kept), "A legacy hostile row is sanitized on the way out" (direct DB write, read through the API), "Listing rows never carry details", "A foreign tenant's task stays indistinguishable from a missing one"
      - depends: T-04 · parallel: no
      - verify: `mvn -B verify` (full suite green = the `implement` step's `verify_green` gate; `OpenApiCoverageTest` unchanged — no new paths, the schema delta is picked up by the generated document)

## Coverage check

Every one of the spec's 25 scenarios is cited by at least one task; each has exactly one **primary**
owner (T-01: 4 · T-02: 2 · T-03: 5 · T-04: 6 · T-05: 8), and the T-04/T-05 overlap on the three
card round-trip scenarios is deliberate — service-level entity assertions and wire-level shape
assertions of the same behaviour, complementary as in feat-010's T-03/T-04. No task cites zero
scenarios; the one non-scenario behaviour (T-04's oversize-details guard) is plan-mandated and
named as such. No scenario is uncovered.

## Implement close-out (2026-08-17)

All five tasks `[x]`, `mvn -B verify` green at **268 tests** (T-01 223 → T-02 231 → T-03 242 →
T-04 253 → T-05 268). Files beyond the plan's list, each noted on its task: test-only
`testsupport/SubtaskChangeRecorder` (T-02) and `domain/error/TaskDetailsTooLongException` (T-04, the
§Errors mechanism the plan's guard implies). Everything the plan marked "explicitly untouched" stayed
untouched: `TaskRepository`, `TaskProgressRecalculator`, both sanitizer classes, `recomputeStatus()`,
every shipped endpoint path and migration, `OpenApiCoverageTest`.
