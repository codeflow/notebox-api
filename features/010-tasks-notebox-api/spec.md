# Feature — Tasks with subtasks and derived progress

**ID:** features/010-tasks-notebox-api
**User Story:** US-4.1
**Version:** v1
**Status:** Approved (human approval 2026-08-13; OQ-21/OQ-22 answered the same day and folded in)
**Date:** 2026-08-13

## Origin
- **User Story:** US-4.1 — *As a tenant member, I want tasks with subtasks whose completion drives the task's progress, so status reflects real work.*
- **FRs covered:**
  - **FR-10 (partial):** create/read/update/delete tasks with **name** and **priority ∈ {Low, Medium, High, Critical}**, and **status as a derived completion %**. The remaining FR-10 attributes — optional card, optional rich-text details, derived dates — are US-4.2's scope (FR-12/FR-13/FR-14) and complete there.
  - **FR-11 (partial):** manage subtasks (**name, start/end dates, done flag**); completing/adding subtasks recomputes the parent task's status %. The subtask's optional **card** is defined by FR-13 and ships with US-4.2.
- **BRs bound:** BR-06 (task status is derived, never assigned — 0% with no subtasks), BR-05 (destructive deletions are explicit and irreversible).
- **NFRs bound:** NFR-08 (task listing paginated: default **50**, max **200**; ordered **newest first** — `createdAt` desc, id tiebreak — *decisão humana 2026-08-13, OQ-21*), NFR-01 (no cross-tenant access), NFR-02/C-09 (localized messages en+pt), NFR-06 (OpenAPI in sync), NFR-07 (structured logging, correlation id).
- **Primary source:** PRD v2 §2 (US-4.1 ← INTAKE C19, C22, C23, C24), §3.1 (FR-10, FR-11), §3 Validation baseline (OQ-09 — *"task and annotation names required"*; specs may refine), §4 acceptance example 2 (subtask completion → 50%); constitution BR-05/BR-06, C-01/C-02/C-09/C-10.
- **Consumes:** feat-001 identity & tenancy — JWT auth and the tenant-scoped access pattern (AD-03 choke point). Tasks are independent of annotation types; no feat-003/005 contract is consumed.
- **Deferred sibling:** BR-07 (task dates derive from subtasks) binds **US-4.2/FR-12**, not this feature — subtask dates are *stored* here so FR-12 can later derive from them, but no task-level date exists yet.

## Summary
A tenant member manages **tasks** — name plus a priority from the closed set {Low, Medium, High,
Critical} — and each task's **subtasks** (name, optional start/end dates, a done flag). The task's
**status is a completion percentage computed from its subtasks**: the proportion marked done, 0% when
there are none (BR-06). Every subtask change — add, complete, un-complete, delete — recomputes the
parent's status; a client can never write the status directly. Task and subtask deletion is explicit,
irreversible and audited (BR-05, C-10). The task listing is paginated (NFR-08). API contract only —
the UI is feat-011.

## Scope
- **In:**
  - **Task CRUD**, tenant-scoped behind JWT auth (C-01/C-02): create and update with a required
    **name** and a required **priority** ∈ {Low, Medium, High, Critical} (no default is defined in the
    PRD, so the client must choose — refinement under the OQ-09 delegation); read-one returning the
    task with its derived status and its subtasks; paginated listing returning each task with its
    derived status.
  - **Subtask management** within a task (FR-11): create/update/delete with a required **name**, an
    optional **start date** and **end date** (each independently optional; when both are present,
    **start ≤ end** — refinement under OQ-09), and a **done flag** togglable in both directions.
  - **Derived status (BR-06):** status % = proportion of subtasks marked done; **0% when the task has
    no subtasks**; recomputed on every subtask add, done-toggle (both directions) and delete. Any
    client attempt to supply a status value on task create or update is **rejected with a localized
    validation error** (enforcement refinement of BR-06 under OQ-09 — never silently ignored).
    Status is an **integer percent, rounded half up** — 1/3 → 33, 2/3 → 67, 1/8 → 13 *(decisão
    humana 2026-08-13, OQ-22)*.
  - **Pagination (NFR-08):** default page size **50**; a requested size above **200 is rejected** with
    a localized validation error (same contract feat-008 delivered for record listings). Listing
    order is **newest first** — `createdAt` descending, ties broken by id, mirroring the OQ-20
    record-listing contract *(decisão humana 2026-08-13, OQ-21)*.
  - **Explicit, audited deletion (BR-05, C-10):** deleting a subtask removes that subtask and
    recomputes the parent's status; deleting a task removes the task **together with its subtasks** —
    the confirmed delete of the task is itself the explicit act, and BR-05 bars deletion only as a
    *silent side effect of another operation*, which this is not. Both deletions write an audit entry
    (who/what/when).
  - **Localized errors (C-09):** every rejection carries a dot-namespaced message key resolvable in
    en and pt, following the established validation/domain-error envelope conventions.
  - **OpenAPI (NFR-06):** every new endpoint documented, in sync, in the same feature.
