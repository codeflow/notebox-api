# Audit — Form widgets and states

**ID:** features/019-form-widgets · **Round:** 1 · **Date:** 2026-08-27
**Verify:** `npm run verify` → 70 files, **516 tests**, build compiled.

## Verdict — **PASS WITH FINDINGS** (two recorded deviations, no defects)

## Traceability
All 5 scenarios have a test that can fail. The clamping pair is the load-bearing one: a spinner that
ignored `numberMin`/`numberMax` would pass a naive "it increments" test and fail both of these.

## Scenario honesty
The empty-field test exists because `Number.parseFloat('') + 1` is `NaN`, which renders as an empty
input and looks like nothing happened — a failure that is invisible unless asserted. The drawer's copy
test pins *what it says*, not merely that a panel opened: an empty drawer would read as "no notes
written yet" rather than "this does not save", which is the honest distinction.

## Findings
### F-01 · deviation — `af-comboField` not extended past the login screen
The report lists it for screens 04b, 13 and 16. The handoff uses the combo shell where a picker or
LOV sits behind the trailing button; this app has neither on those screens. Wrapping a plain input
would add a button-shaped affordance with nothing behind it — the exact failure feat-017 avoided by
making its adornment inert and saying so. Recorded, not silently dropped.

### F-02 · deviation — `af-noteWindow` and `af-messages` not adopted
Both present *server-side* violation detail. The record form already renders violations through the
shipped `nb-msg` path; swapping it would be a behaviour change wearing a conformance label. Left for
a feature that owns validation presentation.

## Scope
Diff matches the plan's blast radius exactly. The 10 pre-existing `ValueField` tests and every
record-form suite passed unmodified.
