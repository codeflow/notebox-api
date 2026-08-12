# Feature — Paginated record listing with visible-field column projection

**ID:** features/008-annotation-listing-notebox-api
**User Story:** US-2.2
**Version:** v1
**Status:** Draft
**Date:** 2026-08-12

## Origin
- **User Story:** US-2.2 — *As a tenant member, I want a listing of a type's annotations exposing
  visible fields, plus a detail view exposing all fields, so grid and detail have the data they need.*
  This feature is the **API half's remaining obligation**: the listing endpoint that feat-005's
  contract explicitly deferred (*"Out of scope (US-2.2): the collection listing
  `GET /annotation-records?typeId=…` with the visible-field column projection + pagination"*). The
  **detail half is already satisfied** by feat-005's `GET /annotation-records/{id}`, which returns
  every field including non-visible ones — this spec pins that, it does not rebuild it.
- **FRs covered:** FR-05 (list a type's annotations returning the **visible** fields; detail returns
  **all** fields). Also absorbs the FR-05 residual of deferred US-1.2 (the visible-column projection —
  the flag itself shipped in feat-003).
- **NFRs bound:** NFR-08 (paginated: default page size **50**, max **200** — decisão humana
  2026-07-22, OQ-10), NFR-06 (OpenAPI coverage).
- **BRs bound:** BR-09 (**visible-for-viewing is presentation, never access control** — the projection
  shapes the grid, while the detail endpoint keeps returning everything to an authorized caller),
  BR-03 (rows expose values only for fields the type defines), BR-10/FR-18 (a visible secret field
  appears **masked** in rows, like everywhere else).
- **Primary source:** PRD v2 §3.1 (FR-05), §3.2 (NFR-08), §Happy path 84 (create → list shows
  visible-field columns); `constitution/00-principles.md` BR-09; feat-005's REST contract (the row's
  value semantics — masking, and feat-007's sanitize-on-read discipline); the satellite's design
  handoff screen 11 (the grid's pager needs the total and page position — a client-observable
  requirement this API must serve).

## Summary
Add the one missing read path of US-2.2: list a type's records, paginated per NFR-08, each row
carrying the record's identity, name, timestamps and the values of **visible-for-viewing fields
only** — with every value rule the single-record read already enforces (secrets masked, rich text
served sanitized) intact on this new exit path. The response carries the pagination facts the grid's
pager displays (total records, page position). Nothing else changes: the detail read, create/update/
delete and reveal are untouched.

## Scope
- **In:**
  - **The listing (FR-05):** records of **one type** (the grid is per-type — design screen 11),
    tenant-scoped, paginated. Each row: record id, name, created/updated timestamps, and the values
    of the type's visible-for-viewing fields, in the type's field order.
  - **Projection (BR-09):** non-visible fields are absent from rows — as presentation shaping, not
    security. The detail endpoint (feat-005, unchanged) remains the all-fields view; this spec pins
    the pair with a scenario.
  - **Pagination (NFR-08):** default page size 50; requested sizes above 200 are rejected with the
    API's uniform validation envelope (consistent with the constitution's explicit-validation
    posture — the API never silently reshapes a caller's request); a page beyond the end is an empty
    page, not an error; the response states the **total** and the page position (the pager's facts).
  - **Sort order:** rows are returned **newest first** — `createdAt` descending, ties broken by id
    for total stability *(decisão humana 2026-08-12, OQ-20)*: a just-created record tops page 1,
    matching the PRD §84 create→grid happy path, and rows never move on edit.
  - **Value semantics inherited on the new path:** a visible **secret** field's value appears masked
    (never cleartext, no reveal here); a visible **rich Free-text** value is served **sanitized**
    (feat-007's C-08 read obligation applies to every exit path, including this one).
- **Out:** see **Out of scope** below.

## Acceptance criteria (Gherkin)
> These scenarios become executable API tests in `implement`. "The listing" means the new
> per-type, paginated collection read.

```gherkin
Feature: List a type's records with visible-field projection (FR-05, BR-09)

  Scenario: Rows carry only the visible fields, in field order
    Given a type with fields URL (visible), Port (visible) and Description (not visible)
    And a record holding values for all three
    When a member lists that type's records
    Then the record's row shows values for URL and Port, in the type's field order
    And no value for Description appears in the row

  Scenario: The non-visible value is still served by the detail read (BR-09 is display-only)
    Given the record above
    When the member reads the record by id
    Then all three values are returned, including Description
    And nothing about the detail read changed from its shipped behaviour

  Scenario: Rows only ever contain records of the requested type and tenant
    Given tenant A has records of types T1 and T2, and tenant B has records of T1's shape
    When a member of tenant A lists T1's records
    Then every row is a T1 record of tenant A
    And nothing of T2 or of tenant B appears

  Scenario: A visible secret field appears masked in rows (FR-18)
    Given a type whose Secret field is flagged visible-for-viewing
    And a record holding a secret value
    When the member lists the type's records
    Then the row's secret value is masked with no cleartext anywhere in the response

  Scenario: A visible rich Free-text value is served sanitized in rows (C-08)
    Given a record whose Free-text value was stored before sanitization existed, containing a script element
    When the member lists the type's records
    Then the row's value carries the dialect-clean form
    And no script element reaches the response
```

```gherkin
Feature: Pagination per NFR-08

  Scenario: The default page holds 50 rows
    Given a type with 51 records
    When the member lists its records without a page size
    Then the first page holds 50 rows
    And the response states a total of 51 and the page position

  Scenario: A requested size above 200 is rejected
    When the member requests a page size of 500
    Then the request is rejected with the uniform validation envelope
    And a localized message names the size bound

  Scenario: A page beyond the end is empty, not an error
    Given a type with 3 records
    When the member requests the fourth page
    Then the response succeeds with zero rows
    And still states the total of 3

  Scenario: Rows are returned in the documented sort order
    Given records created in a known sequence
    When the member lists the type's records
    Then the rows are ordered newest first — createdAt descending, ties broken by id (OQ-20)
    And the same request always returns the same order
```

```gherkin
Feature: Isolation and validation on the new path

  Scenario: Listing a foreign tenant's type is indistinguishable from a missing one
    Given a type id belonging to tenant B
    When a member of tenant A lists its records
    Then the outcome is the same localized not-found as for a nonexistent type id
    And no data of tenant B is returned

  Scenario: The type id is required
    When a member lists records without naming a type
    Then the request is rejected with the uniform validation envelope
```

## Compliance pre-flight
Checklist from `constitution/02-compliance.md`:

- **C-01 · Tenant isolation** — **applies.** The new query is tenant-scoped like every other;
  *evidence:* the cross-tenant listing scenario + repository-level scoping in the established pattern.
- **C-02 · Authenticated by default** — **applies.** The endpoint sits under the same class-level
  authentication as the rest of the resource.
- **C-03 · Least-privilege authorization** — **not applicable.** Listing is member-level; no
  elevated action (reveal stays where it is).
- **C-04 · Personal data minimization** — **applies.** Rows carry only record content; no identities
  or tokens in logs.
- **C-05/C-06/C-07 · Secrets, transit, images** — **not applicable / inherited unchanged.**
- **C-08 · Rich-text sanitization** — **applies.** The listing is a **new output path** and must
  serve rich values through the feat-007 read seam. *Evidence:* the legacy-row listing scenario.
- **C-09 · Localization completeness** — **applies.** Any new validation key (page-size bound,
  missing type id) ships in en + pt with coverage-test rows; if existing keys suffice, that is the
  evidence instead.
- **C-10 · Audit trail** — **not applicable.** Reading is not audited (consistent with feat-005:
  only reveal/delete/erase are).
- **C-11 · Data retention** — **not applicable.**
- **C-12 · Encryption at rest** — **applies (guarded, unchanged).** The masked-row scenario proves
  the listing introduces no new cleartext path.

## Out of scope
- **The detail view** — already shipped (feat-005 `GET /annotation-records/{id}`); pinned here by
  scenario, not rebuilt.
- **QBE filtering / search parameters** — the design's grid shows a QBE filter row, but no FR binds
  server-side filtering; whether feat-009 filters client-side over the loaded page or a filtering
  feature is opened later is **feat-009's spec decision**, recorded there — this endpoint ships
  unfiltered.
- **Sorting options / client-chosen order** — one documented order (OQ-20); user-selectable sort is
  future work if a story asks for it.
- **Cross-type or cross-tenant listings, exports, counts-only endpoints** — no FR names them.
- **Any change to create/update/delete/reveal or the single-record read.**

## Open Questions
No Open Question is pending for this spec.

- **OQ-20 — resolved (2026-08-12):** listing order is **newest first** (`createdAt` desc, id
  tiebreak). Decided by the human; recorded in the catalog and PRD. The wire contract documents it.
