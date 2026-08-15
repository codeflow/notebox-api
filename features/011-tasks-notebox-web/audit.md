# Audit — Tasks & subtasks UI (feat-011, US-4.1, notebox-web)

**Date:** 2026-08-14 · **Auditor:** session (opus·xhigh discipline) + independent adversarial
sub-agent with fresh context for the author-bias-exposed checks (traceability, scenario honesty).
**Tree audited:** commits `dceeee7`/`925cf63`/`d6e1ce4` (T-01..T-03) + T-04 working tree.
**Method:** all six mandated checks ran explicitly; the sub-agent read every spec scenario, every
production file and every test body, then re-ran the suite — **51 files, 353 tests, 0 failures**.
The session additionally ran a **live pass** against the real API + MySQL (seeded tenant/user):
login → nav chrome → empty state → create → subtasks with/without dates → checkbox tick →
**50% from the API's real in-transaction recompute** → list view. Screenshot taken; wire log shows
the POST 201 + PUT 200 sequence with parent-returning bodies.

## Check 1 — Traceability: PASS
All 18 scenarios map to tests whose bodies genuinely exercise them — MSW at the fetch layer, no
mocked system-under-test (only navigation/auth context). S16 (guard) reuses `RouteGuard.test.tsx`
as the plan declared; verified the guard is global default-deny mounted above the `(app)` tree, so
the coverage transfers structurally, and the redirect assertion is concrete.

## Check 2 — Scenario honesty: PASS with findings
The strong points are genuinely strong: no-status-key tests assert the **serialized** body (and
`status?: never` exists in the types); the full-echo tick test deep-equals all four fields; the
panel repaint tests use **non-derivable fixtures** (returned status ≠ what client math would say),
so any client-side percent computation fails them; date columns are position-pinned twice; pt
assertions use exact catalog strings; the layout smoke renders the real production layout. Grep
confirms zero percent math in the tree (the only division is the pager's page count). Three
strength gaps survived — findings 1–3.

## Check 3 — Constitution / house rules: PASS
`adf-fusion.css` untouched (all new CSS in overrides, including the `.af-panelBox` flex-trap pin);
no server rule re-implemented client-side (forms are `noValidate`, every message rendered is the
server's or the catalog's); BR-06 client obligation held structurally (`status?: never` +
serialization locks + verbatim `%` rendering); BR-05 confirms present on both delete paths with
the subtasks-go-too warning; AD-06 consumer posture intact (the client renders, the API decides).

## Check 4 — Compliance: PASS
C-01/C-02 (applies): all calls through the shipped `authFetch`/guard seam — no new fetch path, no
tenant identifier ever sent; guard coverage verified. C-06 standing. C-09 (applies): **52/52 keys
in both catalogs, keysets identical** (compile-typed `Record<MessageKey, string>` + runtime parity
test + exact-pt assertions). C-08 n/a — no rich text in the slice (grep-clean). Others n/a as the
spec recorded.

## Check 5 — Scope: PASS (deviations recorded)
Diff vs `develop` = exactly the plan's enumerated blast radius (the plan prose's "25 created"
miscounted its own list of 24 — the enumeration is the authority; all present, nothing extra) plus
one file: `TypeBuilderForm.test.tsx` — the pre-existing flake the human explicitly asked to fix
mid-feature, committed separately (`aef1d82`) with its own message. Recorded deviations from the
task plan: `tasks.notFound` key pulled forward T-04→T-03 (edit route needed it), noted in
tasks.md at the time.

## Check 6 — Open Questions: PASS
None opened during implementation; none closed by implementer assumption. OQ-21/OQ-22 are consumed
as display facts, as the spec directs.

## Findings (ranked)

1. **Important — the page-level repaint joint is untested.** The panel proves it lifts the
   returned parent; the page proves it renders `task.status` on load; but no test renders
   `TaskDetailPage` and performs a subtask mutation, so the one line joining them
   (`onTaskUpdated={(updated) => setState({status:'ready', task: updated})}`) has no automated
   protection. *Concrete failure:* replace it with a no-op — all 353 tests stay green while
   ticking a checkbox never moves the progress bar. The live pass proved the shipped wiring
   correct in the real browser; the gap is regression protection. The S11 untick panel assertion
   is also the weakest of the set (`toHaveBeenCalledTimes` only).
2. **Important — S5's no-default guarantee pins the test's own initial, not the production
   page's.** A future `priority: 'MEDIUM'` seed in `new/page.tsx` would pass the whole suite
   while violating the spec's deliberate-choice rule. One `toHaveValue('')` in the integration
   create test closes it.
3. **Important — the page-size control is undriven.** The size select (50/100/200) resets page
   and refetches; no test touches it. Dropping the `setPage(0)` (empty page 2 at size 200) or
   disconnecting the select stays green. Shipped-beyond-spec behavior with zero coverage.
4. **Observation** — S18's "every system string" is sampled (detail in pt; form/nav exact-pt
   separately); the list surface never renders under pt. Structurally mitigated by the total
   `Record<MessageKey, string>` type + keyset parity test.
5. **Observation** — S4's letter says the list shows the new task at 0%; the shipped flow
   navigates to the fresh detail instead, as `contracts/interfaces.md` prescribes — the contract
   arbitrates. Covered transitively (fetch-fresh + render-as-served).

## Verdict: **PASS with findings**

No blocker: intent, artifacts, code and the live behavior agree. Findings 1–3 are additive
test-strength gaps (three small test methods, no production change) logged for hardening before
the PR or as backlog — the human decides; 4–5 need no action. Gate `audit_pass`: **open**.

**Post-verdict hardening (same day, human-approved):** findings 1–3 closed — an integration test
ticks on the rendered detail page and asserts the bar repaints to a **non-derivable** 83% (closes
1; the panel's untick assertion also strengthened to `toHaveBeenCalledWith`); the integration
create flow now pins the production page's priority initial to `''` (closes 2); a pager test
drives the size select to 200 and asserts the page-0 reset refetch (closes 3). Verify green:
**355 tests**. Production code needed no change.
