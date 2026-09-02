# feat-024 — audit

**Model:** opus · **Effort:** xhigh · **Date:** 2026-09-01 · **Verdict: PASS with findings**

`verify` green at 718 tests. Two findings, both non-blocking and both logged below rather than
fixed inside the audit.

---

## What was checked, and what it cost to check it

### Traceability

All nine scenarios cite a test that exists and fails when the behaviour is removed. The three
probes named in `tasks.md` were run and each killed only its own assertion.

### Scenario honesty — the one that matters here

`fieldRowInput`'s suite is the load-bearing one, and it is honest: the probe (build the body from
the edited field alone) kills **all six** of its tests, not one. That is the right shape for a
function whose entire job is what it carries through.

The C-03 test is stronger than the spec asked for. The first draft asserted that an administrator
could not promote *themselves*; reading `UserResource` showed there is no role endpoint at all, so
the test now asserts that **no control anywhere carries the role column's name**. An absent
capability beats a hidden control, and the test says which one it is relying on.

### Constitution and compliance

| Item | Evidence |
|---|---|
| **C-02** | Unchanged — every save goes through the existing clients. |
| **C-03** | `canAdminister` gates the edit control; `isSelf` removes the status control; role has no control at all. Three tests, one per clause. |
| **C-09** | Six new keys, all present in **both** catalogs (checked by name), keyset guard green. |
| **C-12** | The `secret` flip is carried to the server and its refusal keeps the row in edit with the flag as set. The grid renders no stored value — asserted. |
| **BR-05** | Untouched: this feature adds no destructive action. Group delete still confirms through the panel. |
| **BR-09** | `visibleForViewing` is edited as presentation. Nothing here reads it as access control. |

### Scope

Two files outside the plan's blast radius: `components/grids/useFrozenColumns.tsx` and
`components/grids/gridConventions.test.ts`. Both are the live pass's F-01 fix, which the plan could
not have named because the defect was not known when it was written. Recorded rather than waved
through.

---

## Findings

### F-01 · The frozen-column fix was private for a whole feature — **fixed, and generalised**

Not a defect in this feature's code; a defect in how feat-023's fix was placed. It lived inside
`RecordsGrid`, so the three grids added here inherited the reflow (measured: 298px → 417px on the
type's field grid). Extracted to `useFrozenColumns` and applied to all four, with a guard
(`gridConventions.test.ts`) that fails when a grid uses `useRowEditor` without it. The probe
confirms the guard bites.

**This is the third time in two days** that a fix for a class of things was applied to one member
of it — the severity icon, the panel's discard guard, and now this. The pattern is worth naming:
each time, the fix was written *inside the caller that needed it first*.

### F-02 · A members row's partial save leaves the grid showing a stale name

**Severity: low. Logged, not fixed.**

A row's confirm can issue two requests (`PATCH` for the name, `PUT /active` for the status). If the
first succeeds and the second is refused, the name **is** saved while the row stays in edit showing
the error — and the list behind it is not reloaded, so cancelling returns the row to its old name
until the next fetch.

Nothing is lost and nothing is wrong on the server. It is a display staleness in a two-request
path the API's shape forced. Fixing it well means either a combined endpoint (an API feature) or
reloading between the two calls (a request the member did not ask for). **Recorded as a follow-up.**

### F-03 · `TranslationsTable` edits inline and does not freeze its columns

**Severity: informational.** It is out of this feature's scope by the spec (it already edited
inline, and it is the pattern the others were copied from), and `gridConventions.test.ts` does not
catch it because it uses its own editor rather than `useRowEditor`. Worth aligning when that screen
is next touched; not worth reopening this feature for.

---

## Verdict

**pass with findings.** F-01 was found by leaving the desk, as the last two features' real defects
were. F-02 and F-03 are logged for the backlog and named here so they are not rediscovered as
surprises.
