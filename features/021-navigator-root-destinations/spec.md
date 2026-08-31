# Feature — Navigator root nodes navigate to their list screens

**ID:** features/021-navigator-root-destinations
**User Story:** US-3.1 (primary) · US-2.2 (the OQ-34 residues)
**Version:** v1
**Status:** Draft
**Date:** 2026-08-31

## Origin
- **User Stories:** US-3.1 (navigation tree) · US-2.2 (the records listing and its sub-grid)
- **FRs covered:** FR-09 (navigation-tree data and its presentation) · FR-05 (the listing whose
  screen the tree cannot currently reach, and whose band carries the two residues)
- **BRs bound:** none new. The tree remains presentation over data the API already scopes.
- **Primary source:** **OQ-33**, raised by the product owner from live use of feat-020 and decided
  the same day (option a); **OQ-34**, decided 2026-08-31 to fold feat-020's two residues here.
  Both rank with the PRD under the source hierarchy (an explicit human decision, recorded).

## Summary

The navigation tree indexes content but cannot reach the screens that list it. Its `Annotations`
root is inert text; its leaves jump straight to one type's records, skipping the types list —
which, since feat-020, is where the records sub-grid lives. This feature makes the two content
roots navigate to their list screens, exactly as the level-1 tabs already do, so the tree stops
being the one surface from which a member cannot reach a list.

It also closes the two residues feat-020's audit left behind (OQ-34): the band's link destination
gains the test it never had, and opening a row twice in one tick stops issuing two identical
requests.

## Scope

- **In:**
  - The tree's `Annotations` root navigates to the annotation-types list; its `Tasks` root
    navigates to the tasks list.
  - Expanding/collapsing stays a separate control from navigating — the twisty keeps its job.
  - The root reflects the current page when the member is already on that screen, derived from
    the URL like every other selected node (W-7 precedent).
  - Both roots' labels remain reachable and operable from the keyboard, and named in both locales.
  - **OQ-34a:** the band's "see all N records" control is asserted to land on that type's records
    screen — the destination itself, not merely the callback.
  - **OQ-34b:** two toggles of the same row delivered before the list re-renders issue exactly one
    records request.

- **Out:**
  - **The `Administration` root.** Its children are already destinations; whether the root itself
    should navigate is a separate product question, not assumed here.
  - **Changing what the type leaves point at.** OQ-33 considered and rejected option (c) — leaves
    keep going to that type's records screen.
  - Any change to the tree's data, its shape, its counts, or the endpoint that serves it.
  - Any change to the band's own behaviour beyond the request-count guard: no new columns, no
    pagination, no row actions.
  - Restyling the tree. The root becomes a control; it must look as it looks today.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-09 — the navigation tree reaches the screens it indexes

  Scenario: The Annotations root opens the annotation types list
    Given the member is on a screen where the tree shows the Annotations branch
    And the browser is not on the annotation types list
    When the member activates the tree's "Annotations" root label
    Then the browser is taken to the annotation types list

  Scenario: The Tasks root opens the tasks list
    Given the member is on a screen where the tree shows the Tasks branch
    When the member activates the tree's "Tasks" root label
    Then the browser is taken to the tasks list

  Scenario: A root already showing its screen reports itself as the current page
    Given the member is on the annotation types list
    When the tree renders
    Then the "Annotations" root is marked as the current page
    And the "Tasks" root is not

  # The two jobs must not collide — this is the regression the change most plausibly causes
  Scenario: Collapsing a branch does not navigate
    Given the member is on the tasks list
    And the tree's Tasks branch is expanded
    When the member activates that branch's expand/collapse control
    Then the branch collapses
    And no navigation occurs

  Scenario: Navigating does not collapse the branch
    Given the tree's Annotations branch is expanded
    When the member activates the "Annotations" root label
    Then the branch is still expanded

  # Boundary: the root's destination does not depend on having children
  Scenario: A root with no children still reaches its list
    Given a tenant with no annotation types
    When the member activates the "Annotations" root label
    Then the browser is taken to the annotation types list

  # Accessibility — owned by this satellite
  Scenario: The root destination is operable from the keyboard
    Given the tree has focus
    When the member tabs to the "Annotations" root label
    And activates it with the keyboard
    Then the browser is taken to the annotation types list

  # C-09
  Scenario Outline: Both root labels are named in both locales
    Given the interface locale is <locale>
    When the tree renders
    Then the "Annotations" and "Tasks" root controls each expose a non-empty accessible name
    And neither name is a raw message key
    Examples:
      | locale |
      | en     |
      | pt     |

