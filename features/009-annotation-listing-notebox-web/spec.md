# Feature — Records grid and detail projection (ADF Fusion theme)

**ID:** features/009-annotation-listing-notebox-web
**User Story:** US-2.2
**Version:** v1
**Status:** Draft
**Date:** 2026-08-12
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-2.2 — the **web half**: the per-type records grid (design screen 11) whose columns
  are the visible-for-viewing fields, wired to feat-008's listing endpoint. The **detail half is
  already shipped** (feat-006's record detail = design screen 12, all fields, masked secrets with
  audited Reveal) — this spec pins the grid/detail pair, it rebuilds nothing.
- **FRs covered (client obligations only):** FR-05 — the grid renders the listing's visible-field
  projection; the detail view keeps exposing all fields.
- **NFRs bound:** NFR-08 (the pager: default 50, max 200, driven by the server's `PageDto` facts).
- **BRs bound:** BR-09 (the grid's column set is presentation — the detail shows everything), BR-08
  (all strings en/pt), BR-10/FR-18 (masked secret cells, no reveal affordance in the grid).
- **Primary source:** the **feat-008 contract**
  (`features/008-annotation-listing-notebox-api/contracts/listing.md` — `PageDto`, rows as filtered
  `AnnotationRecordDto`, newest-first order, the two validation keys); design handoff **screen 11**
  (visible-field columns, LIST values as coloured badges, QBE filter row, horizontal scroll wrapper,
  `af-statusBar` pager) and screen 07's pager idiom; feat-006's shipped detail + navigation; the
  feat-002/004 web infrastructure (guard, `authFetch`, i18n, ADF Fusion theme).

## Summary
Deliver the annotations grid: from a type's page, the member opens its records as a data grid whose
columns are the record name plus the type's visible-for-viewing fields, rows newest first as the
server orders them, paged by the `af-statusBar` pager off `PageDto`'s facts. Cells render by field
type — badges for LIST, masked dots for secrets, compact thumbnails for images, plain-text previews
for rich text — and a row opens the existing detail view, which keeps showing every field (BR-09
pair). A QBE filter row narrows the **loaded page** client-side (the server ships unfiltered by
feat-008's recorded scope). All strings en/pt; the client re-runs no server rule.

## Scope
- **In:**
  - **Route & navigation:** `annotation-types/[id]/records` behind the existing guard; the type
    detail gains a "View records" action beside "New record"; a grid row opens the record detail;
    after a record delete, the member returns to the **grid** (small, declared change to feat-006's
    post-delete navigation, which predates the grid's existence).
  - **The grid (screen 11):** column per visible field in the type's field order, preceded by the
    record **name** column; rows exactly as the listing serves them (newest first — the client never
    re-sorts); horizontal scroll wrapper so no cell is clipped.
  - **Cell rendering by field type:** TEXT → text; NUMBER → number; LIST → **coloured badges**;
    SINGLE/MULTIPLE choice → option labels; **secret → masked dots, no reveal affordance in the
    grid** (reveal lives in the detail, audited); IMAGE → compact authenticated thumbnail;
    FREE_TEXT → a **plain-text preview** (markup stripped client-side, truncated) — rendered as
    text, never as HTML, so no rich value executes in a cell by construction.
  - **The pager (NFR-08, `af-statusBar`):** page position and total from `PageDto`; next/previous
    fetch the corresponding page; a page-size choice bounded by the contract (50 default, up to
    200); the pager never invents counts.
  - **QBE filter row:** narrows the **currently loaded page** client-side (case-insensitive match on
    the name and text-like columns), with a visible "showing filtered rows of this page" note —
    honest about its page-local reach; clearing restores the page. Server-side filtering is future
    work by feat-008's recorded scope, restated below.
  - **States:** loading, empty type ("no records yet" + the New record action), API errors surfaced
    verbatim (a foreign/missing type shows the established not-found idiom).
  - **Localization:** every grid string (headers come from field names — user data; controls,
    pager, filter note, empty state are catalog keys) in en + pt.
- **Out:** see **Out of scope** below.

## Acceptance criteria (Gherkin)
> The `PageDto` shape, row shape, order and error keys are feat-008's **consumed** contract. These
> scenarios become vitest + Testing-Library + MSW tests in `implement`.

```gherkin
Feature: The grid shows visible-field columns; the detail keeps showing all (FR-05, BR-09)

  Scenario: Columns are the name plus visible fields, in field order
    Given a type with fields URL (visible), Port (visible) and Description (not visible)
    And records exist
    When the member opens the type's records grid
    Then the columns are Name, URL, Port — in that order
    And no Description column exists

  Scenario: A grid row opens the detail, which shows every field (BR-09 pair)
    Given the grid above
    When the member opens a row
    Then the record detail shows URL, Port and Description values
    And the detail behaves exactly as shipped (masked secrets, audited Reveal)

  Scenario: Rows render in the server's order, untouched
    Given the listing returns rows r3, r2, r1 (newest first)
    Then the grid shows r3, r2, r1 in that order
    And the client applies no sorting of its own
```

```gherkin
Feature: Cells render by field type (FR-18, C-08, AD-04)

  Scenario: LIST values render as coloured badges
    Given a visible LIST field whose selected option carries badge colour GREEN
    Then the cell renders the option label as a badge with the GREEN colour

  Scenario: A visible secret field renders masked dots and no reveal affordance
    Given a visible Secret field with a stored value
    Then the cell shows the mask
    And no reveal action exists anywhere in the grid

  Scenario: A rich Free-text cell is a plain-text preview, never HTML
    Given a visible Free-text value containing dialect markup
    Then the cell shows the value's text content only, truncated
    And nothing from the value is rendered as HTML in the grid

  Scenario: An image cell renders a compact authenticated thumbnail
    Given a visible Image field with a stored reference
    Then the cell renders a thumbnail resolved through the authenticated binary endpoint
```

```gherkin
Feature: The pager drives the listing (NFR-08)

  Scenario: The pager states the server's facts
    Given the listing answers total 51, page 0, size 50
    Then the pager shows the position and the total of 51
    And the grid holds 50 rows

  Scenario: Next fetches the next page
    When the member pages forward
    Then the client requests page 1 from the listing
    And the grid replaces its rows with the response

  Scenario: The page size choice stays within the contract
    When the member changes the page size
    Then only sizes up to 200 are offered
    And the client requests the chosen size
```

```gherkin
Feature: QBE filters the loaded page, honestly

  Scenario: Typing in the filter row narrows the current page
    Given a loaded page with rows "prod-broker" and "dev-broker"
    When the member types "prod" in the name filter
    Then only "prod-broker" remains visible
    And a note states the filter applies to the loaded page

  Scenario: Clearing the filter restores the page
    When the member clears the filter
    Then all the page's rows are visible again
    And no request was sent by filtering
```

```gherkin
Feature: States, isolation and localization

  Scenario: An empty type shows the empty state with the New record action
    Given a type with no records
    Then the grid area shows the localized empty state
    And offers the New record action

  Scenario: A foreign or missing type shows the not-found idiom
    Given a type id of another tenant
    Then the established localized not-found outcome is shown
    And no data is displayed

  Scenario: The grid renders in pt when the locale is pt
    Given the resolved locale is pt
    Then the pager, filter note, empty state and actions are shown in pt
    And no raw message key reaches the screen

  Scenario: Every grid request carries the bearer and no tenant identifier
    When the grid loads or pages
    Then each request carries the member's token
    And no client-supplied tenant identifier
```

## Compliance pre-flight
Checklist from `constitution/02-compliance.md`, for a **web client** consuming feat-008:

- **C-01 · Tenant isolation** — **applies (client obligation).** No tenant parameter anywhere; the
  not-found idiom for foreign types. *Evidence:* the bearer/no-tenant and not-found scenarios.
- **C-02 · Authenticated by default** — **applies.** The route sits behind the existing global guard
  (covered by `RouteGuard.test.tsx` per the satellite idiom).
- **C-03 · Least-privilege authorization** — **not applicable.** The grid offers no elevated action —
  deliberately: no reveal in the grid (that stays in the detail, ADMIN-gated and audited).
- **C-04 · Personal data minimization** — **applies.** Nothing logged; rows carry only served data.
- **C-05/C-06/C-07** — **not applicable / inherited unchanged** (no upload in the grid).
- **C-08 · Rich-text sanitization** — **applies (render share, held by construction).** Grid cells
  render rich values as **text nodes** (plain preview), never HTML — nothing to sanitize in a cell;
  the detail's `RichTextValue` (sanitize-on-render) is unchanged. *Evidence:* the plain-preview
  scenario.
- **C-09 · Localization completeness** — **applies.** All new keys (pager, filter note, empty state,
  view-records action) in en + pt, keyset coverage green.
- **C-10/C-11** — **not applicable (client).**
- **C-12 · Encryption at rest** — **applies (client share).** Masked cells, no cleartext fetch from
  the grid. *Evidence:* the masked-cell scenario.

## Out of scope
- **Server-side filtering/search and user-chosen sorting** — feat-008's recorded scope; the QBE row
  is page-local by design and says so on screen. A server-filter feature is future work if the
  page-local honesty proves insufficient.
- **The record detail and editor** — shipped (feat-006); only the post-delete navigation target
  changes (to the grid), declared in scope.
- **Grid row selection/toolbar actions** (edit/delete from the grid) — the detail owns actions;
  screen 11 shows the grid as a navigation surface.
- **Groups navigation, tasks, exports** — other stories.
- **Any change to the API** — feat-008's contract is consumed as frozen.

## Open Questions
No new Open Question. OQ-20 (order) was decided before feat-008's plan and the grid renders the
served order untouched; the QBE page-local decision is a presentation choice grounded in feat-008's
recorded out-of-scope, stated in the open for your veto at this gate.
