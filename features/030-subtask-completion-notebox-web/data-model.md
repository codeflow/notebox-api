# Data model — Completing a subtask from the grid

**ID:** features/030-subtask-completion-notebox-web · **Date:** 2026-09-04

This satellite **owns no persistence.** Every durable fact lives behind the API (feat-029). What
follows is the read model as the client sees it, and the derived values computed from it — the
second half is where this feature's correctness lives.

## Read model (from the API, unchanged by this feature)

```ts
interface SubtaskDto {
  id: string;
  name: string;
  startDate: string | null;   // yyyy-MM-dd, the member's planned start
  endDate: string | null;     // yyyy-MM-dd, the member's planned end
  done: boolean;
  card: CardDto | null;
  completedAt: string | null; // ← NEW (feat-029): ISO-8601 UTC instant, or null
  createdAt: string;
  updatedAt: string;
}
```

**`completedAt` is the only field this feature adds to the client's picture**, and it is read-only:
the API rejects a client that supplies one (`task.subtask.completed_at.not_writable`).

### The type mismatch that is the whole design problem

| | Type | Timezone |
|---|---|---|
| `completedAt` | instant | **UTC**, as sent by the server |
| `endDate` | calendar date | **the member's**, as they typed it |

Comparing them is not a subtraction. An instant must first be resolved to a calendar date, and the
only calendar that answers the member's question is theirs. See the invariants.

## Derived values (this feature's own)

| Value | Rule | Where |
|---|---|---|
| `CompletionPlan` | what ticking Done requires: `ask` (no start), `fill` (start, no end), `send` (both) | `completionPlan(subtask, today)` |
| lateness | `done ∧ completedAt ≠ null ∧ endDate ≠ null ∧ localDate(completedAt) > endDate` | `isLateCompletion(subtask)` |
| today | `yyyy-MM-dd` from local `getFullYear/getMonth/getDate` | `todayIso(now?)` |

Nothing is cached and nothing is stored: all three are recomputed per render from the served row.

## Invariants

- **INV-1 · Lateness is compared in the member's timezone.** `completedAt` is converted to a local
  calendar date before it meets `endDate`. *Because:* the end date is a date the member chose in their
  own calendar. A São Paulo member (UTC−3) finishing at 23:30 on 01-Sep sends `02:30Z on 02-Sep`;
  compared in UTC that reads late, and **on time is the truth**.
- **INV-2 · No moment, no verdict.** `completedAt === null` is never late — not "assumed on time",
  not "assumed late", simply not shown. This is every subtask that existed before FR-20, and the
  moment is unrecoverable. *Violation:* a badge accusing work nobody can defend.
- **INV-3 · No end date, no verdict.** There is nothing to be late against.
- **INV-4 · Un-ticking clears the verdict**, because the API erases the moment and INV-2 then holds.
  The client stores no lateness of its own that could survive.
- **INV-5 · `todayIso` never goes through `toISOString()`.** That method emits UTC; at 21:00 in São
  Paulo it yields tomorrow's date, which would be written into a member's subtask as fact.
- **INV-6 · Confirming writes start AND end to today** — the product owner's rule (OQ-38). Cancelling
  writes nothing at all.

## Migrations

**None.** No client-side store, no schema, no local persistence. `completedAt` arrives on a payload
that already existed; a client reading a response from an older API simply sees `undefined`, which
INV-2 already treats as no verdict.
