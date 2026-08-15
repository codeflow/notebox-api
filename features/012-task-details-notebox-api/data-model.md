# Data model — Task dates, card link and rich-text details

**ID:** features/012-task-details-notebox-api · **Companion of:** `plan.md` · **Date:** 2026-08-15

## Entity deltas

### `Task` (`domain/Task.java`, table `task`) — 5 new nullable columns

| field | type | mapping | writer |
|---|---|---|---|
| `startDate` | `LocalDate` | `@Column(name="start_date")` | `recomputeDates()` only (BR-07) |
| `endDate` | `LocalDate` | `@Column(name="end_date")` | `recomputeDates()` only (BR-07) |
| `card` | `Card` | `@Embedded` → `card_code`, `card_url` | service mapping (replace semantics) |
| `details` | `String` | `@Column(name="details", columnDefinition="TEXT")` | service, **post-sanitization only** |

New domain method — the only date writer, mirroring `recomputeStatus()`:

```java
void recomputeDates()   // startDate = min over subtasks' non-null startDate, else null
                        // endDate   = max over subtasks' non-null endDate,   else null
```

### `Subtask` (`domain/Subtask.java`, table `subtask`) — card only

| field | type | mapping |
|---|---|---|
| `card` | `Card` | `@Embedded` → `card_code`, `card_url` (same names as task — no overrides) |

### `Card` — first `@Embeddable` in the codebase (`domain/Card.java`)

| field | type | mapping |
|---|---|---|
| `code` | `String` | `@Column(name="card_code", length=60)` |
| `url` | `String` | `@Column(name="card_url", length=2048)` |

An all-null embeddable materializes as a **null `Card`** — that *is* the "no card" state. No
partial state can reach the entity: input validation makes a card without a code unconstructable.

### `SubtaskChange` (`domain/event/`) — fifth sealed member

```java
sealed interface SubtaskChange permits SubtaskAdded, SubtaskCompleted,
                                       SubtaskUncompleted, SubtaskRemoved, SubtaskRescheduled
record SubtaskRescheduled(UUID taskId) implements SubtaskChange   // fired on real date change only
```

## Invariants

- **INV-1 (BR-07/FR-12):** after every committed subtask mutation, `task.start_date` ≡
  min(non-null subtask `start_date`) and `task.end_date` ≡ max(non-null subtask `end_date`);
  both NULL when no subtask contributes. Maintained by `TaskDatesRecalculator` observing every
  `SubtaskChange` (IN_PROGRESS phase, same TX). No cross-column constraint: start > end is legal
  (spec's inverted-pair scenario).
- **INV-2 (BR-06, frozen):** `status` derivation is untouched by this feature.
- **INV-3 (OQ-06):** a card is inline state of its owner row — `card_code IS NULL ⇔ no card`;
  `card_url` never present without `card_code` (input-enforced; MySQL CHECK not used, per house).
- **INV-4 (C-08):** `task.details` only ever stores **sanitized** dialect output ≤ 65 535 UTF-8
  bytes; read side re-sanitizes at the DTO boundary, so even a hand-written row cannot reach a
  client hostile.

## Migration — `V6__task_details.sql` (additive; backfill included)

```sql
-- US-4.2 (feat-012): the ALTERs V5's header earmarked. All nullable/additive.
ALTER TABLE task
  ADD COLUMN start_date DATE          NULL,
  ADD COLUMN end_date   DATE          NULL,
  ADD COLUMN card_code  VARCHAR(60)   NULL,
  ADD COLUMN card_url   VARCHAR(2048) NULL,
  ADD COLUMN details    TEXT          NULL;

ALTER TABLE subtask
  ADD COLUMN card_code  VARCHAR(60)   NULL,
  ADD COLUMN card_url   VARCHAR(2048) NULL;

-- Backfill derived dates from already-stored subtask dates (FR-12 over pre-V6 rows).
-- Correlated MIN/MAX over an empty/dateless set yields NULL — all-dateless tasks stay dateless.
UPDATE task t
   SET t.start_date = (SELECT MIN(s.start_date) FROM subtask s WHERE s.task_id = t.id),
       t.end_date   = (SELECT MAX(s.end_date)   FROM subtask s WHERE s.task_id = t.id);
```

No new index: dates and card are never a query predicate in this feature (listing stays
`ix_task_tenant_created`), and `details` is TEXT (off-page, never filtered).

## Storage notes

- `TEXT` (64 KB) matches feat-005's Free-text precedent (`annotation_value.text_value TEXT`);
  the service-level UTF-8 byte guard (`task.details.too_long`) fires **after** sanitization and
  **before** persistence, so MySQL strict-mode 1406 is unreachable.
- `VARCHAR(2048)` for `card_url` keeps the value indexable-in-principle and in-row; utf8mb4
  long-value pressure overflows off-page harmlessly (V6 comment notes it).
