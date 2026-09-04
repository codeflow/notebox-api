# Plan — Completing a subtask from the grid

**ID:** features/030-subtask-completion-notebox-web
**Spec:** `spec.md` (Approved 2026-09-04, 12 scenarios)
**Status:** Approved (human approval 2026-09-04, blanket) · **Date:** 2026-09-04
**Satellite:** notebox-web (react, next) · **verify:** `npm run verify`

## Origin

Spec `030`, US-4.1, **FR-20** — the member-facing half of a fact feat-029 already records.

## Approach

Three pure functions and one dialog. Everything else is wiring.

### 1. The tick's decision is a pure function, not a branch inside a handler

`completionPlan(subtask, today)` answers one question — *what does ticking Done on this subtask
require?* — and returns one of three shapes:

- `{ kind: 'ask' }` — no start date. The member must be asked before anything is sent.
- `{ kind: 'fill', endDate: today }` — a start date but no end.
- `{ kind: 'send' }` — both dates present; only the flag changes.

It takes `today` as an argument rather than reading the clock. The whole feature's correctness is a
date comparison, and a function that reads `new Date()` internally can only be tested by mocking the
clock — which this codebase does nowhere.

Un-ticking never consults it: `done → false` is always a plain send.

### 2. Lateness is a pure function, and it compares in the member's timezone

`isLateCompletion(subtask)` is true only when **all** hold: `done`, `completedAt !== null`,
`endDate !== null`, and the **local calendar date** of `completedAt` is after `endDate`.

**The timezone is the decision, not a detail.** `completedAt` arrives as a UTC instant
(`2026-09-04T02:47:17Z`); `endDate` is a calendar date the member chose in their own timezone. A
member in São Paulo (UTC−3) who finishes at 23:30 on 01-Sep sends an instant of `02:30Z on 02-Sep`.
Compared in UTC that is late; compared locally it is on time — and *on time is the truth*, because
the date they chose meant their own calendar. So the instant is converted to a local date first.

The three "never late" populations fall out of the conjunction rather than needing special cases: no
moment (every subtask that exists today), no end date, and not done.

### 3. `todayIso()` formats locally, and that is the same trap in the other direction

`new Date().toISOString().slice(0, 10)` is **wrong** for any member not on UTC: at 21:00 in São Paulo
it yields tomorrow. The helper builds the string from `getFullYear/getMonth/getDate`, which are local
by definition. It lives beside `formatDate.ts`, which already owns the yyyy-MM-dd contract.

### 4. The question is a dialog, matching the screen it appears on

`CompleteSubtaskDialog`, modelled on `DeleteSubtaskDialog` in the same folder: `af-dialogOverlay` +
`af-dialog`, `dlgTitle`/`dlgBody`/`dlgFoot`, an `open` prop, confirm-before-cancel.

**Not `panel.askConfirm`**, although that is the newer idiom. `SubtasksPanel` renders on the task
detail *screen*, not inside the side panel; its own delete already uses a stacked dialog, and putting
this one in the panel would open a side surface to ask a question about a row the member is looking
at. Consistency with the screen beats consistency with the newest mechanism.

**Cancel reverts for free, and that is worth stating.** The checkbox is controlled
(`checked={subtask.done}`), so cancelling means sending nothing and letting the row re-render from
unchanged state. No manual un-ticking, no local flag to get out of sync. The scenario still asserts
it, because "free" is a property of the current markup, not a guarantee.

### 5. Date-order feedback in the form

`SubtaskEditor` already routes the server's `task.subtask.date.invalid` to `errors.dates`. This adds
the same verdict client-side, on change, so the member learns before saving. The **server rule stays
authoritative** — the client check is an early mirror, never a replacement, and the existing
server-rejection path is untouched.

## Alternatives rejected

**Compute lateness on the server.** feat-029 explicitly declined it, and its spec named the reason:
"late" may acquire a grace period or a working-day rule, and freezing it into the read model makes
that a breaking change. Rejected here for a second reason the API could not see: the comparison is
timezone-dependent and the server does not know the member's timezone.

**Ask with a `window.confirm`.** Rejected: it cannot be localized, cannot be styled, and cannot be
asserted in jsdom the way the product's own dialog can.

**Fill the missing start date silently with today, without asking.** Rejected — it is the product
owner's explicit decision that the member is asked. It also guesses: a subtask completed today may
have started weeks ago, and writing today as the start would state something false.

**Derive lateness as `done && endDate < today`.** Rejected: it is the exact bug FR-20 exists to
prevent — work delivered on time starts reporting itself late once today passes its end date.

## Blast radius

| File | Change |
|---|---|
| `lib/api/types.ts` | `completedAt: string \| null` on `SubtaskDto` |
| `lib/tasks/completion.ts` *(new)* | `completionPlan`, `isLateCompletion`, `todayIso` |
| `components/tasks/SubtasksPanel.tsx` | the tick consults `completionPlan`; the late indicator in the end-date cell |
| `components/tasks/CompleteSubtaskDialog.tsx` *(new)* | the question |
| `components/tasks/SubtaskEditor.tsx` | client-side date-order check |
| `lib/i18n/messages/en.ts`, `pt.ts` | 5 keys: dialog title, body, confirm, cancel; late tooltip; date-order message |

**Not touched:** `toSubtaskInput` (the overrides argument already carries everything), the task
detail page, the API client, any grid but this one.

## Risk

| Risk | Signal that reveals it |
|---|---|
| Timezone handling wrong in either direction | unit tests pin both helpers at a UTC offset; a São Paulo evening instant must read as its local date |
| The late rule fires on legacy rows | a scenario with `completedAt: null` and a past end date |
| The dialog opens but the tick still sends | the cancel scenario asserts no request was made |
| A new string ships untranslated | the pt scenario, and `npm run verify` |
| The client check diverges from the server's | the client mirrors the server rule only; the server path keeps its own test |

## Reversibility

| Decision | Reversible? |
|---|---|
| Compare in local time | yes — one function, one test |
| Dialog rather than `askConfirm` | yes — the question's content is independent of its container |
| Writing start = end = today on confirm | **no** once a member confirms: it writes real dates. This is why the member is asked. |
