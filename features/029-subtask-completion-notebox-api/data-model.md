# Data model — A subtask records when it was completed

**Feature:** features/029-subtask-completion-notebox-api · **Date:** 2026-09-03

## Entity change

`Subtask` — one field added. No new entity, no new relationship, no repository.

| Field | Java type | Column | Null? | Written by |
|---|---|---|---|---|
| `completedAt` | `java.time.Instant` | `completed_at` | **yes** | `Subtask.markDone(boolean)`, and nothing else |

Placed **first in the `Instant` group**, immediately above `createdAt`, per the code standard's field
order (group by declared type, alphabetical within the group).

Exposed by `getCompletedAt()`. **There is no setter and no constructor parameter** — that absence is the
design, not an omission.

## Invariants

**INV-1 · A moment implies done.** `completedAt != null ⟹ done == true`.
Guaranteed by `markDone` being the single writer: it only assigns a non-null moment on a false→true
transition, and nulls it on true→false.

**INV-2 · The converse does NOT hold, permanently.** `done == true` does **not** imply
`completedAt != null`. A subtask completed before FR-20 existed is legitimately `done = 1,
completed_at = NULL`, and stays that way — the spec forbids inventing a moment for it. This is why no
CHECK constraint is declared: the only invariant expressible in SQL is INV-1, which guards a direction
nothing threatens, while the shape a bug would actually produce is indistinguishable from a legacy row
forever.

**INV-3 · Only a transition writes.** A call that does not change `done` leaves `completedAt` byte-identical
— the same object, not merely an equal value. This is one guard clause serving two spec scenarios: the
idempotent update and the no-backfill rule.

**INV-4 · Erasure is total.** Un-completing sets `completedAt = null` and the previous value is stored
nowhere else. Re-completion writes a fresh moment; it does not restore the old one. The spec states this
and C-10 accounts for it.

**INV-5 · Not client-writable.** No request payload can reach the field. `SubtaskInput.completedAt` exists
only to be rejected (`@Null`), so a client that supplies it is told, rather than silently ignored — the
BR-06/BR-07 poison-field precedent.

**Unchanged by this feature (BR-06, BR-07).** The task's percentage stays *the proportion of subtasks
marked done*; its derived dates stay min/max of the subtask dates. `completedAt` is an input to neither.

## Migration

`src/main/resources/db/migration/V9__subtask_completed_at.sql` — the next in sequence after
`V8__message_overrides.sql`, in the house comment style (why before what).

```sql
-- FR-20 · When a subtask was completed.
--
-- A subtask carries done as a boolean and nothing about WHEN. That moment cannot be recovered
-- afterwards, and it cannot be derived: "finished after the planned date" computed as
-- done AND end_date < today turns TRUE for work delivered ON TIME as soon as today walks past
-- end_date. So it is written at the transition (OQ-38, human decision 2026-09-02).
--
-- NULLable, and NULL carries meaning rather than absence of data: every row that exists when this
-- runs was completed before the moment was ever recorded, and none is back-filled — inventing a
-- value would manufacture a lateness verdict out of nothing.
--
-- No CHECK, following V6: the API guarantees the invariant. The only constraint expressible here
-- (completed_at IS NULL OR done = 1) guards a direction nothing threatens, while the shape a bug
-- would produce (done = 1 with no moment) is exactly the legacy shape and can never be forbidden.
ALTER TABLE subtask
  ADD COLUMN completed_at DATETIME(6) NULL AFTER done;
```

`DATETIME(6)` matches `created_at` / `updated_at` on this table. **Microsecond resolution while
`Instant.now()` carries nanoseconds** — see the plan's precision risk: a read must be compared against a
read, never against a mutation response.

**Backfill: none, deliberately.** See INV-2, and the plan's reversibility table — this is the one-way
decision whose window closes at deploy.

## State transitions

| From | To | `done` | `completedAt` |
|---|---|---|---|
| new (not done) | created done | `false → true` | `null → now` |
| new (not done) | created not done | stays `false` | stays `null` |
| not done | done | `false → true` | `null → now` |
| done | not done | `true → false` | `value → null` |
| done | done (rename, reschedule, card) | unchanged | **unchanged — same object** |
| not done | not done | unchanged | stays `null` |
| done, legacy (`completed_at NULL`) | done (any update) | unchanged | **stays NULL — no backfill** |
