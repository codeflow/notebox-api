# feat-023 — audit (round 1)

**Model:** opus · **Effort:** xhigh · **Date:** 2026-09-01
**Verdict: FAIL** — reopen `implement`. One user-visible correctness defect, verified live, plus
two lesser ones.

---

## F-01 · A mutation in the panel leaves the grid behind it stale — **ghost rows**

**Severity: high. Reproduced in the browser and confirmed against the database.**

`PanelBody` signals a change with `router.refresh()`. That refreshes server components; the grids
fetch in a client `useEffect`, which it does not re-run.

**Reproduced.** Records grid for *Reunião de projeto*, 13 rows:

| Step | Result |
|---|---|
| opened *Kickoff do Notebox* in the panel, deleted it, confirmed | panel closed |
| database | the record is **gone** (only the same-named record under a different type remains) |
| the grid behind the panel | still **13 rows**, *Kickoff do Notebox* still listed |

Clicking that row now opens a record that does not exist. The same defect covers every panel
mutation over a client-fetched list: `record-new` (the new record never appears) and `task-edit`
(the grid keeps the old values).

**This codebase already solved this, and the fix's own comment says so:**

```ts
// lib/navigation/treeRevision.ts
// `router.refresh()` is not an option here: the Navigator fetches in a client `useEffect`, which
// a server-component refresh does not re-run.
```

That is feat-008's audit finding F-01, written down, and the panel walked into it anyway. The fix
is to reuse that pattern — a revision signal the grids subscribe to — not to invent a second one.
The inline row edit must stay out of it: it repaints from the response deliberately, so the member
does not get moved.

## F-02 · Opening other content over a dirty form discards it silently

**Severity: medium.**

`PanelHost.open()` clears `dirty` and replaces the content. INV-P3 guards the **close** door and
this one is left open: type half a task into the new-task form, choose Administration → Members
from the menu, and the draft is gone with no prompt. The work lost is the same work; only the
gesture differs, and A-6's reasoning ("replacing the panel's content under a half-typed form loses
work") applies verbatim.

## F-03 · A rejected row save unlocks the frozen column widths mid-edit

**Severity: low, but it is T-09's F-02 returning through the error path.**

`confirmRow(record).then(leaveEdit)` — `useRowEditor.confirm` catches its failure and resolves, so
`leaveEdit()` runs even when the save was rejected. The row correctly stays in edit (INV-E3) while
`lockedWidths` goes null, so the columns jump exactly as they did before the fix, at the moment the
member is reading an error message.

## F-04 · Four message keys are now referenced only by the test that asserts their absence

**Severity: informational.** `navigator.administration`, `navigator.groups`, `navigator.members`
and `navigator.translations` survive T-10 in both catalogs with no rendering code left.

---

## Judged, not a finding

### The subtasks-grid amendment (T-07)

The implementer resolved a contradiction inside the spec and **recorded it in the spec as a dated
amendment rather than acting on it silently**, which is what the pipeline asks for. Judging it here:

- The spec's Out list named the subtasks grid; the spec's own **rule** — *a grid edits inline when
  its listed fields ARE its editable fields* — points the other way for it, since its columns
  (name, start, end, card) are the whole of a subtask.
- That row editor is **feat-010's specced behaviour** (its spec S13, with feat-012's tested
  violation matrix). A later feature's Out bullet does not delete an earlier feature's scenario;
  precedent outranks it in the source hierarchy.
- The feature's own data model gives `PanelContent` no subtask kind, so "routes to the panel" had
  no destination designed for it.

**Accepted.** Flagged for the product owner to confirm at review: if they did mean the subtask row
editor, that is a change to feat-010 and needs its own spec.

### The extra read before a row write (INV-E2)

The row's confirm now issues `GET` then `PUT`. The scenario says *"exactly one **update** request
is issued"*; a read is not an update, and T-09 proved the read is the only way the write can avoid
erasing hidden fields. **Accepted as the correct reading**, and it is documented at the call site.

---

## The checks that passed

