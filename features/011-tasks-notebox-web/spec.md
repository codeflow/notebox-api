# Feature — Tasks & subtasks UI with derived progress (ADF Fusion theme)

**ID:** features/011-tasks-notebox-web
**User Story:** US-4.1
**Version:** v1
**Status:** Approved (human approval 2026-08-14)
**Date:** 2026-08-14
**Project:** notebox-web (satellite; react/next)

## Origin
- **User Story:** US-4.1 — the **web half**: the tasks list (design screen 15, its US-4.1 subset) and
  the task detail with subtasks (screen 16, its US-4.1 subset), wired to feat-010's contract. The
  derived-dates column/span, the card link and the rich-text details shown on those screens belong to
  **US-4.2** and are out of scope here.
- **FRs covered (client obligations only):** FR-10 — task CRUD UI with name + priority and the
  **read-only derived status**; FR-11 — subtask management UI whose done-checkbox drives the visible
  progress. The client re-runs no server rule (BR-06 lives in the API).
- **NFRs bound:** NFR-08 (pager off `PageDto` facts: default 50, max 200, newest-first as served).
- **BRs bound:** BR-06 (status is displayed, never editable, never sent), BR-05 (deletes are explicit,
  confirmed acts), BR-08/C-09 (all system strings en/pt).
