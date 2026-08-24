# Feature — Item groups and navigation-tree data

**ID:** features/014-groups-navigation-notebox-api
**User Story:** US-3.1
**Version:** v2
**Status:** Approved (human approval 2026-08-22) · **v2** — nesting scenario amended 2026-08-24 (audit F-01)
**Date:** 2026-08-22

## Origin
- **User Story:** US-3.1 — *As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace.*
- **FRs covered:**
  - **FR-08:** create and manage **groups**; assign an annotation or task to **at most one** group. Groups are **flat** (no nesting) and **separate per domain** — Annotation groups ≠ Task groups *(decisão humana 2026-07-22, OQ-04)*.
  - **FR-09:** provide the **navigation-tree data** — annotations by type and tasks, organized by group.
- **BRs bound:** BR-01/BR-02 (tenant ownership and isolation — a group is tenant-owned domain data like every other entity, D4); BR-05 (delete is explicit and irreversible — refined for this surface by OQ-24: the *group* is destroyed, its members are not).
- **NFRs bound:** NFR-01 (no cross-tenant read or write on any new endpoint), NFR-02/C-09 (every new message resolves in en + pt), NFR-06 (OpenAPI in sync in this same feature), NFR-07 (structured logging with correlation id), NFR-08 (the group listing and the group-filtered item listings are paginated on the established terms — default 50, max 200).
- **Primary source:** PRD v2 §1.4 (*"Groups and the navigation-tree data"* is in-scope for this API), §2 (US-3.1 ← INTAKE C28, C29, C30), §3.1 (FR-08, FR-09), §3 validation baseline (OQ-09 — *"feature specs may refine"*); constitution BR-01/BR-02/BR-05, C-01/C-02/C-09/C-10; `catalogs/open-questions.md` OQ-04 (2026-07-22) and **OQ-23 / OQ-24 / OQ-25 (2026-08-22)**.
- **Consumes:** feat-003's annotation **types** (the structural axis of the tree, C28); feat-005/feat-008's annotation **records** and their paginated listing (the entity that carries an annotation group, and the listing a group node resolves to); feat-010/feat-012's **tasks** and their paginated listing (same, on the task side); feat-001's tenancy choke point (AD-03). Its **replace-update semantics** — an update states the full intended state, omissions clear — govern the new group reference exactly as they govern cards and details.

## Summary
Tenant members gain **groups**: flat, tenant-owned labels living in two independent namespaces —
one for annotation records, one for tasks (OQ-04). Any record or task carries **at most one** group
of its own domain, set and cleared through the established replace-update contract. On top of that,
a single read returns the **navigation-tree data** backing the web Navigator (C28): an *Annotations*
root whose children are the annotation types, each type carrying the groups its own records occupy;
and a *Tasks* root whose children are the task groups. The tree **stops at group nodes** — it never
enumerates individual records or tasks (OQ-23) — so clicking a node resolves through the existing
paginated listings, extended here with a group filter. API contract only; the Navigator UI is feat-015.

## Scope

- **In:**
  - **Group lifecycle (FR-08).** A group carries a **name** and a **domain** ∈ {annotation, task}, is
    owned by exactly one tenant, and is **flat** — no parent, no nesting, at any depth (OQ-04). Full
    CRUD: create, read one, list (paginated, NFR-08), rename, delete. Refinements under the OQ-09
    delegation: name **required and non-blank**, **≤ 120 chars** (the type-name bound feat-003 set),
    and **unique per tenant per domain** — mirroring the baseline's *"annotation type name unique per
    tenant"*, scoped per domain because the namespaces are separate, so the same name may exist once
    as an annotation group and once as a task group. A group's **domain is immutable** after creation:
    changing it would silently orphan every member, and no FR asks for it.
  - **Deleting a group un-groups its members (FR-08, BR-05, OQ-24).** Deleting a group that still holds
    records or tasks succeeds: the group is destroyed, every member survives and becomes **ungrouped**.
    No annotation record and no task is ever deleted by this path. This **deliberately diverges from
    OQ-14's** block rule for annotation types, whose justification — a record without its type is
    meaningless — does not transfer to a label. The deletion is audited (C-10).
  - **Group assignment (FR-08).** An annotation **record** and a **task** each gain an optional
    reference to **at most one** group. Assignment follows the established replace semantics: an update
    that states a group sets it, an update that omits it **clears** it. Rejections: a group of the
    **wrong domain** (a task pointed at an annotation group, or the reverse) and a group **of another
    tenant** — the latter indistinguishable from a group id that does not exist (NFR-01).
  - **Navigation tree (FR-09, OQ-23).** One read returns the whole tree for the caller's tenant:
    - an **Annotations** root whose children are the tenant's annotation **types** (C28) — types are
      *structural*, so a type node appears whether or not it has any records;
    - under each type, the **groups that that type's own records occupy**, plus a synthetic
      **Ungrouped** node when that type has at least one ungrouped record — group and Ungrouped nodes
      are *data projections*, present only when non-empty;
    - a **Tasks** root whose children are the task groups holding at least one task, plus **Ungrouped**
      under the same rule.
    Sibling ordering is **name ascending, case-insensitive, ties broken by id**, with **Ungrouped
    pinned last** regardless of collation (OQ-25). Each node carries what the web needs to route a
    click (C30) — its identity and the domain/type it belongs to — and nothing more.
  - **Group-filtered listings (FR-09 → C30).** The shipped record listing (feat-008) and task listing
    (feat-010) accept an optional **group filter**, including an explicit *ungrouped* selection, so a
    tree node resolves to its items through the existing paginated contract rather than a second
    listing surface. Sort order, page defaults and row shape are **unchanged** (OQ-20, OQ-21, NFR-08).
  - **Localized errors (C-09).** Every new rejection carries a dot-namespaced key resolvable in en and pt.
  - **OpenAPI (NFR-06).** The new endpoints and the extended schemas are published in this feature.

