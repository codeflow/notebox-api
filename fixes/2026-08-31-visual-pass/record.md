# Record — visual pass (fix lane)

**Brief:** `brief.md`, 26 items · **Branch:** `fix/visual-pass` off `develop` ·
**Harness:** `npm run verify` exit 0, **630 tests, 78 files**

> **This does not deliver all 26 items, and that is deliberate.** The brief grew during collect
> from "a few visual fixes" into three lanes at once: real bugs, a chrome pass, and a feature-sized
> panel redesign. What is here is finished and verified in a browser. What is not is listed at the
> bottom with the reason — nothing was silently dropped.

---

## Delivered

### The three real bugs — each fixed and each probed

| # | Defect | Cause | Probe |
|---|---|---|---|
| 5.1 | A saved type/field icon did not come back on the edit screen | `fromDto` hydrates `iconImageId` but has no object URL to go with it, and the editor only rendered a thumbnail when handed one. A type WITH an icon looked exactly like one without. | new assertion fails without the resolve |
| 8.3 / 15.1 | An image in the rich text rendered as the browser's broken placeholder | The document stores `data-image-id`, never a URL — the binary endpoint is authenticated (INV-R6). `RichTextValue` resolved ids on the READ side; the editor never did. | removing the resolve call fails the new assertion alone |
| 12 | A second Save created a **duplicate task** | The button did carry `disabled={saving}`, but `saving` was cleared in a `finally`, re-arming it the instant the request resolved — while navigation had not happened yet. The first save succeeded silently; the second click was reasonable. | restoring the `finally` fails the new assertion alone |

Item 12's fix went to **all three** create/edit forms (task, type, record) — they shared the
pattern, so they shared the defect. Item 15 asked whether the Details tab shared the image bug: it
does, same component, fixed once.

**Live evidence for the icon bug** (the one that could be reproduced with existing data): the type
edit screen now renders the thumbnail — `hasThumbnail: true` with a resolved `blob:` src — while
the three fields that have no icon stay empty.

### Form chrome — one rule, not four patches

The brief reported short rules on four screens (items 8, 10, 15, 22) and mixed button alignment on
four (item 17). Both had a single cause: **`.af-buttonBar` carried `max-width: 780px`**, a cap
meant for the input column, and `.nb-taskForm` carried it too — which is why item 10 saw BOTH of
its rules stop short. The cap now applies to the fields, not to the dividers.

Measured live on `/tasks/new`:

| | before | after |
|---|---|---|
| Title rule | ended ~1045px | **267 → 1426** |
| Button bar | ended ~1045px | **267 → 1426** (identical) |
| Save / Cancel | left on two screens, right on two | **right everywhere** (Save at 1313, Cancel at 1368) |

### Fusion severity icons — defined once for four requests

Items 3, 9, 11 and 25 each asked for icons independently. Three separate answers would have
drifted in shape and tone, so there is now one set in `components/Icons.tsx`: `ErrorIcon`,
`WarningIcon`, `InfoIcon` (filled disc, white glyph, tones matching the message-banner rules
already in the stylesheet), plus `BrowseFileIcon`, `ConfirmEditIcon` and `CancelEditIcon`.

`CancelEditIcon` is deliberately drawn in the theme's blue, **not** the delete tone — item 25's
requirement, born from the human misreading a cancel `✕` as a delete.

### The confirm dialog (item 3)

The confirm button carried the whole question as its label
(*"Are you sure you want to delete this annotation type?"*), which made it far wider than Cancel
and pushed the pair onto separate lines. The question moved into the body beside a warning icon;
the button keeps the verb (`Delete` / `Excluir`), and `dlgFoot` is now flex/end with a gap.
**New i18n key `annotationType.delete.question` in both locales.**

### Field validation messages (item 11)

They wore `.nb-msg-error` — the **banner** class — which paints a full-width pink strip: the
message ran the whole content width, started under the label column rather than beside its input,
and shoved the following rows down. New `FieldError` component: error disc, text, sized to
content.

