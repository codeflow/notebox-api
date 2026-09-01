# Audit — fix-visual-pass (feat-022)

**Date:** 2026-09-01 · **Agent:** opus/xhigh · **Diff:** `develop..1228c80` on `fix/visual-pass`
(33 files, +1113/−178) · **Harness:** `npm run verify` exit 0, **630 tests, 78 files**

> The fix lane skips spec/plan/tasks, so **the brief stands in for the spec** and this audit is run
> against it. A fast lane does not get a fast audit — this step is what catches what the missing
> spec would have caught.

## Verdict — **PASS with findings**

Nothing blocks. Four findings, none of them defects in the delivered code: two are scope
statements the human should see plainly, one is a coverage gap I chose deliberately, and one is a
process note about how this branch relates to the two PRs already open.

---

## 1. Coverage against the brief — 24 of 26 items

Every item is accounted for: delivered, routed to OQ-35 with a reason, or declined with a reason.
The full table lives in `record.md`; the audit's concern is that **nothing is silently missing**,
and nothing is.

**Declined, and I stand behind both:**

- **18.1 `Preferences`/`Help` should work** — there is nothing behind them and "should work" does
  not say what they do. Building either means inventing product. OQ-28 set the precedent for a
  dead affordance.
- **24.3 add a new locale** — new catalogs, a translation surface, a fallback rule. Feature work.

**Deferred with a technical reason:**

- **16, line numbers in the EDITOR's code block** — highlighting works and the numbering CSS
  already serves both sides, but the numbers come from wrapping each line in `.nb-codeLine`, done
  by hand on the read side. Inside the editor that needs a **ProseMirror decoration**: not CSS, and
  a real risk of breaking typing. Correctly out of this lane.

## 2. Scenario honesty — do the new tests bite?

Three probes were run during implementation and each killed exactly its own assertion:

| Probe | Result |
|---|---|
| Remove the icon resolve | the FieldIconEditor assertion fails |
| Restore `finally { setSaving(false) }` | the duplicate-task assertion fails, alone |
| Remove the editor's image resolve | the RichTextEditor assertion fails, alone |

**Visual changes carry no unit tests, on purpose** — jsdom computes no styles, so a test
"covering" a CSS change could not fail. They were verified by measurement in a browser instead,
and the numbers are in `record.md` (rule spans, error-message left/width, row heights, the folder
icon's drawn height, the band header's computed border).

## 3. Compliance

| Item | Verdict |
|---|---|
| **C-09 Localization** | **applies** — 8 new keys, and `git diff` confirms **8 added to each catalog**. The keyset guard and `translationNotCopy` both pass; the editor's `https://` placeholder was added to the identical-on-purpose allowlist as a URL sample, which is what it is. |
| **C-12 Secret values** | **applies, and worth stating** — the type detail screen gained a `Secret` column. It displays the FLAG, never a value, and no reveal path exists on that screen. The masking guarantee is untouched. |
| **C-04 / C-05** | **applies** to the Members and password dialogs, which the brief asked to restyle. Both keep `type="password"`; no "show password" affordance was added; the note explaining that no email is sent was not shortened. Recorded because a "make it prettier" pass is exactly where such a thing slips. |
| C-01, C-02, C-03, C-06, C-07, C-08, C-10, C-11 | **not applicable** — no new data path, endpoint, upload, rich-text rendering path, or admin action. |

## 4. Scope — diff vs the brief

33 files, **all of them named by a brief item**: the icon set, the two new components, the
stylesheet, the i18n catalogs, and the screens each item pointed at. `.gitignore` gained
`.next-verify/` because this branch left `develop` before PR #22 added it.

**Nothing rode along.** No unrelated fix, no drive-by refactor.

---

## Findings

### F-01 · Two accessible names disappeared from view, deliberately — verify the trade held

Every text control that became an icon (items 5.2, 13, 14, 20.2) now carries its name only in
`title` + `aria-label`, following the `RowActions` precedent. That is the correct pattern and the
names come from the catalog, so localization holds.

**But it is a real trade**: a sighted member who did not know the app now reads a row of glyphs.
The brief asked for exactly this, so it is not a defect — recorded so that if usability complaints
follow, the cause is known and the decision is findable rather than mysterious.

### F-02 · The tasks grid's delete does not delete — non-blocking, by design

`onDeleteTask` routes to the task's detail screen, where the existing confirmation lives, rather
than deleting from the row. BR-05 requires a confirmation and building one for the grid is OQ-35's
scope. **A member clicking the trash icon lands on a screen instead of a dialog**, which is not
what the icon promises. Worth revisiting when OQ-35 defines the in-panel confirmation.

### F-03 · The empty state was never seen rendering — coverage gap, stated not hidden

`EmptyState` ships with unit tests over its markup, but no browser has drawn it: this tenant has
tasks and types, so no collection is empty. Every other visual item in this pass was measured
live. **This one rests on jsdom alone**, which is precisely the tier this project has been burned
by twice.

### F-04 · This branch left `develop` before PRs #21 and #22 — process, not code

`fix/visual-pass` branches from `develop`, which does not yet carry feat-021 (PR #21) or the
build chore (PR #22). Consequences the human should know before merging anything:

- **Brief item 9.1** (the tree root's highlight) is untouched here **on purpose** — it belongs to
  feat-021's PR, and editing it from this branch would modify a feature still under review.
- Merge order matters little for conflicts (the files barely overlap), but whoever merges second
  should re-run `verify` on the merged result rather than trusting either branch's green.

---

## Gate

`audit_pass` — **open**.

> **The pipeline itself is blocked, and correctly**: `wf done` refuses to close this feature while
> its predecessor feat-021 is still `in_progress`, waiting on the human's approval of PR #21. That
> is the dependency doing its job. `--force` was NOT used. Publishing this fix needs PR #21
> resolved first.
