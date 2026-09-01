# Feature — The side panel as the primary surface for records

**ID:** features/023-records-panel
**User Story:** US-2.2
**Version:** v1
**Status:** Draft — **carries assumptions, see §Assumptions**
**Date:** 2026-09-01

## Origin
- **User Story:** US-2.2 (a listing exposing visible fields, a detail exposing all fields)
- **FRs covered:** FR-05 (the listing), FR-04/FR-06 (editing a record), FR-09 (the tree, which
  loses its Administration branch as a consequence)
- **BRs bound:** **BR-05** (destructive actions need confirmation), **BR-09** (visible-for-viewing
  is presentation, never access control), **BR-10** (secret values)
- **Primary source:** **OQ-35**, raised by the product owner during the 2026-08-31 visual collect
  and grown by them across nine separate notes.

> ### ⚠ This spec was written without the product owner answering OQ-35's open questions.
>
> They asked for the work to run to completion without interruption. Six decisions in OQ-35 are
> genuinely theirs, so every one of them is resolved here **as a stated assumption**, with the
> alternative that was rejected and why. They are collected in §Assumptions, and each is marked
> `[A-n]` where it drives a scenario.
>
> **Assumptions are cheap to correct in a spec and expensive to correct in code.** Read that
> section first.

## Summary

The tree indexes records; the grids list them; and today every act of looking at one or changing
one throws the member onto a different screen. This feature makes a **side panel** the place where
a record is read and written, opened from wherever the member already is — a grid's action icon, a
menu item, an empty state's button — and makes the **grids editable in place** for the fields a
listing already shows.

It is not an addition to the screens. Six existing screens **move into** the panel, and the tree
loses a branch.

## Scope

**In:**

- **Inline editing** in the data grids, for `visibleForViewing` fields only, copying the pattern
  the message catalog already ships (enter-edit → confirm/cancel, per row).
- **An expandable side panel** built on the existing `af-drawer`, holding: a record's detail, a
  record's full edit form, the new-task form, Groups, Members, and the message catalog.
- **One way to open it.** The same destination is reachable from a grid icon, a menu item and an
  empty-state button; all three invoke one thing.
- **The dialog-inside-a-panel rule** — one answer for confirmations, one for creation forms.
- **The Administration branch leaves the Navigator**, which becomes content-only.
- The Actions column's **four-icon vocabulary**: enter-edit, confirm-edit, cancel-edit, delete-row,
  each with a localized tooltip, and cancel-edit visually distinct from delete.

**Out:**

- **The tasks grid and the subtasks grid do not edit inline** — named by the product owner. They
  open the panel for editing instead.