Feature: FR-05 — the records band's two residues (OQ-34)

  Scenario: The band's link lands on that type's records screen
    Given a type with more records than its band shows
    And the member has expanded its row in the annotation types list
    When the member activates the band's "see all N records" control
    Then the browser is taken to that type's records screen

  # F-05 from feat-020's audit: measured at two identical requests, closed here
  Scenario: Two toggles in one tick fetch once
    Given a row in the annotation types list that has never been expanded
    When two toggles of that row are delivered before the list re-renders
    Then exactly one records request is issued for that type

  Scenario: A row toggled twice ends collapsed, and reopening still serves the cache
    Given a row that was toggled twice in one tick
    Then that row's band is not shown
    When the member expands it again
    Then no further records request is issued
```

## Measurability notes

- *"is taken to"* is asserted on the navigation actually performed with the destination path —
  the route string, not an intermediate callback. That is precisely the gap OQ-34a closes.
- *"exactly one records request"* is a **count**, asserted on the request, not on rendering.
- *"marked as the current page"* is the same assertion the tree already makes for its other
  selected node, so the two cannot drift apart.
- *"no navigation occurs"* is asserted on the navigation being performed zero times.

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-01 Tenant isolation** | **not applicable** | No new data path. The destination screens fetch what they already fetched, tenant-scoped server-side. |
| **C-02 Authenticated by default** | **not applicable** | No new caller and no new endpoint. Both destinations are existing authenticated routes behind the app's route guard; this feature only reaches them from one more place. |
| **C-03 Least-privilege authorization** | **not applicable** | No admin or config surface; the two destinations need only tenant membership, as today. |
| **C-04 Personal data minimization** | **not applicable** | Renders no personal data it does not already render. |
| **C-05 Secrets never committed** | **not applicable** | No credential or connection string. |
| **C-06 Encryption in transit** | **not applicable** | No deployment or transport change. |
| **C-07 Image upload safety** | **not applicable** | Uploads nothing. |
| **C-08 Rich-text sanitization** | **not applicable** | Renders no user-authored rich text; the labels are catalog strings. |
| **C-09 Localization completeness** | **applies** | The roots' labels become accessible names of controls. Evidence: the locale Scenario Outline above, plus the existing keyset coverage check. **No new key is expected** — the labels exist; the scenario exists to catch a change that invents one. |
| **C-10 Audit trail** | **not applicable** | Read-only navigation; nothing irreversible or administrative. |
| **C-11 Data retention & deletion** | **not applicable** | Manages no accounts or tenants. |
| **C-12 Encryption at rest for secret values** | **not applicable** | Renders no field values. |

> **Accessibility** is this satellite's own obligation: covered by the keyboard scenario, and by
> the roots keeping a real control's semantics rather than a clickable span.

> **NFR-09 (UI conformance)** — this is a **recorded deviation** from design screen 05, which
> draws the tree with inert root labels. Recorded in OQ-33 with the decision and its reason, the
> same way the Administration tab's removal was. It is not an omission and must not be
> "corrected" back by a future conformance pass.

## Risks

| # | Risk | Why it is real here |
|---|---|---|
| R1 | The twisty and the label fighting over the same click | They sit in the same row. A label that navigates while the twisty toggles is exactly where a stray handler produces "collapsing also navigates". Two scenarios above exist only to pin this. |
| R2 | The root looking different once it becomes a control | Buttons inherit browser chrome. The tree was measured to a settled layout; a control must not change the row's metrics. |
| R3 | The double-toggle guard breaking the ordinary open | A guard on in-flight requests can, done wrong, swallow the *second, legitimate* open after a collapse. The cache scenario above is what catches it. |
| R4 | Selection drifting from the URL | The tree derives selection from the pathname (W-7). A root that remembers its own "selected" state instead would disagree with the address bar after a back/forward. |

## Open Questions

- **OQ-33** — ✅ resolved 2026-08-31 (option a: root nodes navigate). This feature implements it.
- **OQ-34** — ✅ resolved 2026-08-31 (fold feat-020's two residues here). In scope above.
- None pending. No `[TBD]` remains in this spec.
