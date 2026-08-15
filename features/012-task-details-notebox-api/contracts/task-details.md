# Contract — Task details extension (feat-012, US-4.2)

**Consumers:** feat-013-task-details-notebox-web (primary), OpenAPI at `/q/openapi` (NFR-06).
**Extends:** `features/010-tasks-notebox-api/contracts/tasks.md` — every path, envelope, auth and
pagination rule there stands unchanged. This document is the **delta**; shapes below are the full
post-feat-012 forms.

## Endpoints — unchanged set, extended payloads

No path is added, removed or re-pathed. `POST/PUT /tasks*` accept the new optional fields;
every task-returning response carries the new read-side fields.

## DTOs (`api/dto/`, Java records — full updated shapes)

```java
// inputs
record TaskInput(
    @NotBlank(message = "task.name.required")
        @Size(max = 120, message = "task.name.too_long") String name,
    @NotNull(message = "task.priority.required")
        @ValidPriority(message = "task.priority.invalid") String priority,
    @Null(message = "task.status.not_writable") Integer status,          // poison (shipped)
    @Null(message = "task.dates.not_writable") LocalDate startDate,      // poison (new — BR-07)
    @Null(message = "task.dates.not_writable") LocalDate endDate,        // poison (new — BR-07)
    @Valid CardInput card,                                               // optional; null = no card
    String details)                                                      // optional; sanitized server-side

@DateRangeValid(message = "task.subtask.date.invalid")
record SubtaskInput(
    @NotBlank(message = "task.subtask.name.required")
        @Size(max = 120, message = "task.subtask.name.too_long") String name,
    LocalDate startDate, LocalDate endDate,
    Boolean done,                                                        // null → false (replace)
    @Valid CardInput card)                                               // optional; null = no card

record CardInput(
    @NotBlank(message = "task.card.code.required")
        @Size(max = 60, message = "task.card.code.too_long") String code,
    @Size(max = 2048, message = "task.card.url.too_long")
        @AbsoluteHttpUrl(message = "task.card.url.invalid") String url)  // optional within the card

// outputs (static from(...) factories)
record TaskDto(UUID id, String name, String priority, int status,
               LocalDate startDate, LocalDate endDate,                   // derived (FR-12); null = underivable
               CardDto card,                                             // null = no card
               String details,                                           // sanitized on read; null = none
               Instant createdAt, Instant updatedAt, List<SubtaskDto> subtasks)
record TaskListItemDto(UUID id, String name, String priority, int status,
               LocalDate startDate, LocalDate endDate,                   // derived dates on rows (spec)
               CardDto card,                                             // two short scalars — rides rows
               Instant createdAt, Instant updatedAt)                     // NO details on rows (spec)
record SubtaskDto(UUID id, String name, LocalDate startDate, LocalDate endDate, boolean done,
               CardDto card, Instant createdAt, Instant updatedAt)
record CardDto(String code, String url)
```

**Replace semantics (unchanged rule, new fields):** `PUT /tasks/{id}` and
`PUT /tasks/{id}/subtasks/{subtaskId}` state the full intended state — an omitted/null `card`
clears the card, omitted/null `details` clears the details. `TaskDto.from(...)` gains a
`RichTextSanitizer` parameter (read-side C-08, mirrors `AnnotationValueDto.from`).

## Validation / error matrix — delta

| Condition | Mechanism | HTTP | Key |
|---|---|---|---|
| any `startDate`/`endDate` supplied on task create/update | `@Null` poison | 400 | `task.dates.not_writable` |
| card present with blank/missing `code` | `@NotBlank` (cascaded `@Valid`) | 400 | `task.card.code.required` |
| card `code` > 60 chars | `@Size` | 400 | `task.card.code.too_long` |
| card `url` not absolute http/https (`javascript:`, `data:`, relative) | `@AbsoluteHttpUrl` (new; null passes) | 400 | `task.card.url.invalid` |
| card `url` > 2048 chars | `@Size` | 400 | `task.card.url.too_long` |
| `details` > 65 535 UTF-8 bytes **after** sanitization | service guard (feat-005 `applyText` ordering) | 400 | `task.details.too_long` |

New constraint pair in `api/validation/`: `@AbsoluteHttpUrl` + `AbsoluteHttpUrlValidator`
(`ConstraintValidator<AbsoluteHttpUrl, String>` — passes null; valid iff the value parses as an
absolute URI with scheme `http` or `https`).

## Message keys (6 new — en shown; pt line-parallel)

```
task.dates.not_writable=The task dates are derived from its subtasks and cannot be set directly.
task.card.code.required=The card code is required when a card is provided.
task.card.code.too_long=The card code must be at most 60 characters.
task.card.url.invalid=The card URL must be an absolute http or https URL.
task.card.url.too_long=The card URL must be at most 2048 characters.
task.details.too_long=The task details are too large.
```

## Events (AD-10) — one new sealed member

```java
record SubtaskRescheduled(UUID taskId) implements SubtaskChange
```

Fired by `TaskService.updateSubtask` **only when the stored (startDate, endDate) pair actually
changes**. New observer `TaskDatesRecalculator.onSubtaskChange(@Observes SubtaskChange)` —
IN_PROGRESS phase, same TX — re-fetches via the choke point and calls `Task.recomputeDates()`;
it recomputes on every change kind (done-flips are harmless no-ops for dates).
`TaskProgressRecalculator` is untouched.

## Application-layer signature deltas

```java
class TaskService {   // ctor gains RichTextSanitizer
  // create/update: sanitize details → enforce UTF-8 byte bound → store; map CardInput → Card
  // updateSubtask: fires SubtaskRescheduled on real date change (plus existing done-flip events)
}
class TaskDatesRecalculator {  // new; ctor: (TaskRepository)
  void onSubtaskChange(@Observes SubtaskChange change);
}
```

`TaskResource` injects `RichTextSanitizer` and passes it to `TaskDto` factories
(the `AnnotationRecordResource` pattern).

## Rich-text dialect (FR-14 / C-08)

The **feat-007 dialect, verbatim** — same `RichTextSanitizer` singleton, sanitize on input and
output: tags `p, br, strong, em, u, s, span, ol, ul, li, blockquote, a, code, pre, img`;
`span[style]` colour-only; `a[href]` http/https with forced `rel="noopener noreferrer"`;
`pre[data-language]`; `img[data-image-id, alt]` — **no `src`**. Embedded images dereference via the
shipped tenant-scoped `GET /api/images/{id}` (FR-07); no binary travels in task payloads.

## Documented edges

- **OpenAPI shows the poison fields** (`status`, now `startDate`/`endDate`) on `TaskInput` — they
  exist only to reject writes; clients must never send them (shipped `status` posture).
- **Inverted derived pair is legal:** disjoint one-sided subtask dates can yield
  `startDate > endDate` on the task; reported as computed, never an error (spec scenario). feat-013
  renders as-is.
- **Malformed `card`/`details` JSON types** (e.g. `card` as a string) fail at Jackson binding with a
  non-catalog 400 — same class of edge as feat-010's malformed dates.
- **`details` bound is byte-denominated** (UTF-8, 65 535) — multibyte content hits it below 65 535
  *characters*; the catalog message deliberately avoids naming a character count.
- **Backfill:** V6 derives dates for pre-existing tasks in the migration itself; a task whose
  subtasks are all dateless stays `null/null`.
