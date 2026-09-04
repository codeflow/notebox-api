# Feature — Completing a subtask from the grid

**ID:** features/030-subtask-completion-notebox-web
**User Story:** US-4.1
**Version:** v1
**Status:** Approved (human approval 2026-09-04, blanket)
**Date:** 2026-09-04

## Origin

- **User Story:** US-4.1 — *As a tenant member, I want tasks with subtasks whose completion drives the
  task's progress, so status reflects real work.*
- **FRs covered:** **FR-20** — the member-facing half. feat-029 made the API record and expose the
  moment; nothing a member sees changed. This is everything they see.
- **BRs bound:** **BR-07** (task dates derive from subtasks) — the dates this feature writes are the
  *subtask's*, and writing them moves the parent's derived span. **BR-08** (every user-facing string
  resolves in en and pt). **BR-06** is untouched: the percentage still follows the done flag.
- **Primary source:** direct human decisions, 2026-09-02 and 2026-09-03, recorded as **OQ-38** and
  restated here as the four rules below. The PRD is silent on all of it.
- **Consumes:** feat-029's read model (`completedAt` on a subtask) and its rejection of a
  client-supplied moment; the panel's `askConfirm` (INV-P4), which since feat-023 renders a
  confirmation **in place of** the panel body rather than stacked over it.

## Summary

Ticking **Done** on a subtask that has no planned dates currently succeeds silently, leaving a
completed subtask with no timeline at all — which is what the member reported. This feature makes the
tick ask for what it needs, fill in what it can infer, and say when work landed after its plan.

The four rules, as the product owner stated them:

1. **No start date** → ask whether today should be the start. On confirm, **start and end both become
   today**. On **cancel, the tick reverts** — the question is a condition of completing, not an extra.
2. **Only the end date missing** → fill it with today, without asking.
3. **The subtask was completed after its planned end date** → show an indicator beside the end date,
   with a localized tooltip.
4. A subtask that carries **no recorded moment** — completed before FR-20 existed — is never shown as
   late. There is nothing to compare, and inventing a verdict is worse than showing none.

## Scope

**In**

- The tick's three paths: both dates missing (ask), end missing (fill), both present (unchanged).
- Reverting the checkbox when the member cancels.
- The late indicator and its tooltip, in both locales, driven by `completedAt` vs the planned end.
- Immediate feedback when a subtask form's start date is after its end date — today the member only
  learns on save, from the server.

**Out**

- **Any date arithmetic the server owns.** The task's derived span is recomputed by the API; this
  feature writes subtask dates and re-renders what comes back.
- **A "late" flag from the API.** feat-029 deliberately left the comparison to the client; this
  feature performs it. Changing that is a feat-029 change.
- **Backfilling.** Rule 4 is a display rule, not a repair.
- **The task-level Done.** Tasks have no done flag; status is derived (BR-06).

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-20 — completing a subtask from the grid

  Scenario: Ticking Done with no dates asks before completing
    Given a subtask that is not done and has neither a start nor an end date
    When the member ticks Done
    Then a confirmation asks whether today should be the start date
    And the subtask is not yet marked done

  Scenario: Confirming sets both dates to today and completes
    Given the confirmation from ticking Done on a subtask with no dates
    When the member confirms
    Then the subtask is marked done
    And its start date and its end date are both today

  Scenario: Cancelling reverts the tick
    Given the confirmation from ticking Done on a subtask with no dates
    When the member cancels
    Then the subtask is not done
    And it still has neither a start nor an end date
    And the checkbox is unticked

  Scenario: Ticking Done with only the end date missing fills it with today
    Given a subtask that is not done, with a start date and no end date
    When the member ticks Done
    Then no confirmation is shown
    And the subtask is marked done
    And its end date is today
    And its start date is unchanged

  Scenario: Ticking Done with both dates present changes only the flag
    Given a subtask that is not done, with a start date and an end date
    When the member ticks Done
    Then no confirmation is shown
    And the subtask is marked done
    And both dates are unchanged

  Scenario: A subtask completed after its planned end is marked late
    Given a subtask that is done, whose completion moment is after its planned end date
    When the subtasks grid is shown
    Then an indicator appears beside its end date
    And the indicator's tooltip says the work finished after the planned date

  Scenario: A subtask completed on or before its planned end is not marked late
    Given a subtask that is done, whose completion moment is on or before its planned end date
    When the subtasks grid is shown
    Then no late indicator appears beside its end date

  Scenario: A subtask completed before this feature existed is never marked late
    Given a subtask that is done, with a planned end date in the past and no recorded completion moment
    When the subtasks grid is shown
    Then no late indicator appears beside its end date

  Scenario: A subtask with no planned end date is never marked late
    Given a subtask that is done, with a recorded completion moment and no end date
    When the subtasks grid is shown
    Then no late indicator appears beside its end date

  Scenario: Un-ticking Done removes the late indicator
    Given a subtask that is done and marked late
    When the member unticks Done
    Then the subtask is not done
    And no late indicator appears beside its end date

  Scenario: The start date cannot be set after the end date, before saving
    Given the subtask form with an end date of 01-Sep-2026
    When the member enters a start date of 10-Sep-2026
    Then a message says the start date must not be after the end date
    And the Save button does not send the form

  Scenario: Every string this feature adds resolves in Portuguese
    Given the interface language is Portuguese
    When the confirmation, the late tooltip and the date-order message are shown
    Then each renders its Portuguese text
    And no raw message key or empty string appears