Measured live after a failed submit: the message sits at **left 415, the same left as the input**,
**160px wide** instead of ~780, with the icon present.

### Empty states (item 9)

`No tasks yet` was bare text on white. New `EmptyState` component — info disc, message, action —
in the ADF message-panel shape, applied to **both** the tasks list and the annotation-types list,
which had been answering the same situation two different ways.

### Chrome and grid

- **Item 1.1** — the tenant name is out of the workspace title. It read as part of the product's
  name ("Notebox — Live Pass") while the branding bar already states both. Title is now `Notebox`.
- **Item 18.2** — `Edit` and `View` menus removed. They held undo/cut/copy and expand/collapse,
  chrome with no behaviour behind it. **Recorded deviation from design 05/15**, like the
  Administration tab before it.
- **Item 18.4** — `Groups` added to Administration. The Navigator already listed it there; the
  menu bar disagreeing with the tree was the smell.
- **Item 2.1** — types-grid rows: **36px → 25–32px**. The height came from the action buttons, not
  the cell padding.
- **Item 2.2** — the band's header gained its top edge. It was being stripped by
  `.af-panelCollection .af-table thead tr:first-child th { border-top: none }`, a rule that exists
  because a grid touching the panel's border would draw two lines 1.5px apart — reasoning that
  does not reach a grid nested mid-table. Fixed at matching specificity, declared later, so it
  wins only where it should. Verified live: `1px solid rgb(169,184,199)`.
- **Item 1.3** — the folder icon drew 9.9 units tall against its siblings' 13.9. Now **12.6**.

---

## NOT delivered, and why

**Everything routed to OQ-35** — the side panel and inline editing (items 2.3, 2.4, 4.4, 7.1, 8.1,
10.1, 13.3, 14.4, 18.3, 19, 20.4, 21.1, 22.3, 24.9, 26). This is a feature with a spec, and the
brief itself grew its scope: the panel is not an add-on to the grids, it becomes the primary
surface for viewing and editing. Folding it into a fix PR would make the diff unreviewable.

**Item 18.1 — `Preferences` / `Help` should work.** There is nothing behind them, and "should
work" does not say what they do. Preferences implies a settings surface nobody has defined; Help
implies content nobody has written. Building either would be inventing product. OQ-28 set the
precedent for a dead affordance on the login screen.

**Item 24.3 — add a new locale.** New catalogs, a translation surface, and a fallback rule.
Feature work.

**Items 4.1–4.3 — the type detail screen as a grid** with visible/secret icons. Not reached in this
pass. Worth doing, and item 4's own note flags the ordering question: it gains inline editing at
OQ-35, so building it as a grid now means building it twice.

**Items 13.1–13.2, 14.1–14.3, 20.2 — action-icon columns** (tasks grid, task detail, subtasks,
Groups). Not reached. Each needs localized tooltips, so each adds i18n keys — real work, not
decoration. `RowActions` is the precedent to copy.

**Item 16 — code-block line numbers and highlighting in the EDITOR.** Investigated: **lowlight is
already installed** and `RichTextValue` already highlights with line numbers on the read side. So
this is fix-lane, not a new dependency — but it is editor styling that was not reached.

**Items 1.2, 20.1, 22.1, 23, 24.9 — the open-ended "embelezar" requests** (workspace, Groups,
Members, the password dialog, the message catalog). The dialog *layout* was fixed generically
(items 3/21/23 shared one cause), but the per-screen restyles were not attempted.

**Item 24.5–24.6 — the i18n audit.** `0 of 80 reworded by this workspace` is not in the catalog,
and the rule "every text and every tooltip localizable" needs a sweep over the whole app, not an
edit.

**Item 9.1 — the tree root's highlight.** Belongs to **feat-021's PR #21**, not here: that change
is what paints it, and it is not merged yet. Adjusting it in this branch would edit a feature
that is still under review.

## Not verified live

The **empty state** could not be exercised: this tenant has tasks and types, so no collection is
empty. The component and its CSS are in, and the unit tests cover the markup, but nobody has seen
it render. Stated rather than glossed.