- **Out:**
  - **Task dates derived from subtasks** (FR-12, BR-07), the **card** value object on task or subtask
    (FR-13), and **rich-text details** (FR-14) — all US-4.2. Consequently **C-08 does not apply**
    here: this slice stores no rich text.
  - **Groups and the navigation tree** (FR-08/FR-09, US-3.1) — tasks are created ungrouped; grouping
    arrives with E3.
  - The **task UI** — feat-011-tasks-notebox-web.
  - **Redis caching** of task reads (NFR-03) — a plan-level optimization; correctness must not depend
    on it.
  - **Secret values / encryption** (FR-18, C-12) — tasks define no secret-flagged fields.
  - Any per-tenant task quota, task ordering *within* the model (no position field is named by any
    FR), or bulk operations — no FR names them.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-10 Task CRUD with derived status
  Scenario: Create a task with the minimum shape
    Given an authenticated member of tenant A
    When they create a task named "Migrate broker" with priority High
    Then the task is created in tenant A
    And its status is 0% because it has no subtasks

  Scenario: A task without a name is rejected
    Given an authenticated member of tenant A
    When they create a task with a blank name and priority Low
    Then the request is rejected with a localized required-name validation error

  Scenario: A priority outside the closed set is rejected
    Given an authenticated member of tenant A
    When they create a task named "Ops review" with priority "Urgent"
    Then the request is rejected with a localized invalid-priority validation error

  Scenario: A client cannot write the status directly
    Given a task of tenant A with 2 subtasks, none done
    When a member of tenant A updates the task supplying a status of 80%
    Then the request is rejected with a localized validation error naming the status as non-writable
    And the task's status remains 0%

  Scenario: Update name and priority without touching progress
    Given a task of tenant A named "Migrate broker" with priority High and 2 of 4 subtasks done
    When a member of tenant A renames it to "Migrate RabbitMQ" with priority Critical
    Then the task carries the new name and priority
    And its status is still 50%

  Scenario: Read one task with its subtasks and derived status
    Given a task of tenant A with subtasks "Inventory" (done) and "Cutover" (not done)
    When a member of tenant A reads the task
    Then the response carries the task's name, priority and status 50%
    And both subtasks with their name, dates and done flag

  Scenario: Deleting a task removes it and its subtasks, audited
    Given a task of tenant A with 3 subtasks
    When a member of tenant A explicitly deletes the task
    Then the task and its 3 subtasks no longer exist
    And an audit entry records who deleted which task and when
    And reading the task afterwards is indistinguishable from a task that never existed

Feature: FR-11 Subtasks drive the parent task's progress
  Scenario: Completing a subtask updates progress (PRD §4 example)
    Given a task with 4 subtasks, 1 completed
    When a client marks a second subtask done
    Then the task status is 50%

  Scenario: Adding a subtask recomputes progress downward
    Given a task with 1 subtask which is done, so status is 100%
    When a member adds a new subtask "Validate backups" not done
    Then the task status is 50%

  Scenario: Un-completing a subtask recomputes progress
    Given a task with 2 subtasks, both done, so status is 100%
    When a member marks one subtask as not done
    Then the task status is 50%

  Scenario: Deleting a subtask recomputes progress
    Given a task with 4 subtasks of which only "Inventory" is done, so status is 25%
    When a member deletes the subtask "Inventory"
    Then the task has 3 subtasks and status 0%
    And an audit entry records the subtask deletion

  Scenario: A subtask without a name is rejected
    Given a task of tenant A
    When a member adds a subtask with a blank name
    Then the request is rejected with a localized required-name validation error

  Scenario: A subtask with start after end is rejected
    Given a task of tenant A
    When a member adds a subtask "Cutover" starting 2026-09-10 and ending 2026-09-01
    Then the request is rejected with a localized date-range validation error

  Scenario: A non-integer proportion rounds to an integer percent
    Given a task with 3 subtasks, 1 completed
    When a member reads the task
    Then the task status is 33% (integer percent, rounded half up — OQ-22)