| Check | Result |
|---|---|
| **Traceability** | All 14 scenarios cite a test. The A-7 outline lost one example by amendment (above); the remaining example is asserted in `TasksTable.test.tsx`. |
| **Scenario honesty** | One test **was** dishonest and is fixed: `RecordsGrid.test.tsx` asserted hidden fields survived a row save using a fixture whose listing row carried values `forListing` never sends. Split into `row()`/`detail()`; the probe now kills the real defect. Every other T-0x test was probed at implement time. |
| **C-02 authenticated** | Unchanged — the panel calls the same clients through `authFetch`. |
| **C-03 least-privilege** | `MembersScreen` and `CatalogScreen` carry their `isAdmin` gate into the panel verbatim; the server remains the control. No new opener bypasses it. |
| **C-07 image upload** | The record form moved as a component; `RecordForm` → `imagesClient` path untouched. |
| **C-08 rich-text sanitize** | Same — `RichTextEditor` moved with the form, not re-implemented. |
| **C-09 localization** | All 18 new keys present in **both** catalogs; keyset guard green. |
| **C-12 secret at rest** | `RowEditorCell` returns the mask **first**, with no editable branch; asserted against `annotationRecordsClient.reveal` (the client feat-020's audit found asserted wrongly) and rendered as ADMIN so the affordance is reachable if it existed. `rowReplaceInput` never sets `clearSecret`, and the API preserves ciphertext byte-identical. |
| **BR-05 destructive confirm** | Record delete and group delete both confirm; the group's wording still says its items survive. |
| **BR-09 visibleForViewing is presentation** | Not used for access control anywhere new. The listing projection is the *reason* for F-01's fix, not a control. |
| **INV-P4 confirmation replaces** | Measured live: `.af-dialogOverlay` count 0, one `[role=alertdialog]`, one panel. |
| **INV-R1 routes kept** | All seven wrappers assert their content and their `replace` target. |
| **Scope** | Nine files outside the plan's blast-radius table, all of them the components the six screens had to be extracted into. The plan named the *pages* and not the components they became — an under-specified plan, not scope creep. `GroupsPanel`/`DeleteGroupDialog` were anticipated by INV-P4 but missing from the table. |
| **Open Questions** | OQ-35 is answered by A-1..A-7, declared in the spec as assumptions before implementation, not closed silently afterwards. |

---

## What reopening `implement` must fix

1. **F-01** — replace `router.refresh()` with the established revision signal; grids subscribe.
2. **F-02** — `open()` must go through the same discard guard as `close()`.
3. **F-03** — release the width lock only when the row actually leaves edit.
4. **F-04** — remove the four dead keys.

---

# feat-023 — audit (round 2)

**Date:** 2026-09-01 · **Verdict: PASS**

`implement` was reopened with `--cascade` and all four findings were fixed. Each was re-verified
by the means that found it: the two live ones in the browser, the rest by probe.

## F-01 · Ghost rows — **fixed, verified live**

`lib/data/listingRevision.ts`, deliberately the same shape as `treeRevision.ts`. `RecordsGrid` and
`TasksTable` subscribe with `useSyncExternalStore` and carry the revision in their fetch deps;
`PanelBody` bumps it after a delete or a save resolves, never optimistically. The inline row edit
does **not** bump it — that path repaints one row from the response so the member is not moved.

| | Before | After |
|---|---|---|
| grid rows before deleting from the panel | 13 | 12 |
| grid rows after | **13 — the deleted record still listed** | **11 — gone** |

Probe: dropping `listingRevision` from the grid's dependencies kills the test, and only that test.

## F-02 · The other door onto unsaved input — **fixed, verified live**

`open()` now goes through the same guard as `close()`, and confirming the discard **completes the
interrupted move** rather than merely closing. Live: typing into the record form and then choosing
Administration → Members asked first, kept Members off screen while it asked, and the typed text
was still there after *Keep editing*; a second attempt, discarded, landed on Members.

Probe: letting `open()` replace without asking kills all three of its tests.

## F-05 · **The guard was destroying the input it exists to protect** — found by F-02's test

Not in round 1, and it should have been. The confirmation rendered *instead of* the content, which
unmounted the form — so *Keep editing* restored an **empty** form. The work was lost either way;
choosing to keep it just lost it more quietly.

The body is hidden (`hidden` + `aria-hidden`) rather than unmounted. That is what INV-P4 is about
— one thing perceivable, one meaning for Escape — and the assertions moved from
`toBeInTheDocument` to `toBeVisible`, which is the question that was always being asked. Confirmed
live: `.nb-panelBody[hidden]` present while the delete confirmation was up.

**Why round 1 missed it:** its INV-P4 evidence counted overlays and alertdialogs. Both counts were
correct while a form's state was being thrown away behind them — a check that measured the shape
of the guard and not what it cost.

## F-03 · Width lock on the error path — fixed

`leaveEdit()` moved inside the success branch of `confirmRow`. A rejected save keeps the row in
edit **and** keeps its geometry.

## F-04 · Dead keys — removed

The four `navigator.*` keys are gone from both catalogs; the test asserts the absent words.

---

## Verdict

**pass.** `verify` green at 686 tests. The two findings that mattered were user-visible data or
work loss, and both were caught by leaving the desk — F-01 in the browser, F-05 by writing the
test for another finding. Neither was reachable from the suite as it stood.

**One item for the human at review:** the subtasks-grid amendment (judged in round 1). If the
product owner did mean the subtask row editor when they named that grid, that is a change to
feat-010's spec S13 and needs its own feature, not a line in this one.
