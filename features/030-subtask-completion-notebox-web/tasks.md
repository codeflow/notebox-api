# Tasks — Completing a subtask from the grid

**ID:** features/030-subtask-completion-notebox-web
**Plan:** `plan.md` (Approved 2026-09-04) · **Status:** Approved (human approval 2026-09-04, blanket) · **Date:** 2026-09-04

Five tasks. **T-01 carries every decision the plan could have got wrong** — both timezone traps and
the whole lateness rule — and it is pure functions, so it fails in milliseconds rather than after a
render. It goes first.

---

- [x] **T-01 · The three helpers, and the timezone traps**
      - files: `lib/tasks/completion.ts` *(new)*, `lib/tasks/completion.test.ts` *(new)*,
        `lib/api/types.ts`
      - covers: FR-20 · scenarios: the three "never late" ones (no moment / no end date / not done),
        *"A subtask completed after its planned end is marked late"*, *"…on or before… is not"*
      - depends: —
      - parallel: no — everything imports it
      - verify: `npm run verify`
      - **Binding on the author.** Two assertions must exist or the task is not done:
        `todayIso(new Date('2026-09-04T02:30:00Z'))` evaluated at UTC−3 must be **`2026-09-03`**, and
        a subtask with `completedAt: '2026-09-02T02:30:00Z'` and `endDate: '2026-09-01'` must be
        **not late** at UTC−3. Both fail against `toISOString()`, which is the whole point.
        Set the zone with `process.env.TZ` before the import, the way a date-sensitive suite must.

- [x] **T-02 · The tick asks, fills, or just sends**
      - files: `components/tasks/SubtasksPanel.tsx`,
        `components/tasks/CompleteSubtaskDialog.tsx` *(new)*,
        `components/tasks/SubtasksPanel.test.tsx`
      - covers: FR-20 · scenarios: *"Ticking Done with no dates asks"*, *"Confirming sets both dates
        to today"*, *"Cancelling reverts the tick"*, *"…only the end date missing…"*,
        *"…both dates present…"*
      - depends: T-01
      - parallel: no
      - verify: `npm run verify -- SubtasksPanel`
      - The dialog is `DeleteSubtaskDialog`'s shape — `af-dialogOverlay` + `af-dialog`, `open` prop,
        confirm before cancel. **The cancel scenario must assert that no request was made**, not just
        that the box is unticked: the box is controlled, so it un-ticks whether or not a request went
        out, and asserting only the checkbox would pass on a version that silently saved.

- [x] **T-03 · The late indicator and its tooltip**
      - files: `components/tasks/SubtasksPanel.tsx`, `components/tasks/SubtasksPanel.test.tsx`,
        `src/styles/adf-fusion.overrides.css`
      - covers: FR-20, OQ-41 · scenarios: the two indicator scenarios, plus *"Un-ticking Done removes
        the late indicator"*
      - depends: T-01
      - parallel: **yes** — CSS and a cell renderer; T-04 and T-05 touch neither
      - verify: `npm run verify -- SubtasksPanel`
      - Treatment per OQ-41: the `SeverityMessage` glyph at the row icons' size, after the end date,
        tooltip as its accessible name. **A recorded divergence from the handoff, not an invention** —
        the handoff draws no marker of this kind in a value cell.
      - jsdom sees no layout: the visual pass belongs to the live browser check at the end.

- [x] **T-04 · Date order, before the round trip**
      - files: `components/tasks/SubtaskEditor.tsx`, `components/tasks/SubtaskEditor.test.tsx` *(new)*
      - covers: FR-20 · scenario: *"The start date cannot be set after the end date, before saving"*
      - depends: T-01
      - parallel: **yes** — its own component and its own test file
      - verify: `npm run verify -- SubtaskEditor`
      - The client mirrors the server rule; it never replaces it. The existing server-rejection path
        keeps its own coverage, so a regression in either is still caught.

- [x] **T-05 · Both locales, and the guard that the catalog cannot give**
      - files: `lib/i18n/messages/en.ts`, `lib/i18n/messages/pt.ts`,
        `components/tasks/SubtasksPanel.test.tsx`
      - covers: C-09, BR-08 · scenario: *"Every string this feature adds resolves in Portuguese"*
      - depends: T-02, T-03, T-04 — it asserts strings those three introduce
      - parallel: no
      - verify: `npm run verify`
      - **OQ-37 is why this task exists as a task.** The message catalog reaches the 93 server
        messages and none of the 425 interface strings, so these five keys have no runtime override
        and no catalog coverage check. The pt-locale test is the only guard there is.

---

## Dependency chain

```
T-01 ──┬──> T-02 ──┐
       ├──> T-03 ──┼──> T-05
       └──> T-04 ──┘
```

**Parallelisable:** T-03 and T-04. T-02 is sequential with T-03 only because both edit
`SubtasksPanel.tsx`; running them in parallel worktrees would conflict on merge.

## Scenario coverage

| Scenario | Task |
|---|---|
| Ticking Done with no dates asks before completing | T-02 |
| Confirming sets both dates to today and completes | T-02 |
| Cancelling reverts the tick | T-02 |
| Ticking Done with only the end date missing fills it with today | T-01, T-02 |
| Ticking Done with both dates present changes only the flag | T-01, T-02 |
| A subtask completed after its planned end is marked late | T-01, T-03 |
| A subtask completed on or before its planned end is not marked late | T-01, T-03 |
| A subtask completed before this feature existed is never marked late | T-01, T-03 |
| A subtask with no planned end date is never marked late | T-01, T-03 |
| Un-ticking Done removes the late indicator | T-03 |
| The start date cannot be set after the end date, before saving | T-04 |
| Every string this feature adds resolves in Portuguese | T-05 |

**Nothing uncovered.** One thing no task can assert: that the indicator is *legible* — jsdom computes
no layout and loads no stylesheet. That is the live browser pass at the end, which is also where the
product owner validates the pair.
