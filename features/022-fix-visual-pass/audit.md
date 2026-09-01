# Audit — feat-022 fix-visual-pass

**This feature took the fix lane** (`/wf-fix`), so its documentation lives with the brief it was
built from rather than as spec/plan/tasks:

- **Brief** (what the human asked for, verbatim, 26 items): `fixes/2026-08-31-visual-pass/brief.md`
- **Record** (what was done, the evidence, what was deferred): `fixes/2026-08-31-visual-pass/record.md`
- **Audit** (the full report): `fixes/2026-08-31-visual-pass/audit.md`

## Verdict — PASS with findings

24 of 26 brief items accounted for: delivered, routed to OQ-35 with a reason, or declined with a
reason. 33 files changed, every one named by a brief item. 8 new i18n keys, 8 in each catalog.
`npm run verify` exit 0 — **643 tests** on the branch rebased over `develop` (which now carries
feat-021 and the build chore), because the audit's own F-04 said not to trust either side's green
after the merge.

Four findings, none a defect in the delivered code:

1. **Icon-only controls are a real usability trade** the brief asked for — names live in
   `title`/`aria-label`, so localization and assistive technology hold, but a sighted newcomer now
   reads glyphs. Recorded so the cause is findable if complaints follow.
2. **The tasks grid's trash routes to a confirmation screen, not a dialog** — BR-05 needs a
   confirmation and building one for the grid is OQ-35's scope. The icon promises more than it
   does; revisit when OQ-35 defines the in-panel confirmation.
3. **The empty state has never been seen rendering** — no collection in this tenant is empty. The
   one visual item resting on jsdom alone, which is the tier this project has been burned by twice.
4. **This branch predated PRs #21 and #22** — resolved by rebasing onto the merged `develop` and
   re-running `verify` there.

> **Process note for `/wf-fix`:** the lane stores its documents under `fixes/<slug>/`, but
> `wf done` looks for `features/NNN-slug/audit.md`. This file bridges the two. The command should
> either write both locations or the pipeline should accept the lane's path — worth fixing in the
> template's copy of the command.