```

**Boundaries pinned above, and why**

| Boundary | Why it is a scenario |
|---|---|
| Cancel reverts | The product owner's explicit answer. Without it the tick could land with no dates — the exact state this feature exists to prevent. |
| No moment, past end date | The trap of rule 3. `done && endDate < today` would mark this late; only `completedAt` distinguishes it. Every subtask that exists today is in this population. |
| No end date | Nothing to be late against. |
| Un-tick removes it | The indicator follows the moment, and feat-029 erases the moment on un-completion. |
| Both dates present | The common path must stay a one-field change. |

## Compliance pre-flight

Compliance items are the API's constitution; this satellite inherits the ones that reach the client.

- [ ] **C-01 · Tenant isolation** — **n/a here, standing at the API.** The client sends no tenant and
  chooses no scope; every request is authorized server-side (feat-029's cross-tenant evidence).
- [x] **C-02 · Authenticated by default** — **applies (standing).** These calls travel `authFetch`,
  which carries the session token and handles a 401 by returning the member to sign-in. No new
  unauthenticated surface.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** Ordinary tenant membership.
- [ ] **C-04 · Personal data minimization** — **n/a.** No identity data is collected or displayed. The
  moment is attributable to no person — see feat-029's C-04, and its tripwire: this flips the day a
  subtask gains an assignee.
- [ ] **C-05 · Secrets never committed** — **n/a.**
- [x] **C-06 · Encryption in transit** — **applies (standing).** Existing TLS ingress; no new transport.
- [ ] **C-07 · Image upload safety** — **n/a.**
- [ ] **C-08 · Rich-text sanitization** — **n/a for new work; standing otherwise.**
- [x] **C-09 · Localization completeness** — **applies.** Three new user-facing strings at least: the
  confirmation, the late tooltip, and the client-side date-order message. *Evidence:* the Portuguese
  scenario above, plus entries in both catalogs. **The catalog cannot help here:** OQ-37 records that
  the message catalog reaches the 93 server messages and **none** of the 425 interface strings, so
  these are compile-time keys with no runtime override — the pt-locale test is the only guard.
- [ ] **C-10 · Audit trail** — **n/a.** No irreversible or administrative action; un-ticking reverses.
- [ ] **C-11 · Data retention** — **n/a.**
- [ ] **C-12 · Encryption at rest** — **n/a.**

## Design conformance (FR-19, NFR-09)

The confirmation uses the panel's own `askConfirm`, which since the 2026-09-02 correction renders the
product's `af-dialog` chrome — title bar, `dlgBody`, `dlgFoot` — **in place of** the panel body rather
than stacked over it (INV-P4). The late indicator is a marker beside a grid cell; the handoff has no
drawn precedent for it, so its treatment is a **recorded divergence** to settle at the approval gate,
not an invention to discover at review.

## Open Questions

- **OQ-41 — What does the late indicator look like?** *Open, non-blocking.* The handoff draws no
  marker of this kind in a datagrid cell. Proposal: reuse the severity glyph already used by
  `SeverityMessage` at the size of the row's action icons, beside the end date, with the tooltip as
  its accessible name. Blocks nothing; the plan may proceed and the gate can overturn the treatment.
- No other open question. OQ-38 (persist the moment, clear it on un-completion) is resolved and
  folded in; OQ-37 is cited above as the reason C-09's evidence rests on a test rather than a catalog.