- The records **band** inside the types list stays read-only (feat-020's decision, unchanged).
- No API change. No new endpoint, no new DTO field.
- `Preferences` and `Help` remain inert — they belong to no feature yet.

## Assumptions

Each of these is **mine, not the product owner's**. Each names what I rejected.

| # | Question (OQ-35) | Assumed answer | Rejected, and why |
|---|---|---|---|
| **A-1** | What starts an inline edit? | **An explicit edit icon in the Actions column.** | *Click-the-cell* — it makes every accidental click a state change, and in a grid whose first column is a navigation link the two gestures collide. The catalog's existing pattern is an icon, and copying it keeps one behaviour. |
| **A-2** | Save granularity, and failure mid-row? | **Per row, on confirm.** A failed save keeps the row in edit with its values and shows the server's message; nothing is partially written. | *Per field on blur* — it multiplies requests, and a row with one field saved and another rejected has no honest state to render. |
| **A-3** | Does BR-05's confirmation apply to an inline edit? | **No for editing, yes for deleting.** An edit is reversible by editing again; the row-delete icon always confirms. | Confirming every edit would train the member to dismiss confirmations, which is how the one that matters gets dismissed too. |
| **A-4** | Secret fields in an inline editor? | **A Secret field is never editable inline and never revealed there.** It renders as the mask, exactly as it does today; changing it needs the full form in the panel. | Anything else risks making a listing a reveal surface — **C-12 is compliance, not taste**, so this takes the restrictive reading deliberately. |
| **A-5** | What does "expand" mean for `af-drawer`? | **Two widths.** Normal (a reading column) and expanded (most of the viewport, for the rich-text editor and wide grids), toggled by a control in the panel's own header, remembered for the session. | A full-screen mode — it stops being a panel and becomes the screen the feature is removing. |
| **A-6** | A dialog inside the panel? | **Two answers, because the cases differ.** A *confirmation* is transient: it replaces the panel's content, offering back/confirm. A *creation form* holds unsaved input: it opens as its own panel content, and closing it asks before discarding. | One answer for both — replacing the panel's content under a half-typed form loses work, which is exactly the risk the product owner named. |
| **A-7** | Why are tasks/subtasks exceptions? | **Because a task's fields do not fit a row** — priority, dates, card, subtask progress. The rule generalises: *a grid edits inline when its listed fields ARE its editable fields; otherwise it opens the panel.* | Treating them as arbitrary exceptions — a rule nobody can restate is a rule that gets applied wrong next time. |

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-05 — a record is edited where it is listed

  Scenario: Editing a visible field in place
    Given a grid listing a type's records
    When the member activates the edit icon on a row
    Then that row's visible fields become editable in place
    And the row offers confirm and cancel controls

  Scenario: Confirming writes the row once
    Given a row in edit with a changed value
    When the member confirms
    Then exactly one update request is issued for that record
    And the row returns to its read state showing the new value

  Scenario: A failed save keeps the member's work    # [A-2]
    Given a row in edit whose update will be rejected
    When the member confirms
    Then the row stays in edit with the values the member typed
    And the server's message is shown
    And no part of the row has been written

  Scenario: Cancelling discards, and says nothing was written
    Given a row in edit with a changed value
    When the member cancels
    Then the row returns to its read state showing the ORIGINAL value

  # A-4 — the compliance boundary, and the strictest scenario here
  Scenario: A Secret field is not editable in a grid
    Given a type with a Secret field marked visible for viewing
    And a grid listing that type's records
    When the member activates the edit icon on a row
    Then the Secret field shows its mask and is not editable
    And no reveal request is issued

  # A-3
  Scenario: Deleting from a row asks first
    Given a grid row
    When the member activates the delete icon
    Then a confirmation is shown naming what will be deleted
    And nothing is deleted until it is confirmed

  # A-7 — the exceptions, stated as a rule rather than a list
  Scenario Outline: A grid whose listed fields are not its editable fields opens the panel
    Given the <grid>
    When the member activates the edit icon on a row
    Then the panel opens with that row's full form
    And the row does not become editable in place
    Examples:
      | grid          |
      | tasks grid    |

      | subtasks grid |

  # AMENDMENT WITHDRAWN 2026-09-01. The amendment that stood here removed the subtasks grid from
  # this outline, arguing that A-7's rule pointed the other way for it and that feat-010's spec S13
  # already specced its row editor.
  #
  # **It was wrong, and wrong in the way that matters most here.** The product owner had already
  # said it, in their own words, in the visual-pass brief (item 14):
  #
  #   > "e nesse grid a edição também abre um popup"
  #
  # A direct human decision ranks with the PRD in this project's source hierarchy — above
  # precedent, and far above a rule the implementer derived from the spec's own prose. The
  # amendment inverted that order: it used my reading of A-7 to overrule what was asked for.
  #
  # The example is restored and the behaviour built (T-11). feat-010's spec S13 is superseded for
  # the *entry point* only — the subtask's fields, its validation and its server-violation matrix
  # are untouched; they moved into the panel, they did not change.

Feature: FR-05 — the panel is where a record is read and written

  Scenario: One destination, many callers
    Given the new-task form is reachable from the Workspace menu, a grid icon and an empty state
    When the member opens it from any of the three
    Then the same panel content is shown

  Scenario: Expanding the panel        # [A-5]
    Given the panel is open at its normal width
    When the member activates the expand control
    Then the panel widens
    And its content remains the same
    When the member activates it again
    Then the panel returns to its normal width

  # A-6, case one: a confirmation is transient
  Scenario: Confirming a destructive action inside the panel
    Given Groups is open in the panel
    When the member deletes a group
    Then the panel's content is replaced by the confirmation
    And the confirmation offers going back without deleting
    When the member goes back
    Then Groups is shown again, unchanged

  # A-6, case two: a creation form holds work
  Scenario: Closing a creation form with unsaved input
    Given the new-task form is open in the panel
    And the member has typed a name
    When the member closes the panel
    Then they are asked before the input is discarded

  Scenario: Closing a creation form with nothing typed
    Given the new-task form is open in the panel
    And the member has typed nothing
    When the member closes the panel
    Then the panel closes without asking

Feature: FR-09 — the tree indexes content, and only content

  Scenario: Administration leaves the tree
    Given the member is on an administration screen
    When the Navigator renders
    Then it shows no Administration branch
    And Groups, Members and the message catalog are reachable from the Administration menu
```

## Measurability notes

- *"exactly one update request"* is a **count**, asserted on the request.
- *"no reveal request is issued"* is asserted on the reveal client being untouched — the mistake
  feat-020's audit caught was asserting this against the wrong client, so it is named here.
- *"the panel widens"* is asserted on the measured width in a browser, not on a class name.
- *"asked before the input is discarded"* is asserted on the prompt appearing AND on the input
  surviving a cancel.

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-01 Tenant isolation** | **not applicable** | No new data path; the panel calls the endpoints the screens it replaces already called. |
| **C-02 Authenticated by default** | **applies** | Every call still goes through `authFetch`. Evidence: an expired session inside the panel takes the app-wide path, as feat-020 asserted for the band. |
| **C-03 Least-privilege** | **applies** | Members and the message catalog are administration surfaces; moving them into a panel must not change who may open them. Evidence: the route guard still governs. |
| **C-04 Personal data minimization** | **not applicable** | Renders nothing new. |
| **C-05 Secrets never committed** | **not applicable** | — |
| **C-06 Encryption in transit** | **not applicable** | — |
| **C-07 Image upload safety** | **applies** | The record form moves into the panel and it uploads images. The existing pre-validation must come with it. |
| **C-08 Rich-text sanitization** | **applies** | The rich-text editor moves into the panel. It must keep the same sanitize path — a new host must not become a second, unsanitized one. |
| **C-09 Localization** | **applies** | Four action tooltips, the expand control, and the discard prompt. Evidence: the keyset guard plus a locale outline. |
| **C-10 Audit trail** | **not applicable** | Server-side and unchanged. |
| **C-11 Retention & deletion** | **not applicable** | — |
| **C-12 Encryption at rest for secret values** | **applies — the strictest item here** | Inline editing puts an editor next to a masked value for the first time. **[A-4]** forbids it. Evidence: the Secret scenario above, asserted against `annotationRecordsClient.reveal` — the client feat-020's audit found being asserted wrongly. |

> **Accessibility**: the panel is a region with a name; opening it moves focus into it; Escape
> closes it under the same discard rule as the close control; the four row icons keep the
> localized accessible names `RowActions` already gives them.

> **NFR-09**: design screen 16 draws the notes drawer, not this. **Recorded deviation** — the panel
> generalises that drawer into the primary surface, which the handoff does not depict.

## Risks

| # | Risk | Why it is real |
|---|---|---|
| R1 | An inline editor appearing over a Secret value | The one compliance failure this feature can cause. A-4 forbids it; a scenario asserts it. |
| R2 | Unsaved input lost when the panel changes content | The product owner named it. Six surfaces move into one panel, and three of them hold typed input. |
| R3 | Three callers drifting into three implementations | The same destination is opened from a menu, a grid and an empty state. |
| R4 | The confirmation rule improvised per screen | Every destructive action in the panel meets it; decided once in A-6. |
| R5 | The tree losing Administration before the panel exists | Removing the branch while the menu still routes to full screens leaves the reason untrue. Sequencing matters. |

## Open Questions

- **OQ-35** — the source. **Its six questions are answered here as assumptions [A-1..A-7], not as
  the product owner's decisions.** The OQ should be updated with whatever they confirm or correct.
- No `[TBD]` remains, but §Assumptions is the list of things a reader must check before trusting
  this spec.
