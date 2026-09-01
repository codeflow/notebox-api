# Plan — feat-023 records panel

**Spec:** `spec.md` v1 · **Satellite:** `notebox-web` (react/next)

> The spec carries seven stated assumptions `[A-1..A-7]` in place of the product owner's answers
> to OQ-35. This plan implements those assumptions; if any is corrected, the affected seam is
> named in each section so the cost of changing it is visible.

## Origin
- **Spec:** `features/023-records-panel/spec.md` · **US:** US-2.2 · **FRs:** FR-04, FR-05, FR-09
- **BRs:** BR-05, BR-09, BR-10 · **Compliance that applies:** C-02, C-03, C-07, C-08, C-09, C-12

---

## Approach

### 1. One panel, one opener — the seam that stops three callers drifting (R3)

The spec's hardest structural requirement is that a menu item, a grid icon and an empty-state
button open **the same thing**. That is a state problem, not a component problem, so it gets a
single owner:

```
PanelHost         mounted once in the authenticated layout, beside the Navigator
  ↳ usePanel()    open(content) / close() / expand toggle
```

`open()` takes a **discriminated union** naming what to show — `{kind:'record', id}`,
`{kind:'record-edit', id}`, `{kind:'task-new'}`, `{kind:'groups'}`, `{kind:'members'}`,
`{kind:'catalog'}` — not a rendered node. A caller therefore cannot pass "its own version" of a
screen, which is exactly how the three paths would drift.

**Reuse, not invention:** the chrome is the existing `af-drawer` / `af-drawerDock` /
`af-drawerTab` from `NotesDrawer` (design 16, 43 lines). What it lacks is a second width, a
content slot and focus handling.

### 2. Expand as a width, not a mode [A-5]

`af-drawer` gains a `data-width="normal|expanded"` attribute and the panel header gains a toggle.
Two CSS widths, one attribute, no layout mode. The choice lives in `sessionStorage` so it survives
navigation within the session and does not become a stored preference nobody asked for.

> **If A-5 is corrected** (say, to a full-screen mode) only the CSS and the toggle change; the
> content contract is untouched.

### 3. The nesting rule, as two different mechanisms [A-6]

This is where the product owner's sharpest question lands, and the two cases are genuinely
different, so they get different mechanisms rather than one compromise:

| Case | Mechanism | Why |
|---|---|---|
| **Confirmation** (destructive) | The panel's content is **replaced** by the confirmation, with back/confirm | Transient, holds nothing. A dialog stacked over a panel is two overlays deep and the Escape key stops meaning one thing. |
| **Creation form** (holds input) | Opens as its own panel content; **closing asks first** when anything was typed | Replacing content under a half-typed form loses work — the exact risk named in the collect. |

The "anything was typed" test is a **dirty flag the form owns**, not a diff of the draft: a form
knows when it changed, and comparing objects invites false positives that would nag on every
close.

### 4. Inline editing — copy the pattern that already works

`TranslationsTable` **already ships** enter-edit → confirm/cancel with `RowActions`. That is the
precedent; the generic version is extracted from it rather than designed:

```
useRowEditor<T>()   which row is editing, the draft, confirm/cancel
<RowEditorCell>     renders a field as value or control, by field type
```

- **[A-1]** entry is the edit icon. **[A-2]** confirm sends **one** update for the row; a rejection
  keeps the row in edit with the member's values and surfaces the server's message.
- **[A-4] the compliance boundary:** `RowEditorCell` returns the **mask** for `field.secret`,
  with no editable branch at all. Not a disabled input — no input. A disabled control is one prop
  away from being enabled by someone who does not know why it was disabled.
- **[A-7]** a grid opts out by not being given the editor: `TasksTable` and `SubtasksPanel` keep
  their action icons but route edit to the panel.

### 5. The tree loses Administration [R5]