Feature: NFR-08 Paginated task listing
  Scenario: The default page size is 50
    Given tenant A has 51 tasks
    When a member lists tasks without pagination parameters
    Then exactly 50 tasks are returned, each with its derived status
    And the response signals that more exist

  Scenario: A requested size above 200 is rejected
    Given an authenticated member of tenant A
    When they request a task page of size 201
    Then the request is rejected with a localized pagination validation error

  Scenario: The listing is ordered newest first
    Given tenant A created task "T1", then "T2", then "T3" at distinct instants
    When a member lists tasks
    Then the rows come in the order T3, T2, T1 — createdAt descending, ties broken by id (OQ-21)

Feature: Tenant isolation and authentication (C-01, C-02, NFR-01)
  Scenario: Unauthenticated access is rejected
    Given no authentication token
    When a client lists tasks
    Then the request is rejected as unauthenticated

  Scenario: A foreign tenant's task is indistinguishable from a missing one
    Given a task belonging to tenant B
    When an authenticated member of tenant A reads, updates or deletes that task
    Then the outcome is exactly the outcome for a task id that does not exist

  Scenario: Listings never leak across tenants
    Given tenant A has 2 tasks and tenant B has 3 tasks
    When a member of tenant A lists tasks
    Then exactly tenant A's 2 tasks are returned

  Scenario: Validation messages resolve in the caller's locale (C-09)
    Given an authenticated member of tenant A whose locale resolves to pt
    When they create a task with a blank name
    Then the rejection message is the Portuguese catalog text for the required-name key
    And no raw message key or blank string reaches the client
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies.** All task/subtask access is tenant-owned. *Evidence:* the cross-tenant scenarios above + the AD-03 repository choke point; automated cross-tenant test per endpoint (NFR-01).
- [x] **C-02 · Authenticated by default** — **applies.** Every new endpoint requires an authenticated tenant context; none is public. *Evidence:* the unauthenticated-rejection scenario as a security test (401/403 family).
- [ ] **C-03 · Least-privilege authorization** — **n/a.** No admin/config surface: task CRUD is ordinary tenant-member work; no elevated role is involved.
- [ ] **C-04 · Personal data minimization** — **n/a.** Tasks/subtasks store no user-identity data beyond the standard tenant scoping and audit actor reference already established.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential, connection string or key is introduced.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Served under the existing TLS ingress; no new transport surface. *Evidence:* deployment/ingress config (unchanged).
- [ ] **C-07 · Image upload safety** — **n/a.** This slice accepts and serves no image binaries (rich-text embeds arrive with US-4.2).
- [ ] **C-08 · Rich-text sanitization** — **n/a.** No rich text is stored or returned in this slice; task `details` (FR-14) is US-4.2, where C-08 applies in full.
- [x] **C-09 · Localization completeness** — **applies.** Every new validation/domain message key has en + pt catalog values. *Evidence:* the pt-locale scenario + catalog coverage check (NFR-02).
- [x] **C-10 · Audit trail for irreversible & admin actions** — **applies.** Task and subtask deletions are irreversible (BR-05). *Evidence:* audit-entry assertions in the two deletion scenarios.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account/tenant management; task deletion itself is the user-facing removal path and is covered by BR-05/C-10.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** Tasks define no Secret-flagged fields.

## Open Questions
- **OQ-21 — Task listing sort order** — ✅ **resolved 2026-08-13:** newest first (`createdAt` desc, id tiebreak), mirroring OQ-20 for one uniform listing contract. Folded into the scenarios above.
- **OQ-22 — Task status representation** — ✅ **resolved 2026-08-13:** integer percent, rounded half up (1/3 → 33); 0% with no subtasks (BR-06). Folded into the scenarios above.
