# Feature — Task dates, card link and rich-text details

**ID:** features/012-task-details-notebox-api
**User Story:** US-4.2
**Version:** v1
**Status:** Approved (human approval 2026-08-15)
**Date:** 2026-08-15

## Origin
- **User Story:** US-4.2 — *As a tenant member, I want task dates derived from subtasks, a card link, and rich-text details, so a task is self-contained.*
- **FRs covered:**
  - **FR-12:** derive a task's **start date = min(subtask start date)** and **end date = max(subtask end date)** — by date, not by position/insertion order *(decisão humana 2026-07-22, OQ-05)*.
  - **FR-13:** attach a **card** as an **inline value object** (code id + optional URL) directly on a task or subtask; expose the id and its link. Not a shared entity *(decisão humana 2026-07-22, OQ-06)*.
  - **FR-14:** store and return a task's **rich-text details** (WYSIWYG HTML: styles, colour, bold, underline, embedded images), **sanitized on input and output** (C-08).
  - Together these complete the FR-10/FR-11 attributes that feat-010 deliberately deferred (its Origin: *"optional card, optional rich-text details, derived dates — US-4.2's scope"*).
- **BRs bound:** BR-07 (task dates derive from subtasks; computed, never independently editable — enforced in the BR-06 style: a client write attempt is **rejected**, never silently ignored, refinement under the OQ-09 delegation). BR-06 itself is untouched: status derivation ships in feat-010 and this feature must not alter it.
- **NFRs bound:** NFR-01 (no cross-tenant access), NFR-02/C-09 (localized messages en+pt), NFR-06 (OpenAPI in sync in the same feature), NFR-07 (structured logging, correlation id). NFR-04/C-07 remain **standing** obligations of the shipped FR-07 image store — this feature adds **no** image upload or retrieval surface (embedded images are dialect references into that store).
- **Primary source:** PRD v2 §2 (US-4.2 ← INTAKE C21, C25, C26, C27), §3.1 (FR-12/FR-13/FR-14), §3 validation baseline (OQ-09 — *"feature specs may refine"*); constitution BR-06/BR-07, C-08/C-09; `catalogs/open-questions.md` OQ-05 + OQ-06 (both resolved 2026-07-22).
- **Consumes:** feat-010's task aggregate and contract (the surface being extended; its **replace-update semantics** — an update states the full intended state, omissions clear — govern the new optional fields too); feat-007's sanitization dialect (the project's **single rich-text dialect**, C-08 — task details use it *exactly*, per the PRD: Free-text values are sanitized *"exactly as task details are"*); FR-07's image store (`data-image-id` references); feat-001 tenancy (AD-03 choke point).

## Summary
A task becomes self-contained: its **start/end dates are derived** from its subtasks (earliest start,
latest end — recomputed on every subtask add, update and delete, and never writable by a client, BR-07);
a task or subtask can carry an inline **card** — a short code id plus an optional link — owned by that
task or subtask alone (OQ-06); and a task can carry optional **rich-text details** in the established
sanitized dialect, cleaned on write **and** on read (C-08), with embedded images referencing the shipped
image store rather than carrying binaries. API contract only — the UI is feat-013.

## Scope
- **In:**
  - **Derived task dates (FR-12, BR-07):** task `startDate` = minimum of its subtasks' start dates,
    `endDate` = maximum of its subtasks' end dates; a subtask with no start (or no end) date simply does
    not participate in that bound; a task whose subtasks carry no dates — or with no subtasks — has no
    dates. Recomputed on subtask **add, update and delete** (the same trigger set that recomputes status).
    Derived dates appear on **every task representation** (read-one, listing rows, and the task returned
    by subtask mutations — which must already reflect the recomputation). A client attempt to write task
    dates is rejected with a localized validation error (BR-06-style enforcement, OQ-09 refinement).
    No cross-subtask ordering constraint exists: the per-subtask start ≤ end rule (feat-010) stands, but
    derived start may exceed derived end when disjoint subtasks imply it — reported as computed.
  - **Card value object (FR-13, OQ-06):** an optional card on a **task** and on a **subtask** — a
    required **code** plus an optional **URL** — stored inline on its owner, returned wherever the owner
    is returned (including listing rows: two short scalars). Refinements under OQ-09: code required when
    a card is present, code ≤ **60** chars, URL ≤ **2048** chars, URL must be absolute **http/https**
    (a card link renders as an anchor in the web — no `javascript:`/`data:` scheme may be storable).
    Updates follow the established replace semantics: an update omitting the card clears it.
  - **Rich-text details (FR-14, C-08):** an optional `details` value on the **task only**, accepted and
    returned in the **feat-007 dialect** — sanitized on write and on read with the same allow-list
    (formatting marks, lists, blockquote, colour-only style, http links with the forced rel, inline
    code / pre code blocks, and `data-image-id` images with no `src`). Embedded images are **references**
    into the shipped FR-07 store; no binary travels in the HTML. Details are returned by read-one and by
    the task returned from mutations, but **never on listing rows** (mirrors the feat-010 decision that
    listing rows stay scalar — details are unbounded text). Omitting details on update clears them
    (replace semantics); a task created without details has none.
  - **Localized errors (C-09):** every new rejection carries a dot-namespaced key resolvable in en and pt.
  - **OpenAPI (NFR-06):** the extended schemas are reflected in the published contract in this feature.
- **Out:**
  - **Groups and the navigation tree** (FR-08/FR-09, US-3.1) — tasks remain ungrouped.
  - The **task UI** — feat-013-task-details-notebox-web.
  - **Any new image upload/retrieval surface** — FR-07 shipped it; this feature only references it.
  - **Subtask rich-text details** — FR-11 grants subtasks name, dates, card and done flag; no FR gives a
    subtask a details value.
  - **Manual task dates** — BR-07 forbids them; the write-rejection above is the whole story.
  - **Any change to status derivation** (BR-06, feat-010) — the % rule, rounding and triggers are frozen.
  - **A numeric size cap on `details`** — no FR or NFR names one; storage sizing is the plan's concern
    (NFR-04's 5 MB bounds image binaries, which never enter this payload).
  - **Redis caching** (NFR-03), **secret-value handling** (FR-18/C-12 — tasks define no secret fields),
    card-as-shared-entity or any card catalog/CRUD (OQ-06 decided inline), bulk operations, and subtask
    ordering/position — no FR names them.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-12 Task dates derive from subtasks (BR-07)
  Scenario: Start is the earliest start and end the latest end, regardless of insertion order
    Given a task of tenant A
    And a subtask "Cutover" created first, starting 2026-09-03 and ending 2026-09-10
    And a subtask "Inventory" created second, starting 2026-09-01 and ending 2026-09-05
    When a member of tenant A reads the task
    Then the task's start date is 2026-09-01 and its end date is 2026-09-10

  Scenario: A subtask missing one date simply does not participate in that bound
    Given a task with subtasks "A" (start 2026-09-01, no end), "B" (no start, end 2026-09-10) and "C" (no dates)
    When a member reads the task
    Then the task's start date is 2026-09-01 and its end date is 2026-09-10

  Scenario: A task with no dated subtasks has no dates
    Given a task with one subtask that has no dates
    When a member reads the task
    Then the task carries no start date and no end date
    And a task with no subtasks at all also carries neither

  Scenario: Changing a subtask's dates moves the derived dates immediately
    Given a task whose only subtask "A" runs 2026-09-01 to 2026-09-05, so the task runs 2026-09-01 to 2026-09-05
    When a member updates "A" to run 2026-09-02 to 2026-09-08
    Then the response already shows the task running 2026-09-02 to 2026-09-08

  Scenario: Deleting the boundary subtask recomputes the bound
    Given a task with subtasks "A" (2026-09-01 → 2026-09-02) and "B" (2026-09-05 → 2026-09-10)
    When a member deletes subtask "A"
    Then the response shows the task running 2026-09-05 to 2026-09-10

  Scenario: Disjoint one-sided subtasks may inverse the derived pair, and it is reported as computed
    Given a task with subtask "A" (start 2026-09-10, no end) and subtask "B" (no start, end 2026-09-01)
    When a member reads the task
    Then the task's start date is 2026-09-10 and its end date is 2026-09-01
    And no error is raised — only the per-subtask start ≤ end rule exists

  Scenario: A client cannot write task dates
    Given an authenticated member of tenant A
    When they create or update a task supplying a start date or an end date
    Then the request is rejected with a localized validation error naming the dates as derived and non-writable
    And the stored task's dates remain exactly the computed values

  Scenario: Listing rows carry the derived dates
    Given tenant A has a task whose subtasks span 2026-09-01 to 2026-09-10
    When a member lists tasks
    Then that task's row carries start 2026-09-01 and end 2026-09-10 alongside its derived status

Feature: FR-13 Card as an inline value object on task and subtask
  Scenario: A task card with code and URL round-trips
    Given an authenticated member of tenant A
    When they create a task named "Broker migration" with card code "PAY-231" and card URL "https://tracker.example/PAY-231"
    Then reading the task returns the card with exactly that code and URL

  Scenario: A card may carry only its code
    When a member creates a task with card code "PAY-232" and no card URL
    Then reading the task returns the card code "PAY-232" and no URL

  Scenario: A card without a code is rejected
    When a member creates a task supplying a card whose code is blank and whose URL is "https://tracker.example/x"
    Then the request is rejected with a localized required-card-code validation error

  Scenario: A card URL that is not absolute http or https is rejected
    When a member creates a task with card code "PAY-233" and card URL "javascript:alert(1)"
    Then the request is rejected with a localized invalid-card-url validation error
    And the same happens for a card URL of "data:text/html;base64,x" or a relative path

  Scenario: A card code above 60 characters is rejected
    When a member creates a task whose card code is 61 characters long
    Then the request is rejected with a localized card-code-too-long validation error

  Scenario: A subtask carries a card under the same contract
    Given a task of tenant A
    When a member adds a subtask "Contract review" with card code "LEG-7" and card URL "https://tracker.example/LEG-7"
    Then the returned task shows that subtask carrying exactly that card

  Scenario: An update omitting the card clears it
    Given a task carrying card "PAY-231"
    When a member updates the task stating name and priority but no card
    Then reading the task returns no card

  Scenario: Cards are inline copies, never a shared entity
    Given tasks "T1" and "T2" of tenant A, each carrying a card with code "PAY-231"
    When a member updates T1's card URL
    Then T2's card is byte-for-byte unchanged

Feature: FR-14 Rich-text details, sanitized both ways (C-08)
  Scenario: A dialect-clean details value round-trips unchanged
    Given a details value using the established dialect — formatting marks, a list, a blockquote, a colour span, an http link with the forced rel, inline code, and an image carrying data-image-id="i1"
    When a member creates a task with those details and reads it back
    Then the returned details are byte-identical to what was sent

  Scenario: Hostile markup is stripped on write
    When a member creates a task whose details are "<p>before</p><script>steal()</script><p onclick=\"x()\">after</p>"
    Then the create succeeds
    And the stored and returned details contain "before" and "after"
    And no script element and no on* attribute survive

  Scenario: A javascript: link loses its href but keeps its text
    When a member writes details containing <a href="javascript:alert(1)">click</a>
    Then the returned details contain "click"
    And no javascript: URL survives anywhere in them

  Scenario: An embedded image is a reference, never a fetched binary
    When a member writes details containing <img src="https://evil.example/x.png" data-image-id="i9" alt="diagram">
    Then the returned details carry data-image-id="i9" and alt="diagram"
    And no src attribute and no "evil.example" survive

  Scenario: A legacy hostile row is sanitized on the way out
    Given a task details value written directly to the database containing a script element and an onerror attribute
    When a member reads the task through the API
    Then the returned details carry the surviving text and dialect markup only

  Scenario: Details are optional and cleared by an update that omits them
    Given a task created without details
    Then reading it returns no details
    And when a member later sets details and then updates the task omitting them
    Then reading the task returns no details again

  Scenario: Listing rows never carry details
    Given tenant A has a task whose details are a long rich-text value
    When a member lists tasks
    Then that task's row carries name, priority, status, dates and card — and no details value

Feature: Standing guarantees over the extended surface (C-01, C-02, C-09)
  Scenario: A foreign tenant's task stays indistinguishable from a missing one
    Given a task of tenant B carrying a card and details
    When an authenticated member of tenant A reads, updates or deletes that task
    Then the outcome is exactly the outcome for a task id that does not exist

  Scenario: New validation messages resolve in the caller's locale (C-09)
    Given an authenticated member of tenant A whose locale resolves to pt
    When they create a task with a card whose code is blank
    Then the rejection message is the Portuguese catalog text for the required-card-code key
    And no raw message key or blank string reaches the client
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies.** The new fields ride the tenant-owned task surface; no new
  query path bypasses the choke point. *Evidence:* the foreign-tenant scenario above; feat-010's
  per-endpoint cross-tenant tests stay green over the extended DTOs (NFR-01).
- [x] **C-02 · Authenticated by default** — **applies.** No new endpoint is introduced and no extended
  endpoint becomes public. *Evidence:* feat-010's unauthenticated-rejection tests remain asserting over
  the extended surface.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** No admin/config surface; cards, dates and
  details are ordinary tenant-member data.
- [ ] **C-04 · Personal data minimization** — **n/a.** No new user/tenant identity data is stored or
  returned.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential, connection string or key.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Served under the existing TLS ingress;
  no new transport surface. *Evidence:* deployment/ingress config (unchanged).
- [ ] **C-07 · Image upload safety** — **n/a for new work, standing otherwise.** This feature accepts and
  serves no image binaries: embedded images are `data-image-id` references into the shipped FR-07 store,
  whose upload path already enforces content-type/size bounds (NFR-04) — and the dialect strips `src`, so
  no binary or foreign URL can enter through details. *Evidence:* the embedded-image sanitization scenario.
- [x] **C-08 · Rich-text sanitization** — **applies in full**; this feature is the constitution item's
  named case (task `details`). Sanitized on **input and output** to the established feat-007 allow-list —
  one dialect project-wide. *Evidence:* the hostile-fixture scenarios (write, read-side legacy row,
  embedded image, javascript: link) becoming tests in `implement`.
- [x] **C-09 · Localization completeness** — **applies.** Every new validation key (card code/URL, derived
  dates non-writable) has en + pt catalog values. *Evidence:* the pt-locale scenario + catalog coverage
  check (NFR-02).
- [ ] **C-10 · Audit trail** — **n/a.** No new irreversible or administrative action: deletions and their
  audits shipped in feat-010; clearing a card or details is the client's explicitly stated replace-update
  state (the established contract), not a silent side effect (BR-05 untouched).
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account/tenant management.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** Tasks define no Secret-flagged fields.

## Open Questions
- **None new.** OQ-05 (min/max by date) and OQ-06 (inline value object) were resolved 2026-07-22 and are
  folded into the scenarios above. The card bounds (code ≤ 60, URL ≤ 2048, absolute http/https only) and
  the BR-07 write-rejection are refinements under the standing OQ-09 delegation (*"feature specs may
  refine"*), consistent with feat-010's precedent.