Sequenced **last**, and only after the panel serves those three destinations — removing the branch
earlier would leave the product owner's stated reason ("they already open in a popup") untrue.

---

## Alternatives rejected

1. **A modal dialog per screen instead of one panel.** Rejected: it is what exists today in
   spirit (a screen per surface), and it multiplies the nesting question by six.
2. **A routed panel (`?panel=record:123`).** Attractive — shareable, back-button-friendly. Rejected
   **for this feature**: it makes every panel state a route change, and the app's screens are
   client-fetched, so each open would cost a navigation. Worth revisiting once the panel is proven;
   noted as reversible below.
3. **Editing every grid inline, including tasks.** Rejected: the product owner named the exception,
   and A-7 gives the rule behind it.
4. **A shared `isDirty` computed by comparing drafts.** Rejected in §3.
5. **Rewriting `NotesDrawer` into the new panel.** Rejected: it is a different thing (a dock tab
   for notes on one screen). It keeps working as-is; the panel borrows its CSS idiom, not its
   component.

---

## Blast radius

| File | Change | Risk |
|---|---|---|
| `components/panel/PanelHost.tsx` · `usePanel.ts` | **new** — the single opener and its state | R3 |
| `components/panel/PanelChrome.tsx` | **new** — header, expand toggle, close-with-guard | R2 |
| `app/(app)/layout.tsx` | mounts `PanelHost` once | — |
| `components/grids/useRowEditor.ts` · `RowEditorCell.tsx` | **new**, extracted from `TranslationsTable`'s pattern | R1 |
| `components/annotationRecords/RecordsGrid.tsx` | gains inline editing | R1, C-12 |
| `components/tasks/TasksTable.tsx` · `SubtasksPanel.tsx` | edit icon routes to the panel [A-7] | — |
| `app/(app)/…/records/[recordId]/{page,edit}` · `tasks/new` · `groups` · `members` · `translations` | become panel content; routes kept as thin wrappers | R5 |
| `components/navigation/Navigator.tsx` | Administration branch removed | R5 |
| `components/MenuBar.tsx` | administration + workspace items open the panel | R3 |
| `lib/i18n/messages/{en,pt}.ts` | expand, discard prompt, 4 row-action tooltips | C-09 |

**Routes are kept as wrappers, not deleted.** A shared link to `/annotation-types/x/records/y`
must not 404 — it opens the screen with the panel showing that record. Deleting the routes would
break every link anyone has saved.

---

## Reversibility

| Decision | Kind | Note |
|---|---|---|
| One `usePanel` opener | reversible | Internal seam. |
| Panel content as a union | reversible | Adding a surface is one variant. |
| Expand as an attribute [A-5] | reversible | CSS + toggle. |
| Confirmation replaces content [A-6] | reversible | One component swap. |
| **Secret never editable [A-4]** | **one-way in practice** | Loosening it later needs a compliance argument, not a preference. |
| Not routing panel state | reversible, but later costs more | Alternative 2 — the longer the panel exists unrouted, the more callers assume it. |

## Risk, and the signal that reveals it

| # | Risk | Signal |
|---|---|---|
| R1 | An editable control over a Secret value | A test rendering a Secret visible field in edit mode asserts **no input exists** and `annotationRecordsClient.reveal` is untouched. |
| R2 | Unsaved input lost on close | Type, close, assert the prompt; cancel, assert the input survives. |
| R3 | Three callers drifting | One test opens the same content from menu, grid and empty state and asserts identical rendered content. |
| R4 | Confirmation improvised per screen | The Groups delete scenario asserts the panel's content is replaced, not stacked. |
| R5 | Tree branch removed too early | The Administration removal task depends on the panel serving all three destinations. |
| R6 | A shared link breaking | A test hits each kept route directly and asserts the screen renders with the panel open. |

## Contracts

**No API contract changes.** `contracts/consumed.md` records the endpoints the migrated screens
already call, so the auditor can confirm this feature added none.