- **Primary source:** the **feat-010 contract**
  (`features/010-tasks-notebox-api/contracts/tasks.md` — 8 endpoints, `TaskDto`/`TaskListItemDto`/
  `SubtaskDto`, `PageDto`, the 11 `task.*` message keys, **PUT-replace semantics** and the
  **`@Null`-poisoned `status`**); design handoff **screen 15** (tasks list chrome, priority values,
  `af-progress` for status) and **screen 16** (subtask checkboxes recomputing % in place), filtered
  to their US-4.1 subset; the shipped web infrastructure (guard, `authFetch`, i18n, ADF Fusion theme,
  feat-009's `af-statusBar` pager idiom).
- **Contract precedence note:** the design README sketches a task payload
  (`{ subtasks[], derived: { percent, start, end } }`) as *behaviour reference only* — the wire truth
  is feat-010's `TaskDto` (`status` int, `subtasks[]`). Where they differ, the contract wins.

## Summary
A tenant member opens **Tasks** from the workspace navigation and sees the paginated list — name,
priority, and the derived status as an `af-progress` bar — newest first exactly as the server orders
it. They create a task by naming it and choosing a priority (no default — the closed set
Low/Medium/High/Critical), open a task to manage its subtasks, and tick/untick subtask checkboxes;
each mutation sends the **full subtask object** (PUT-replace) and repaints the progress from the
**returned parent task**, so the % always shows the server's number, never a client computation.
Subtasks carry optional start/end dates (start ≤ end enforced server-side, surfaced localized).
Deleting a task (with its subtasks) or a subtask is an explicit, confirmed act. Status is read-only
everywhere and never serialized into any request. All strings en/pt.

## Scope
- **In:**
  - **Route & navigation:** a `tasks` route behind the existing auth guard, reachable from the
    workspace navigation (the chrome's Tasks entry, per screens 05/15); a list row opens the task
    detail (`tasks/[id]`).
  - **Tasks list (screen 15, US-4.1 subset):** columns name · priority · status as `af-progress`
    with the integer percent (OQ-22 display: the server's number verbatim); rows exactly as served —
    newest first, the client never re-sorts; the `af-statusBar` pager off `PageDto` (`page`, `size`,
    `total`), same idiom feat-009 shipped; empty state ("no tasks yet" + New task action); loading
    state.
  - **Create / edit task:** a form with name (required) and priority (required, the closed set,
    **no preselected default** — absence must be a deliberate choice); server validation surfaced
    per field, localized (`task.name.required`, `task.priority.required`, `task.priority.invalid`,
    `task.name.too_long`); edit changes name/priority only.
  - **Status is read-only, structurally:** no status input exists in any form; no request body ever
    contains a `status` key (the API would reject it with `task.status.not_writable` — the client
    must never trigger that key in normal operation).
  - **Task detail (screen 16, US-4.1 subset):** the task's name, priority, progress bar, and its
    subtask list — each subtask showing name, optional start/end dates, and the done checkbox.
  - **Subtask management:** add (name required; dates optional, each independently; server's
    start ≤ end rejection surfaced localized), edit (same form), tick/untick the done checkbox.
    **Every subtask write sends the full object** (name, both dates, done) — PUT-replace semantics:
    an omitted field clears, so the client is responsible for echoing what it doesn't change.
  - **Progress repaint from the response:** every subtask mutation (add / edit / tick / untick /
    delete) repaints the task's progress and subtask list from the **returned parent `TaskDto`** —
    one round trip, no client-side percent math, no separate refetch.
  - **Explicit deletes (BR-05):** deleting a subtask asks confirmation, then repaints from the
    response; deleting a task states that its subtasks go with it, asks confirmation, then returns
    to the list.
  - **Errors & not-found:** API errors surfaced with their localized message; a foreign or missing
    task id shows the established not-found idiom (the server's 404 is indistinguishable by design).
  - **Localization:** every system string (headers, buttons, empty/loading states, confirmations)
    is a catalog entry in en + pt; server messages arrive already localized via `Accept-Language`.
- **Out:**
  - **US-4.2 web scope previewed by screens 15/16:** derived min/max **date** columns and span,
    the **card** link/value object, **rich-text details** editor (and with it any C-08 client
    concern), the Notes drawer interaction. The screens show them; this slice does not build them.
  - **Groups / navigation tree** placement of tasks (E3; FR-08/09) — the tasks-in-flight table of
    screen 05's home is E3/US-4.2-adjacent chrome, not this slice.
  - **Server behaviors** — ordering, rounding, validation, audit, cascade: feat-010's, consumed
    not re-implemented.
  - **Client-side sorting/filtering** of the list — the server's order is the order (a QBE-style
    filter, if wanted later, is a new declared scope as feat-009's was).

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-10 Tasks list and lifecycle in the UI
  Scenario: The list renders the served page, newest first
    Given the API serves page 0 with tasks "T3", "T2", "T1" in that order and total 3
    When a member opens the Tasks page
    Then three rows render in exactly that order
    And each row shows the task name, its priority and an af-progress bar at the served status

  Scenario: The pager reflects PageDto facts
    Given the API serves total 51 with page size 50
    When the member looks at the pager after opening Tasks
    Then it reports 51 total and offers a next page
    And requesting the next page fetches page 1 from the API

  Scenario: Empty state
    Given the member's tenant has no tasks
    When they open the Tasks page
    Then the localized "no tasks yet" state renders with a New task action

  Scenario: Create a task with the minimum shape
    Given the member opens the New task form
    When they submit name "Migrate broker" and priority High
    Then the client POSTs exactly name and priority — no status key in the body
    And on the 201 the list shows "Migrate broker" with progress 0%

  Scenario: Priority starts unchosen
    Given the member opens the New task form
    Then the priority control has no preselected value
    And submitting without choosing surfaces the localized task.priority.required message at the field

  Scenario: Server validation surfaces at the fields
    Given the member submits the New task form with a blank name
    Then the localized task.name.required message renders at the name field
    And nothing navigates away

  Scenario: Edit changes name and priority only
    Given a task "Migrate broker" (High) at 50%
    When the member renames it to "Migrate RabbitMQ" with priority Critical and saves
    Then the client PUTs name and priority — no status key
    And the row shows the new name and priority with the progress bar still at 50%

  Scenario: Deleting a task is explicit and warns about its subtasks
    Given a task with 3 subtasks
    When the member chooses Delete
    Then a confirmation states the task and its subtasks will be removed permanently
    And only after confirming does the client DELETE and return to the list

Feature: FR-11 Subtasks drive the visible progress
  Scenario: Detail renders subtasks and the derived status
    Given a task with subtasks "Inventory" (done) and "Cutover" (not done, 2026-09-01 → 2026-09-10)
    When the member opens the task detail
    Then the progress bar shows 50%
    And "Inventory" shows a ticked checkbox
    And "Cutover" shows its start and end dates in their own columns, unswapped

  Scenario: Ticking a checkbox sends the full subtask and repaints from the response
    Given the detail of a task at 25% with subtask "s2" not done (with dates set)
    When the member ticks "s2"
    Then the client PUTs the full subtask object — name, both dates, done true — nothing omitted
    And the progress bar repaints to exactly the status returned by the API, with no client math

  Scenario: Unticking recomputes downward
    Given a task at 100% with two done subtasks
    When the member unticks one
    Then the progress bar shows the returned 50%

  Scenario: A non-integer proportion displays the server's integer
    Given a task with 3 subtasks, 1 done
    When the member opens its detail
    Then the progress bar shows 33% — the server's rounded integer, not 33.33

  Scenario: Adding a subtask
    Given the detail of a task at 100% with one done subtask
    When the member adds subtask "Validate backups" without dates
    Then on the 201 the subtask list shows both rows and the progress bar the returned 50%

  Scenario: The server's date-range rejection surfaces localized
    Given the member adds subtask "Cutover" starting 2026-09-10 and ending 2026-09-01
    Then the localized task.subtask.date.invalid message renders at the dates
    And no subtask is added

  Scenario: Deleting a subtask is explicit and repaints
    Given a task at 25% whose only done subtask is "Inventory"
    When the member deletes "Inventory" and confirms
    Then the subtask list drops the row and the progress bar shows the returned 0%

Feature: Cross-cutting UI obligations
  Scenario: The Tasks route is guarded
    Given no authenticated session
    When a client navigates to the tasks route
    Then the established auth guard redirects to sign-in

  Scenario: A foreign or missing task id shows not-found
    Given the API answers 404 task.not_found for task id X
    When the member navigates to that task's detail
    Then the established not-found idiom renders with the localized message

  Scenario: Portuguese locale end to end
    Given the member's locale resolves to pt
    When they open Tasks and submit a blank-name task
    Then every system string on the page is the Portuguese catalog text
    And the field error is the API's Portuguese task.name.required message
    And no raw key or blank string renders anywhere
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies (client obligation).** All task calls go through the
  established authenticated fetch; the UI never caches or renders another tenant's data. *Evidence:*
  the guarded-route scenario + reuse of the shipped `authFetch`/guard seam (no new fetch path).
- [x] **C-02 · Authenticated by default** — **applies.** The tasks routes sit behind the existing
  guard. *Evidence:* the redirect scenario.
- [ ] **C-03 · Least-privilege** — **n/a.** No admin surface; task CRUD is member-level.
- [ ] **C-04 · Personal data minimization** — **n/a.** No identity data beyond the session already
  established.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential or config.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Same origin/TLS posture; no new
  transport.
- [ ] **C-07 · Image upload safety** — **n/a.** No images in this slice.
- [ ] **C-08 · Rich-text sanitization** — **n/a.** No rich text in this slice — the task `details`
  editor is US-4.2, where the client-side defence-in-depth obligation returns.
- [x] **C-09 · Localization completeness** — **applies.** Every new UI string in en + pt; server
  messages consumed localized. *Evidence:* the pt end-to-end scenario + the satellite's message
  catalogs.
- [ ] **C-10 · Audit trail** — **n/a client-side.** Deletion audit is feat-010's; the UI's
  obligation is the explicit confirm (BR-05), covered by the two delete scenarios.
- [ ] **C-11 · Data retention** — **n/a.** No account management.
- [ ] **C-12 · Secret values** — **n/a.** Tasks have no secret fields.

## Open Questions
- None. The two contract-level OQs this story raised (OQ-21 ordering, OQ-22 rounding) were resolved
  2026-08-13 and are consumed here as display facts. Presentation details not fixed by screens 15/16
  (e.g. exact priority styling) are the plan's to settle against the design handoff — behaviour, not
  contract.
