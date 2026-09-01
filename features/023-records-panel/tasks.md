# Tasks — feat-023 records panel

**Plan:** `plan.md` · **Spec:** `spec.md` v1 (seven stated assumptions)

Ordered so the riskiest claim goes first. **T-01 leads because the whole feature rests on one
seam** — a single opener that many callers invoke. If that cannot hold, the three-callers risk
(R3) is real and the plan moves; finding out costs least before six screens depend on it.

**T-04 is the compliance task and does not move.** It is scheduled early on purpose: if a Secret
field cannot be kept out of an inline editor, the feature's scope changes.

---

- [x] **T-01 · One panel, one opener**
      - files: `components/panel/PanelHost.tsx`, `usePanel.ts`, `PanelHost.test.tsx`, `app/(app)/layout.tsx`
      - covers: scenario *"One destination, many callers"* · **INV-P1**, **INV-P2**, risk **R3**
      - the assertion that matters: the same `PanelContent` opened from three different callers
        renders identical content — the union takes a value, never a node
      - depends: —
      - parallel: no (everything builds on this)
      - verify: `npx vitest run components/panel`
      - probe: let `open()` accept a ReactNode — the identical-content assertion must fail

- [x] **T-02 · The panel's chrome: expand, close, and focus**
      - files: `components/panel/PanelChrome.tsx`, its test, `src/styles/adf-fusion.overrides.css`
      - covers: scenario *"Expanding the panel"* · **[A-5]**
      - reuses `af-drawer` / `af-drawerDock` from `NotesDrawer` — the CSS idiom, not the component
      - the width is `data-width="normal|expanded"` in `sessionStorage`; opening moves focus into
        the region and Escape closes under T-03's guard
      - depends: T-01
      - parallel: no (same directory)
      - verify: `npx vitest run components/panel`
      - **live check (T-09):** jsdom cannot see a width. The two widths are measured in a browser.

- [x] **T-03 · Closing with unsaved input asks first**
      - files: `components/panel/PanelChrome.tsx`, `usePanel.ts`, tests
      - covers: scenarios *"Closing a creation form with unsaved input"* and *"…with nothing typed"* · **INV-P3**, risk **R2**
      - the dirty flag is **owned by the form**, never a diff of drafts — a diff nags on every close
      - both directions asserted: typed → asks, and cancelling the prompt keeps the input; untyped → closes silently
      - depends: T-01
      - parallel: no
      - verify: `npx vitest run components/panel`

- [x] **T-04 · A Secret field has no editable control — the compliance task**
      - files: `components/grids/RowEditorCell.tsx`, its test
      - covers: scenario *"A Secret field is not editable in a grid"* · **INV-E4**, **C-12**, risk **R1**
      - `RowEditorCell` returns the mask for `field.secret` with **no editable branch at all** —
        not a disabled input, which is one prop away from being enabled by someone who does not
        know why it was disabled
      - the test asserts **no input exists** AND that `annotationRecordsClient.reveal` was not
        called — mocked explicitly, because feat-020's audit found exactly this assertion made
        against the wrong client
      - depends: —
      - parallel: yes (`isolation: worktree`)
      - verify: `npx vitest run components/grids`
      - probe: render an input for a secret field — both assertions must fail

- [x] **T-05 · Inline editing: enter, confirm, cancel**
      - files: `components/grids/useRowEditor.ts`, `RowEditorCell.tsx`, tests
      - covers: scenarios *"Editing a visible field in place"*, *"Confirming writes the row once"*, *"Cancelling discards"* · **INV-E1**, **INV-E2**, **[A-1]**, **[A-2]**
      - extracted from `TranslationsTable`, which already ships this pattern — copied, not designed
      - depends: T-04 (the cell renders before the editor drives it)
      - parallel: no
      - verify: `npx vitest run components/grids`
      - probe: send one request per field instead of per row — the count assertion must fail

- [x] **T-06 · A failed save keeps the member's work**
      - files: `components/grids/useRowEditor.ts`, its test
      - covers: scenario *"A failed save keeps the member's work"* · **INV-E3**, **[A-2]**
      - both halves: the row stays in edit with the typed values, and nothing was written
      - depends: T-05
      - parallel: no
      - verify: `npx vitest run components/grids`

- [x] **T-07 · The grids that opt out, and the rule behind it**
      - files: `components/tasks/TasksTable.tsx`, `SubtasksPanel.tsx`, `RecordsGrid.tsx`, tests
      - covers: Scenario Outline *"A grid whose listed fields are not its editable fields opens the panel"* · **[A-7]**
      - records grid gains the editor; tasks and subtasks route their edit icon to the panel
      - depends: T-05
      - parallel: no
      - verify: `npx vitest run components/tasks components/annotationRecords`

- [x] **T-08 · The six surfaces move in, and the routes stay**
      - files: the six screens, `MenuBar.tsx`, `app/(app)/**` route wrappers, integration tests
      - covers: the panel content scenarios · **INV-R1**, risk **R6**
      - **routes are kept as thin wrappers, never deleted** — a shared link must not 404; it opens
        the screen with the panel showing that content
      - includes the Groups delete scenario (**INV-P4**: the confirmation REPLACES content)
      - depends: T-02, T-03, T-07
      - parallel: no
      - verify: `npx vitest run "app/(app)"`

- [x] **T-09 · Live browser pass**
      - files: — (evidence, recorded in the audit)
      - **why it is a task:** jsdom loads no stylesheet. feat-020 and feat-021 each shipped a defect
        that only a browser could see — a `border` shorthand resetting a colour, and a root that was
        "current page" for a screen reader and nobody else. A panel with two widths and a focus
        trap is exactly that kind of thing.
      - check: both widths measured; the panel does not cover the Navigator at normal width; focus
        enters on open and Escape closes; the confirmation replaces rather than stacks; a row in
        edit lines its controls up with the cells above
      - depends: T-08
      - parallel: no
      - verify: measured in a browser, numbers stated — not a screenshot alone

- [x] **T-10 · Administration leaves the tree**
      - files: `components/navigation/Navigator.tsx`, its test
      - covers: scenario *"Administration leaves the tree"* · risk **R5**
      - **deliberately last:** removing the branch before the panel serves those three destinations
        would leave the product owner's stated reason ("they already open in a popup") untrue
      - depends: T-08
      - parallel: yes (`isolation: worktree`)
      - verify: `npx vitest run components/navigation`

---

## Summary

| | |
|---|---|
| Tasks | 10 |
| Chain | T-01 → T-02, T-03 · T-04 → T-05 → T-06, T-07 → T-08 → T-09, T-10 |
| Parallel | T-04, T-10 (`isolation: worktree`) — genuinely separate files |
| Serial | T-01…T-03 share `components/panel/`; T-05…T-07 share the editor |
| Scenarios uncovered | **none** — all 14 are cited |
| Probes named | T-01, T-04, T-05 |

**The two tasks that must not be reordered:** T-04 before any editor exists (if a Secret field
cannot be kept out, the scope changes), and T-10 after T-08 (the tree's reason must be true when
the branch goes).
