# Feature — Form widgets and states (design screens 08, 13, 16, 19)

**ID:** features/019-form-widgets · **US:** US-7.1 · **Version:** v1
**Status:** Approved — standing authorisation in chat 2026-08-27.
**Date:** 2026-08-27 · **Project:** `notebox-web`

## Origin
US-7.1 / FR-19 / NFR-09 · conformance report **§C-3 and §C-4** — the widgets that carry behaviour
rather than styling: `af-spinButtons`, `af-choiceGroup`, and the task-detail notes drawer
(`af-drawer` / `af-drawerDock` / `af-drawerTab` / `af-note`).

## Summary
Number fields become ADF **spin boxes** whose buttons respect the field's own bounds; choice fields
adopt the ADF **choice group**; the task-detail screen gains the handoff's **notes drawer**, closed by
default and opened from a dock tab. The rich-text toolbar's separators and the type builder's *Fields*
sub-header shipped with feat-018, which owns the toolbar cluster.

## Scope
- **In:** spin buttons with clamping behaviour; `af-choiceGroup`; the notes drawer and its dock.
- **Out:** `af-comboField` beyond the login screen — the remaining screens the report lists for it
  (04b, 13, 16) use it for date pickers and LOVs the app does not have, so wrapping a plain input in
  a combo shell would add an affordance with nothing behind it. **`af-noteWindow` and `af-messages`**
  are deferred with it: both exist to present *server-side* violation detail, and the record form
  already renders violations through the shipped `nb-msg` path — replacing that is a behaviour change,
  not a conformance one. Recorded as deviations rather than silently dropped. `af-shuttle` stays
  deferred (no bulk API).

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-19 — form widgets that carry behaviour

  Scenario: A number field steps with its spin buttons
    Given a number field showing 4
    When the member presses the increase button
    Then the field's value becomes 5

  Scenario: A spinner never leaves the field's declared bounds
    Given a number field bounded 1..10 showing 10
    When the member presses the increase button
    Then the value stays 10

  Scenario: A spinner starts somewhere valid when the field is empty
    Given a number field bounded 1..10 with no value
    When the member presses the increase button
    Then the value becomes 1

  Scenario: The notes drawer is closed until its dock tab is used
    Given the member opens a task's detail
    Then a notes dock tab is shown and no drawer panel is open
    When the member activates the dock tab
    Then the drawer opens

  Scenario: The drawer states that notes are not stored
    Given the notes drawer is open
    Then it says notes are not stored yet
```

## Compliance pre-flight
- [x] **C-01 / C-02 / C-06** — applies (standing): guarded screens, no new fetch path, no new transport.
- [ ] **C-03 / C-04 / C-05 / C-10 / C-11 / C-12** — n/a.
- [x] **C-07 · Image upload safety** — **applies (unchanged).** The image field's shipped validation is
  untouched by this feature.
- [x] **C-08 · Rich-text sanitization** — **applies as a prohibition.** No `dangerouslySetInnerHTML`
  introduced; the drawer renders plain catalog text.
- [x] **C-09 · Localization completeness** — **applies.** Spin-button labels and every drawer string
  in en + pt.

## Open Questions
None blocking. The deferrals above are recorded deviations, not questions: each names why the widget
would be an empty affordance in this app today.
