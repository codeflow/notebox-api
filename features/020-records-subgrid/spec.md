# Feature — Records sub-grid in the types list (af:table detailStamp)

**ID:** features/020-records-subgrid
**User Story:** US-2.2
**Version:** v1
**Status:** Draft
**Date:** 2026-08-30
**Project:** notebox-web (satellite)

## Origin

- **User Story:** US-2.2 — *"a listing exposing visible fields plus a detail view exposing all
  fields, so grid and detail have what they need"*
- **FRs covered:** FR-05 (list a type's annotations returning visible fields) — this feature adds
  no new listing rule; it renders the existing one in a second place. FR-19 / NFR-09 (UI
  conformance) are engaged as a **recorded deviation**, below.
- **BRs bound:** BR-09 (visible-for-viewing governs which fields a listing exposes)
- **Primary source:** product-owner decision 2026-08-30, recorded as **OQ-32** (resolved). The
  decision was taken against a mockup of two options built with the tenant's real data.

## Summary

A row of the annotation-types list can be expanded to reveal that type's own records, without
leaving the screen. The expanded region carries the record grid the type defines — its columns
are the type's visible fields — in the ADF Faces `af:table` **detailStamp** idiom: a disclosure
control in a leading column, and the detail rendered full-width beneath its row.

Today the list shows a `Records` count that leads nowhere; reaching the records costs two clicks
through an intermediate screen. This closes that distance for the common case of *"what is
inside this type?"* while leaving the full grid as the place to actually work.

## The deviation, stated plainly

The handoff draws screen **07** (types list) and screen **11** (annotations grid) as two separate
screens. Putting 11 inside 07 is a departure, and **NFR-09 measures conformance against the
handoff** — so this is an exception on the record, like the Administration tab before it, not an
omission for the audit to find.

**The cost the product owner accepted** (OQ-32, restated here so the audit does not relitigate it):

1. The column set changes per expanded row, because each type declares its own visible fields.
   Rows of the outer grid therefore stop being comparable with one another.
2. A type with no records still draws its header over an empty band.

A uniform-column preview avoiding both was offered and declined. That is the owner's call.

## Scope

**In**

- A disclosure control per row of the types list, with its expanded/collapsed state.
- The expanded band: that type's record grid, columns from its **visible** fields, plus the
  record name — the same column set `RecordsGrid` already derives.
- A footer line in the band naming the true total and linking to the full grid.
- The empty and failed states of the band.
- Localized strings for everything the band emits (en, pt).

**Out**

- Any change to the API. `GET /annotation-records?typeId&page&size` already serves this.
- Row actions inside the band (view/edit/delete). The band is for reading; acting on a record
  happens in the full grid. Adding them later is a separate decision.
- Pagination controls inside the band — see S4.
- Sorting or filtering within the band.
- The tasks list. This feature touches the annotation-types list only.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-05 — a type's records, read from the list that names the type

  Scenario: Expanding a row shows that type's records with that type's columns
    Given a type "Reunião de projeto" whose visible fields are "Assunto" and "Participantes"
    And it has 5 records
    When the member expands its row
    Then a band appears beneath that row and no other
    And the band's grid has the columns Name, Assunto and Participantes
    And it lists 5 records

  Scenario: Two types expanded at once keep their own columns
    Given a type "Contato" with visible fields "Nome" and "Telefone"
    And a type "Reunião de projeto" with visible fields "Assunto" and "Participantes"
    When the member expands both rows
    Then both bands are visible at the same time
    And each band's columns are its own type's visible fields, not the other's

  Scenario: A type with more records than the band shows says so, and links out
    Given a type with 47 records
    When the member expands its row
    Then the band lists exactly 10 records
    And the band shows a control naming the true total, "see all 47 records"
    And activating it opens that type's full records grid

  Scenario: A type with no records draws no empty table
    Given a type with 0 records
    When the member expands its row
    Then the band shows the grid's existing empty message
    And the band shows no column header row

  Scenario: Collapsing a row removes its band and leaves its neighbours alone
    Given two expanded rows
    When the member collapses the first
    Then the first band is gone
    And the second band is unchanged

  Scenario: The records are fetched when the row opens, not when the list loads
    Given a list of 12 types, none expanded
    When the list finishes loading
    Then no request for records has been issued
    When the member expands one row
    Then exactly one records request is issued, for that type, with size 10

  Scenario: Reopening a row does not refetch what it already has
    Given a row that has been expanded and then collapsed
    When the member expands it again
    Then no further records request is issued

  # Error path
  Scenario: A band whose records fail to load says so and stays dismissible
    Given the records request for a type will fail with a server error
    When the member expands its row
    Then the band shows the localized failure message, not a raw code or a blank band
    And the rest of the list is unaffected
    And the member can still collapse the row

  # Session boundary — the app-wide rule, asserted here because the band is a new caller
  Scenario: An expired session inside a band ends the session, not just the band
    Given the records request will answer 401
    When the member expands a row
    Then the session-expired path is taken, as for any other request

  # Keyboard — a requirement, not a preference (OQ-32 §4)
  Scenario: The disclosure is operable from the keyboard and announces its state
    Given the types list has focus
    When the member tabs to a row's disclosure control
    And activates it with the keyboard
    Then the band opens
    And the control reports aria-expanded="true"
    And the band's link to the full grid is reachable by tabbing onward

  # C-09
  Scenario Outline: Every string the band emits exists in both locales
    Given the interface locale is <locale>
    When a band is opened, is empty, and fails
    Then no raw message key and no blank label is rendered in any of the three states
    Examples:
      | locale |
      | en     |
      | pt     |
```

## Measurability notes

- *"exactly 10 records"* and *"size 10"* are literal: the request carries `size=10`, and the
  count is asserted on rendered rows. NFR-08's default of 50 is respected by asking for less,
  not by excepting it.
- *"naming the true total"* is asserted against `PageDto.total` from the response — not against
  the number of rows drawn, which is the whole point of the line.
- *"no request has been issued"* is asserted on the request count, not on absence of rendering.

## Compliance pre-flight

| Item | Verdict | Why / evidence |
|---|---|---|
| **C-01 Tenant isolation** | **not applicable** — client-side feature | The band calls the same tenant-scoped endpoint the full grid calls; isolation is enforced server-side and already covered by feat-008. No new access path. |
| **C-02 Authenticated by default** | **applies** | The band's request goes through `authFetch`, so it carries the bearer like every other call. Evidence: the 401 scenario above. |
| **C-03 Least-privilege authorization** | **not applicable** | No admin or config surface; reading records needs only tenant membership, as the full grid does. |
| **C-04 Personal data minimization** | **not applicable** | The band returns nothing the full grid does not already return for the same records. |
| **C-05 Secrets never committed** | **not applicable** | No credential or connection string. |
| **C-06 Encryption in transit** | **not applicable** | No new endpoint; deployment config unchanged. |
| **C-07 Image upload safety** | **not applicable** | The band uploads nothing. An IMAGE-typed visible field renders through the existing `RecordGridCell`, which already fetches through the binary endpoint. |
| **C-08 Rich-text sanitization** | **applies** | A FREE_TEXT visible field renders in the band. It must go through the same `RecordGridCell` preview path as the full grid — the band must not introduce a second, unsanitized rendering. Evidence: hostile-markup fixture rendered inside a band. |
| **C-09 Localization completeness** | **applies** | The band emits: the disclosure's accessible name, the "see all N records" line, the empty message and the failure message. Evidence: the locale Scenario Outline above, plus the existing keyset coverage check. |
| **C-10 Audit trail** | **not applicable** | Read-only; no irreversible or administrative action. |
| **C-11 Data retention & deletion** | **not applicable** | Manages no accounts or tenants. |
| **C-12 Encryption at rest for secret values** | **applies** | A Secret-flagged field can be marked visible. The band must mask it exactly as the full grid does and must never trigger a reveal. Evidence: a Secret visible field rendered in a band asserts the mask, and asserts no reveal request is issued. |

> **Accessibility** is owned by this satellite, not by the API's checklist. It is covered here by
> the keyboard scenario and by `aria-expanded` on the disclosure.

## Risks

| # | Risk | Why it is real here |
|---|---|---|
| R1 | The band re-implements the record grid instead of reusing it | Two renderings of the same data drift, and C-08/C-12 would then need proving twice. The plan must say which component is shared. |
| R2 | A Secret or FREE_TEXT field reaching the band by a path that skips `RecordGridCell` | That is exactly how a sanitization or masking hole appears. |
| R3 | The outer list re-rendering closes open bands | Expansion state must survive a parent re-render — the navigator's collapse set is the precedent. |
| R4 | The disclosure column shifting the existing column widths | The types list was measured to a settled layout this session; the new column must not disturb it. |
| R5 | `[+]`/`[-]` drawn a second time instead of reusing `ExpandIcon`/`CollapseIcon` | They exist and were built for exactly this idiom in the navigator. |

## Out of scope, deliberately

Everything under **Scope → Out**. In particular: **no API change**. If the plan concludes the
endpoint is insufficient, that is a finding to raise — not a licence to widen this feature into
the hub.

## Open Questions

None open. **OQ-32 is resolved** (2026-08-30) and its four answers are the source of S3, S4, S2
and the keyboard scenario respectively. Any new question found while planning gets its own OQ
rather than a silent assumption.