- **Out:**
  - **The Navigator UI** — the sidebar tree, click-to-detail routing and any drag/drop are C28/C30 WEB
    scope, delivered by feat-015-groups-navigation-notebox-web.
  - **Nested groups and multi-group membership** — closed by OQ-04; no parent reference is accepted.
  - **Grouping annotation *types*** — closed by OQ-23; the group reference sits on the record, not the type.
  - **Records and tasks as tree leaves** — closed by OQ-23; enumerating them would defeat NFR-08 and
    duplicate the listing contract.
  - **Node counts, badges or icons on tree nodes** — no FR names them; adding them would put an
    unbounded aggregate behind every node.
  - **Cross-domain groups** — one group never serves both records and tasks (OQ-04's separate namespaces).
  - **Bulk assignment / bulk move** — no FR names a bulk surface; assignment rides the existing per-item update.
  - **Localization of group names** — D5: user-created content is stored as entered, never translated.
  - **Reordering groups by hand** — ordering is derived (OQ-25); no FR grants a manual position.
  - **Redis caching (NFR-03)**, **secret-value handling (FR-18/C-12)** — groups define no secret fields —
    and any change to record/task **sort order** or **status/date derivation** (OQ-20, OQ-21, BR-06, BR-07).

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-08 Group lifecycle in two separate namespaces (OQ-04)
  Scenario: A member creates a group in each domain
    Given an authenticated member of tenant A
    When they create an annotation group named "Brokers" and a task group named "Q3 Migration"
    Then both are created and owned by tenant A
    And each reports the domain it was created in

  Scenario: The same name is free in the other domain but taken in its own
    Given tenant A already has an annotation group named "Brokers"
    When a member creates a task group named "Brokers"
    Then it is created, because the namespaces are separate
    But when they create a second annotation group named "Brokers"
    Then the request is rejected with a localized duplicate-name error
    And tenant A still has exactly one annotation group named "Brokers"

  Scenario: Name uniqueness is scoped to the tenant
    Given tenant B has an annotation group named "Brokers"
    When a member of tenant A creates an annotation group named "Brokers"
    Then it is created, because uniqueness is per tenant per domain

  Scenario: A group cannot be nested
    Given an authenticated member of tenant A and an existing annotation group "Brokers"
    When they attempt to create a group declaring "Brokers" as its parent
    Then the parent is not stored and the created group has none
    And reading it back returns no parent of any kind
    # The contract exposes no parent, so nesting is unrepresentable rather than rejected:
    # unknown JSON properties are ignored project-wide (no FAIL_ON_UNKNOWN_PROPERTIES), which
    # is every endpoint's behaviour, not this feature's. Amended from "the request is rejected"
    # — audit F-01, decided by rafaelsantos 2026-08-24.

  Scenario: A group's domain is fixed at creation
    Given an annotation group "Brokers" of tenant A
    When a member updates it stating the task domain
    Then the request is rejected with a localized immutable-domain error
    And "Brokers" is still an annotation group

  Scenario: A blank name is rejected
    Given an authenticated member of tenant A
    When they create a group whose name is blank
    Then the request is rejected with a localized required-name error

  Scenario: Renaming keeps the members attached
    Given an annotation group "Brokers" of tenant A holding 3 records
    When a member renames it to "Message Brokers"
    Then the group's name is "Message Brokers"
    And the same 3 records are still assigned to it

Feature: FR-08 Deleting a group destroys the label, never its members (BR-05, OQ-24)
  Scenario: A non-empty group is deleted and its members become ungrouped
    Given an annotation group "Brokers" of tenant A holding 12 records
    When a member deletes "Brokers"
    Then the deletion succeeds and "Brokers" no longer exists
    And all 12 records still exist
    And each of those 12 records is now ungrouped

  Scenario: The same rule holds on the task side
    Given a task group "Q3 Migration" of tenant A holding 4 tasks
    When a member deletes it
    Then the 4 tasks still exist and are now ungrouped

  Scenario: The deletion is audited (C-10)
    Given an annotation group "Brokers" of tenant A
    When a member deletes it
    Then an audit entry records who deleted it, which group, and when

Feature: FR-08 An item belongs to at most one group, of its own domain
  Scenario: Assigning a record to an annotation group
    Given a record of type "RabbitMQ" in tenant A and an annotation group "Brokers"
    When a member updates the record stating the group "Brokers"
    Then the record reports "Brokers" as its group

  Scenario: A second assignment replaces the first — never accumulates
    Given a record assigned to the annotation group "Brokers"
    And a second annotation group "Datastores" in tenant A
    When a member updates the record stating the group "Datastores"
    Then the record's only group is "Datastores"

  Scenario: Omitting the group clears it (established replace semantics)
    Given a record assigned to the annotation group "Brokers"
    When a member updates the record without stating any group
    Then the record is ungrouped
    And the group "Brokers" still exists

  Scenario: A task cannot be put in an annotation group
    Given a task of tenant A and an annotation group "Brokers" of tenant A
    When a member updates the task stating the group "Brokers"
    Then the request is rejected with a localized wrong-domain error
    And the task is still ungrouped

  Scenario: A foreign tenant's group is indistinguishable from a missing one (NFR-01)
    Given an annotation group "Brokers" belonging to tenant B
    When an authenticated member of tenant A updates one of their records stating that group
    Then the outcome is exactly the outcome for a group id that does not exist
    And the record is unchanged

Feature: FR-09 Navigation-tree data (OQ-23, OQ-25)
  Scenario: The tree carries the two roots, types under Annotations, groups under each type
    Given tenant A has annotation types "RabbitMQ" and "Kafka"
    And "RabbitMQ" has records in the annotation groups "alpha" and "gamma", and one ungrouped record
    And "Kafka" has one record in "alpha" and no ungrouped record
    And tenant A has one task in the task group "Q3 Migration" and one ungrouped task
    When a member of tenant A reads the navigation tree
    Then the tree has an Annotations root and a Tasks root
    And under Annotations, "Kafka" precedes "RabbitMQ"
    And "RabbitMQ" carries the nodes "alpha", "gamma" and Ungrouped, in that order
    And "Kafka" carries the node "alpha" and no Ungrouped node
    And under Tasks, the nodes are "Q3 Migration" then Ungrouped
    And no individual record or task appears anywhere in the tree

  Scenario: Sibling ordering is case-insensitive with Ungrouped pinned last
    Given a type "RabbitMQ" whose records occupy the annotation groups "gamma", "Beta" and "alpha"
    And that type also has one ungrouped record
    When a member reads the navigation tree
    Then that type's nodes are "alpha", "Beta", "gamma", Ungrouped — in that order

  Scenario: A type node is structural, a group node is a projection
    Given tenant A has an annotation type "Postgres" with no records at all
    And tenant A has an annotation group "empty-group" that no record references
    When a member reads the navigation tree
    Then "Postgres" appears as a type node with no child nodes
    And "empty-group" appears nowhere in the tree
    But "empty-group" is still returned by the group listing

  Scenario: A tenant with nothing yet gets both roots and no children
    Given tenant A has no annotation types, no records, no tasks and no groups
    When a member reads the navigation tree
    Then the Annotations root and the Tasks root are both present and both empty

  Scenario: The tree never crosses tenants (NFR-01)
    Given tenant B has annotation types, groups, records and tasks
    When a member of tenant A reads the navigation tree
    Then nothing owned by tenant B appears in it

Feature: FR-09 A tree node resolves through the existing paginated listings (C30, NFR-08)
  Scenario: Filtering a type's records by a group node
    Given the type "RabbitMQ" has 3 records in the annotation group "alpha" and 2 ungrouped records
    When a member lists that type's records filtered to "alpha"
    Then exactly the 3 records of "alpha" are returned
    And the rows carry the same visible fields, order and page defaults as the unfiltered listing

  Scenario: Filtering to ungrouped items explicitly
    Given the type "RabbitMQ" has 3 records in "alpha" and 2 ungrouped records
    When a member lists that type's records filtered to ungrouped
    Then exactly the 2 ungrouped records are returned

  Scenario: Filtering tasks by a group node
    Given tenant A has 4 tasks in the task group "Q3 Migration" and 6 tasks elsewhere
    When a member lists tasks filtered to "Q3 Migration"
    Then exactly those 4 tasks are returned, newest first, on the established page defaults

Feature: Standing guarantees over the new surface (C-01, C-02, C-09)
  Scenario: Every new endpoint refuses an unauthenticated caller
    Given no credentials are presented
    When the group endpoints or the navigation-tree endpoint are called
    Then each is rejected as unauthenticated, and no tenant data is disclosed

  Scenario: A foreign tenant's group stays indistinguishable from a missing one
    Given a group of tenant B
    When an authenticated member of tenant A reads, renames or deletes it
    Then the outcome is exactly the outcome for a group id that does not exist

  Scenario: New validation messages resolve in the caller's locale (C-09)
    Given an authenticated member of tenant A whose locale resolves to pt
    When they create a group whose name duplicates an existing one in the same domain
    Then the rejection message is the Portuguese catalog text for the duplicate-name key
    And no raw message key or blank string reaches the client
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies.** Groups and the tree are tenant-owned data on new
  endpoints; every read and write resolves through the AD-03 choke point, and the tree is assembled
  only from the caller's tenant. *Evidence:* the foreign-tenant group scenario, the cross-tenant tree
  scenario, and a cross-tenant test per new endpoint (NFR-01).
- [x] **C-02 · Authenticated by default** — **applies.** This feature adds the first new endpoints since
  feat-012; none of them is public. *Evidence:* the unauthenticated-rejection scenario above, asserted
  per new endpoint.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** Groups are ordinary tenant-member data, not an
  admin or config surface: any member of the tenant may create, rename and delete them. The item is
  triggered by admin surfaces such as the AD-05 message catalog, which this feature does not touch.
- [ ] **C-04 · Personal data minimization** — **n/a.** A group stores a user-supplied name and a domain;
  no user or tenant identity data is collected or returned beyond the ownership already carried by every
  entity. The audit entry (C-10) records the acting identity, which is that item's purpose, not incidental PII.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential, connection string or key.
- [x] **C-06 · Encryption in transit** — **applies (standing).** The new endpoints are served by the
  existing TLS ingress; no new transport surface. *Evidence:* deployment/ingress config (unchanged).
- [ ] **C-07 · Image upload safety** — **n/a.** This feature accepts and serves no image binaries. Type
  icons reached through the tree are references into the shipped FR-07 store, whose upload path already
  enforces content-type and size bounds (NFR-04); no new binary surface is introduced.
- [ ] **C-08 · Rich-text sanitization** — **n/a.** Group names are plain text and no rich-text value is
  stored or returned here. The tree carries names only; escaping them at render time is a `notebox-web`
  concern (AD-06). Task `details` remain governed by feat-007's dialect, untouched by this feature.
- [x] **C-09 · Localization completeness** — **applies.** Every new rejection — required name, duplicate
  name, immutable domain, wrong-domain assignment — has en + pt catalog values. *Evidence:* the pt-locale
  scenario plus the per-locale catalog coverage check (NFR-02).
- [x] **C-10 · Audit trail for irreversible & admin actions** — **applies.** Deleting a group is an
  irreversible delete under BR-05 and is recorded who/what/when, following the deletion-audit pattern
  feat-010 established. Un-grouping the members is the stated, documented consequence of that same
  explicit action, not a separate silent mutation. Creating and renaming a group are reversible and are
  not audited. *Evidence:* the audit scenario above.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account or tenant management. Groups are
  tenant-owned data and are removed with the tenant through the path that already covers every entity.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** Groups define no fields, so no
  Secret-flagged value is stored, updated or revealed here.

## Open Questions
- **None open.** Three ambiguities were surfaced while writing this spec and all three were decided by
  the human on 2026-08-22, before any scenario was written:
  - **OQ-23** — tree composition: **type → group**; groups hold records and tasks (FR-08's literal
    wording), the type is the primary axis under Annotations (C28), and the tree stops at group nodes.
  - **OQ-24** — deleting a non-empty group **un-groups its members**; no record or task is deleted.
    A deliberate divergence from OQ-14, whose block rule guards against orphaned records.
  - **OQ-25** — sibling ordering is **name ascending**, case-insensitive, id tiebreak, with **Ungrouped
    pinned last**. A deliberate divergence from OQ-20/OQ-21, whose recency rule protects *page*
    stability that an unpaginated tree does not need.
- **Amended after audit Round 1 (2026-08-24, F-01):** the nesting scenario said *"the request is
  rejected"*. It is not — a stated parent yields 201 and is dropped, because unknown JSON properties
  are ignored project-wide. OQ-04's substance was never at risk (no parent column, none stored, none
  returned, and `GroupTest` fails if an accessor appears), so the scenario was corrected to match
  the codebase rather than deserialization changed for every endpoint.
- Refinements taken under the standing **OQ-09** delegation (*"feature specs may refine"*), consistent
  with the feat-010/feat-012 precedent: group name required, ≤ 120 chars, unique per tenant per domain;
  group domain immutable after creation; group and Ungrouped nodes appear only when non-empty while type
  nodes are always present.
