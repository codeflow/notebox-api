# Feature — Aggregate counts for the Navigator and group listings

**ID:** features/016-group-counts
**User Story:** US-3.1
**Version:** v1
**Status:** Approved (human approval 2026-08-24)
**Date:** 2026-08-24

## Origin
- **User Story:** US-3.1 — *As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace.*
- **FRs served:** **FR-09** (the navigation-tree data the Navigator renders) and **FR-08** (the group management surface). This feature adds no new capability — it makes the *existing* group and tree contracts carry the sizes the UI needs to be readable.
- **BRs bound:** BR-01/BR-02 (every aggregate is computed inside the caller's tenant); **BR-06** (a task's status is the derived integer percent — this feature averages that value and never redefines it).
- **NFRs bound:** NFR-01 (no cross-tenant count), NFR-06 (OpenAPI updated in this feature), NFR-08 (the aggregates must not turn a bounded read into an unbounded one).
- **Primary source:** **OQ-27** *(decided by rafaelsantos, 2026-08-24)* — the aggregates design screens 05 and 14 require, added to the API rather than dropped from the design or derived client-side. The satellite's design reference is the demand side: `notebox-web/design/handoff` screen 05 (`Service Endpoint (12)`, `Migration (8)`, `Ungrouped (3)`) and screen 14 (**Annotations**, **Types used**, **Tasks**, **Avg. status** columns).
- **Consumes:** feat-014's shipped surface — `Group`, the `item_group` table, the membership FKs, `GET /navigation`, `GET /groups`, and the `group` filter on both item listings. **Additive only:** no field feat-014 published changes shape or meaning.
- **Why a separate feature:** feat-014 merged (PR #16) before the design conflict surfaced, so these fields could not ride with it. They must land **before feat-015's `implement`**, which consumes them — hence the delivery container was reordered to put this slice first.

## Summary
Three read surfaces gain the sizes they were missing. Each **group node** in the navigation tree
reports how many items it stands for. Each **group** in the listing reports its own aggregates —
for annotation groups the number of records and how many distinct types they span; for task groups
the number of tasks and the mean of their derived statuses. Every number is computed inside the
caller's tenant, in the same bounded query that already produces the row, so no read becomes N+1
and no aggregate scans more than the tenant's own data.

## Scope

- **In:**
  - **A count on every navigation group node (FR-09, design 05).** Each group node in
    `GET /navigation` reports the number of items it stands for. Because a group node under the
    Annotations root lives beneath a **type**, its count is *the records of that type in that
    group*; a group node under the Tasks root counts *the tasks in that group*. The synthetic
    **Ungrouped** node carries its count under the same rule. One consistent meaning throughout:
    **the count is the size of the listing that clicking this node opens** — which makes it
    verifiable against that listing's `total` rather than a number nobody can check.
  - **Per-group aggregates on the group listing (FR-08, design 14).** `GET /groups` rows gain:
    - **annotation domain:** the number of records in the group, and the number of **distinct
      annotation types** those records span (screen 14's *Annotations* and *Types used*);
    - **task domain:** the number of tasks in the group, and the **average of their derived
      statuses** (screen 14's *Tasks* and *Avg. status*).
    A group's aggregates cover only its own namespace — the other table is not consulted, since an
    item can only reference a group of its own domain (feat-014, I-8).
  - **Average status, defined precisely (BR-06, OQ-22 refinement).** The average is over the tasks'
    already-derived integer statuses, rounded to an **integer percent, half up** — mirroring OQ-22's
    rule for the status itself, so one rounding convention holds across the product. A group holding
    **no tasks has no average** (absent, not zero): zero means *has tasks, all at 0%*, and collapsing
    the two would misreport an empty group as a stalled one.
  - **Aggregates are tenant-scoped and bounded (NFR-01, NFR-08).** Every count is filtered by the
    caller's tenant at the same choke point as the row it decorates (AD-03), and is produced by the
    same bounded query — grouped aggregation, not a per-row follow-up. The tree stays bounded by
    *types × groups* and the group listing by its page size.
  - **OpenAPI (NFR-06)** updated for the extended schemas in this feature.

- **Out:**
  - **Any change to an existing field.** Purely additive; a client that ignores the new fields
    behaves exactly as it does today.
  - **Aggregates for the "ungrouped" row of design screen 14.** `GET /groups` lists *groups*, and a
    synthetic non-group row would corrupt the listing's `total` and its paging (NFR-08). The web can
    already source the ungrouped **counts** from the navigation tree it fetches anyway.
    **Consequence, resolved at the gate:** screen 14's ungrouped **Avg. status** cell had *no source*.
    **Decided by rafaelsantos 2026-08-24: the cell is removed from the design** (notebox-web
    `2ee5e3f`), rather than commissioning an ungrouped-aggregate surface to fill a mock. The
    ungrouped **count** survives, derived from the navigation tree.
  - **Counts on type nodes independent of a group**, i.e. "how many records does this type have in
    total". No design screen asks for it and no FR names it.
  - **Caching or materialising the aggregates** (NFR-03/Redis) — out of scope for this slice;
    correctness first, and the queries are bounded.
  - **Historical or trend data**, per-user counts, and any aggregate over annotation *values*.
  - **Recomputation triggers / stored counters.** The aggregates are computed on read. A stored
    counter would need invalidation on every record, task and group mutation — an integrity
    obligation no requirement justifies.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-09 Every navigation group node reports its size
  Scenario: A group node under a type counts that type's records in that group
    Given tenant A has the annotation type "RabbitMQ"
    And 12 of its records are in the group "Infrastructure" and 6 are ungrouped
    When a member reads the navigation tree
    Then under "RabbitMQ" the "Infrastructure" node reports 12
    And the Ungrouped node under "RabbitMQ" reports 6

  Scenario: The count equals the listing that clicking the node opens
    Given the navigation tree reports 12 for a group node under "RabbitMQ"
    When a member lists that type's records filtered to that group
    Then the listing's total is 12

  Scenario: A task group node counts the tasks in that group
    Given tenant A has 8 tasks in the task group "Migration" and 2 ungrouped tasks
    When a member reads the navigation tree
    Then the "Migration" node reports 8
    And the Ungrouped node under Tasks reports 2

  Scenario: The same group under two types counts each independently
    Given the group "Infrastructure" holds 12 records of "RabbitMQ" and 6 of "Runbook"
    When a member reads the navigation tree
    Then the "Infrastructure" node under "RabbitMQ" reports 12
    And the "Infrastructure" node under "Runbook" reports 6

  Scenario: Counts never cross tenants
    Given tenant B has 40 records of its own in a group named "Infrastructure"
    When a member of tenant A reads the navigation tree
    Then no node reports any of tenant B's records

Feature: FR-08 Annotation groups report their size and reach
  Scenario: A group reports its records and the types they span
    Given the annotation group "Infrastructure" holds 12 records of "RabbitMQ" and 6 of "Runbook"
    When a member lists the annotation groups
    Then "Infrastructure" reports 18 records
    And it reports 2 types used

  Scenario: An empty group reports zero on both
    Given the annotation group "Scratch" holds no records
    When a member lists the annotation groups
    Then "Scratch" reports 0 records and 0 types used

  Scenario: Types used counts distinct types, not records
    Given the annotation group "Documentation" holds 9 records, all of type "Vendor Contact"
    When a member lists the annotation groups
    Then it reports 9 records and 1 type used

Feature: FR-08 Task groups report their size and average progress
  Scenario: A group reports its tasks and their mean derived status
    Given the task group "Migration" holds tasks at 50%, 50% and 74%
    When a member lists the task groups
    Then "Migration" reports 3 tasks
    And its average status is 58

  Scenario: The average rounds half up, like the status itself (OQ-22)
    Given the task group "Support" holds tasks at 33% and 34%
    When a member lists the task groups
    Then its average status is 34

  Scenario: A group with no tasks has NO average, which is not zero
    Given the task group "Empty" holds no tasks
    When a member lists the task groups
    Then it reports 0 tasks
    And it reports no average status at all

  Scenario: A group whose tasks are all at zero averages zero, not nothing
    Given the task group "Fresh" holds two tasks, both at 0%
    When a member lists the task groups
    Then it reports 2 tasks and an average status of 0
    And that is distinguishable from the empty group above

  Scenario: The average follows the tasks' derived status, never a stored one
    Given a task in "Migration" has 1 of 2 subtasks done, so its status is 50
    When a member marks the second subtask done
    And lists the task groups again
    Then the group's average status has risen accordingly

Feature: Additive by construction (NFR-06, NFR-08)
  Scenario: A client ignoring the new fields is unaffected
    Given a client reading the tree and the group listing as feat-014 published them
    When it reads them after this feature
    Then every field it already consumed has the same name, type and meaning

  Scenario: The aggregates do not turn a bounded read into an unbounded one
    Given a tenant with 3 types, 4 groups and 500 records
    When a member reads the navigation tree
    Then the number of database statements is the same as before this feature
    And no statement is issued per node

  Scenario: An unauthenticated caller still gets nothing
    Given no credentials are presented
    When the navigation or group endpoints are called
    Then each is rejected as unauthenticated, and no count is disclosed
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies.** Every aggregate is computed behind the AD-03 choke
  point, on the same tenant predicate as the row it decorates. A count is a disclosure: reporting
  another tenant's total would leak the existence and size of their data without returning a row.
  *Evidence:* the cross-tenant tree scenario + a cross-tenant assertion on the group listing.
- [x] **C-02 · Authenticated by default** — **applies.** No new endpoint; the extended ones keep the
  shipped `@Authenticated` posture. *Evidence:* the unauthenticated scenario.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** No admin surface; group sizes are ordinary
  tenant-member data, visible to anyone who can already list the groups.
- [ ] **C-04 · Personal data minimization** — **n/a.** Aggregates are counts of the tenant's own
  domain objects; no user or tenant identity data is added to any payload.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential or key.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Existing TLS ingress; no new
  transport surface.
- [ ] **C-07 · Image upload safety** — **n/a.** No image binary is accepted or served.
- [ ] **C-08 · Rich-text sanitization** — **n/a.** The added fields are integers.
- [ ] **C-09 · Localization completeness** — **n/a.** This feature emits **no new user-facing
  message**: it adds numbers to existing payloads and introduces no new rejection. The labels
  beside those numbers are the satellite's (feat-015), in its own catalogs.
- [ ] **C-10 · Audit trail** — **n/a.** Read-only; no irreversible or administrative action.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account or tenant management.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** No Secret-flagged value is read;
  counting a record never decrypts one.

## Open Questions
- **None blocking.** **OQ-27** (2026-08-24) is this feature's originating decision and is recorded
  resolved.
- **Two refinements taken here under the standing OQ-09 delegation** (*"feature specs may refine"*),
  both disclosed rather than assumed:
  - **Average status rounds to an integer percent, half up**, mirroring OQ-22's rule for the status
    itself — one rounding convention across the product.
  - **A group with no tasks has no average at all**, which is deliberately distinct from an average
    of 0. Collapsing them would report an empty group as a stalled one.
- **Resolved at this spec's gate (2026-08-24):** scoping the ungrouped row out of `GET /groups`
  left design screen 14's ungrouped **Avg. status** cell without a source. **rafaelsantos chose to
  remove the cell from the design** — amended in notebox-web `2ee5e3f`, with the reason recorded on
  the screen-14 row and inline in the HTML. No API surface was invented to fill a mock, and the
  ungrouped *count* is unaffected.
