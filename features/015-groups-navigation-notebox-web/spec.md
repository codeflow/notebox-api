# Feature — Item groups and navigation-tree data (web)

**ID:** features/015-groups-navigation-notebox-web
**User Story:** US-3.1
**Version:** v1
**Status:** Approved (human approval 2026-08-24)
**Date:** 2026-08-24
**Project:** `notebox-web` (react/next) — routed satellite

## Origin
- **User Story:** US-3.1 — *As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace.*
- **FRs covered (client half):**
  - **FR-08:** manage groups and assign an annotation or task to at most one group, from the UI.
  - **FR-09:** render the navigation tree — the Navigator (INTAKE **C28**), with click-to-detail routing (**C30**).
- **BRs bound:** BR-01/BR-02 (every call carries the session's tenant; nothing client-side widens it), BR-05 (deleting a group is explicit — the UI confirms, the API performs).
- **NFRs bound:** NFR-01 (no cross-tenant surface), NFR-02/C-09 (every new string in en + pt), NFR-08 (the grids the tree routes into keep the shipped pager).
- **Primary source:** PRD v2 §2 (US-3.1 ← C28, C29, C30), §3.1 (FR-08/FR-09); **the satellite's design reference is the visual source of truth** — `notebox-web/design/handoff` **screen 05** (workspace home: splitter + accordion Navigator) and **screen 14** (Groups management); `catalogs/open-questions.md` OQ-04, **OQ-23 (as clarified 2026-08-24)**, OQ-24, OQ-25, **OQ-27**.
- **Consumes:** feat-014's shipped contract — `features/014-groups-navigation-notebox-api/contracts/groups-navigation.md` (`/groups` CRUD, `GET /navigation`, the `group` filter on both listings, `groupId` on both item surfaces). Client-side: the shipped `authFetch`/`apiClient` stack, `lib/i18n/useTranslation` with the `en`/`pt` `MessageKey` catalogs, `src/styles/adf-fusion.overrides.css` for all project CSS (never `adf-fusion.css`), Vitest + Testing Library + MSW.
- **⚠️ Depends on an API slice that does not exist yet:** **OQ-27** adds aggregate counts to `/navigation` and the group listing. feat-014 is merged and closed, so those fields ship as a separate API feature that must land **before** this feature's `implement`. See *Out* and the gate report.

## Summary
The workspace gains its **Navigator** (C28): a left accordion tree, rendered from one
`GET /navigation` call, with *Annotations* and *Tasks* roots. Per design screen 05 the tree nests
**group → type** — groups above, the types occupying them beneath — which the client derives by
inverting the API's type↔group payload; the API is unchanged (OQ-23 as clarified). Clicking a node
routes to the grid it stands for, filtered by that group (C30). A separate **Groups** screen
(design 14) gives the two flat, per-domain group lists their CRUD, and item forms gain a group
selector. The API is already shipped; this feature is its consumer.

## Scope

- **In:**
  - **The Navigator (FR-09, C28, design 05).** A left panel with an accordion *Workspace* section
    holding the tree, populated from a single `GET /navigation`. Two roots — **Annotations** and
    **Tasks**. Under Annotations the nodes are **groups**, each expanding to the annotation
    **types** whose records occupy it; under Tasks the nodes are groups directly. The synthetic
    **Ungrouped** node is `groupId: null` on the wire and is **labelled from the client's own
    catalog** (`nav.ungrouped`, en + pt), never from the payload — the API deliberately sends no
    label (AD-05/AD-06). Sibling order follows the payload (OQ-25: name asc, Ungrouped last), and
    the client does not re-sort.
  - **Group → type inversion (OQ-23 as clarified).** The payload is `type → [groups]`; the design
    renders `group → [types]`. The client inverts it. A type appearing under several groups is
    normal and expected — it means that type has records in each.
  - **Click-to-detail routing (C30).** Selecting a **type node under a group** opens that type's
    records grid filtered to that group; a **task group node** opens the tasks grid filtered to it;
    an **Ungrouped** node opens the same grid filtered to the ungrouped. Filtering rides the
    shipped `group` query parameter (`<uuid>` | `none`) — no new listing surface. The selected node
    is visibly current, and the route is shareable (the filter lives in the URL, not in memory).
  - **Groups management (FR-08, design 14).** An Administration screen with **two independent
    panels** — Annotation groups and Task groups — each listing its namespace's groups with
    **New**, **Rename** and **Delete**. Creation states the name; the domain comes from the panel,
    never from a control, because the namespaces are separate (OQ-04) and a domain picker would
    invite the cross-domain error the API rejects. Deleting confirms first and states plainly that
    **the group is removed and its items become ungrouped — nothing is deleted** (OQ-24); the
    dialog is the BR-05 confirmation the API deliberately does not perform.
  - **Assigning an item to a group (FR-08).** The annotation-record form and the task form each
    gain an optional group selector, offering only that domain's groups plus a "no group" choice.
    **Every update sends `groupId` explicitly** — including `null` when cleared — because the API
    is replace-not-patch and an omitted key clears the group. This is the feat-012/feat-013
    full-echo builder discipline, and it is what closes feat-014's recorded rollout hazard.
  - **Aggregate counts (OQ-27).** Tree nodes show their member count and the group tables show
    their per-group aggregates, exactly as designs 05 and 14 draw them, read from the API fields
    OQ-27 adds. **Consumed, never computed client-side.**
  - **Localization (C-09).** Every new string resolves in en and pt through the shipped catalogs;
    server rejections are displayed as the API localized them.
  - **Styling.** ADF Fusion classes as the design uses them (`af-panelLeft`, `af-accSection`,
    `af-tree`, `af-panelCollection`, `af-dialog`); anything the sheet lacks becomes an `nb-*` class
    in `adf-fusion.overrides.css`. `adf-fusion.css` is never edited.

- **Out:**
  - **Any API change.** This feature consumes contracts only. The OQ-27 count fields are a
    **prerequisite API slice**, not work performed here.
  - **Nested groups, multi-group membership, grouping types** — closed by OQ-04 and OQ-23; no UI
    affordance may imply otherwise.
  - **Drag-and-drop assignment** and the design's `af-selectManyShuttle` bulk-assignment control —
    the API exposes no bulk surface (feat-014 scoped it out), so a shuttle would need a request per
    item and would misrepresent a bulk operation. Deferred with a stated reason rather than faked.
  - **The rest of design screen 05** — branding bar, menu bar, toolbar, breadcrumbs, summary
    `af-panelBox` row, "Tasks in flight" table, right drawer dock, footer, splitter drag-to-resize,
    "Recently viewed" and "Saved searches" accordion sections. Screen 05 is the *whole* workspace
    home; only the Navigator belongs to FR-09. The rest has no FR and is not US-3.1's.
  - **Renaming/deleting a group from the tree** — the tree is navigation; management is screen 14.
  - **Group sort/filter controls, pagination of the group tables** — the design shows none, and the
    group listing's own paging (NFR-08) is not exercised at realistic group counts.
  - **Offline/optimistic updates, caching of the tree** — no requirement; the tree refetches.

## Acceptance criteria (Gherkin)

```gherkin
Feature: FR-09 The Navigator renders the tree group → type (C28, OQ-23 clarified)
  Scenario: Groups sit above the types that occupy them
    Given the navigation payload says type "RabbitMQ" occupies groups "alpha" and "gamma"
    And type "Kafka" occupies group "alpha"
    When a member opens the workspace
    Then the Annotations root shows the group nodes "alpha" and "gamma"
    And expanding "alpha" reveals both "Kafka" and "RabbitMQ"
    And expanding "gamma" reveals "RabbitMQ"

  Scenario: A type occupying two groups appears under each
    Given type "RabbitMQ" occupies both "alpha" and "gamma"
    When a member expands both group nodes
    Then "RabbitMQ" appears under "alpha" and under "gamma"
    And neither occurrence is marked as a duplicate or an error

  Scenario: The Ungrouped node is labelled by the client, not the payload
    Given the payload carries a node with a null groupId and a null name
    When a member opens the workspace in English
    Then that node reads "Ungrouped"
    And when the same member switches to Portuguese it reads "Sem grupo"
    And no node renders an empty or blank label

  Scenario: Sibling order comes from the payload and is not re-sorted
    Given the payload lists the groups in the order "alpha", "Beta", "gamma", then the null node
    When the tree renders
    Then the nodes appear in exactly that order, with Ungrouped last

  Scenario: A tenant with nothing sees both roots and no children
    Given the payload carries two empty roots
    When a member opens the workspace
    Then the Annotations and Tasks roots are both present and both empty
    And no error or empty-state failure is shown

  Scenario: The tree is fetched once per view, not per node
    Given a workspace with 3 groups and 4 types
    When the Navigator renders
    Then exactly one request to the navigation endpoint has been made

Feature: FR-09 Clicking a node opens the grid it stands for (C30)
  Scenario: A type under a group opens that type's records filtered to the group
    Given the Navigator shows "RabbitMQ" under the group "alpha"
    When a member clicks "RabbitMQ"
    Then the annotation records grid for "RabbitMQ" is shown, filtered to "alpha"
    And the request carried that group's id as the group filter
    And the node is shown as the selected one

  Scenario: A task group node opens the tasks grid filtered to it
    Given the Navigator shows the task group "Q3 Migration"
    When a member clicks it
    Then the tasks grid is shown, filtered to that group

  Scenario: An Ungrouped node filters to the ungrouped, not to everything
    Given the Navigator shows an Ungrouped node under Tasks
    When a member clicks it
    Then the tasks grid is shown and the request carried the ungrouped filter
    And tasks that belong to a group are not listed

  Scenario: The filtered view is shareable
    Given a member has clicked a group node
    When the resulting URL is opened in a fresh session by a member of the same tenant
    Then the same filtered grid is shown, with the same node selected

Feature: FR-08 Managing groups in two independent namespaces (design 14)
  Scenario: The screen shows one panel per namespace
    Given a member opens the Groups screen
    Then there is an Annotation groups panel and a Task groups panel
    And each lists only its own namespace's groups

  Scenario: Creating a group takes its domain from the panel, not from a control
    Given a member is on the Groups screen
    When they create "Brokers" from the Annotation groups panel
    Then the request stated the annotation domain
    And no domain picker was presented

  Scenario: The same name is accepted in the other namespace
    Given "Brokers" already exists as an annotation group
    When a member creates "Brokers" from the Task groups panel
    Then it is created and both panels show a "Brokers" row

  Scenario: A duplicate name in the same namespace shows the server's message
    Given "Brokers" already exists as an annotation group
    When a member creates "Brokers" again from that panel
    Then the API's localized duplicate-name message is shown
    And no group was created

  Scenario: Renaming a group updates it in place
    Given the annotation group "Brokers" exists
    When a member renames it to "Message Brokers"
    Then the panel shows the new name

  Scenario: Deleting confirms, and says what actually happens
    Given the annotation group "Brokers" holds 12 records
    When a member chooses Delete
    Then a confirmation states that the group is removed and its items become ungrouped
    And states that no annotation is deleted
    And nothing is sent until the member confirms

  Scenario: Confirming the delete removes only the group
    Given the confirmation above is shown
    When the member confirms
    Then the group disappears from the panel
    And the Navigator no longer shows that group node

Feature: FR-08 Assigning an item to a group from its form
  Scenario: The selector offers only the item's own namespace
    Given annotation groups "Brokers" and task groups "Q3 Migration" exist
    When a member opens an annotation record form
    Then the group selector offers "Brokers" and a no-group choice
    And it does not offer "Q3 Migration"

  Scenario: Assigning a group and saving
    Given a member is editing a record with no group
    When they select "Brokers" and save
    Then the request carried that group's id
    And reopening the record shows "Brokers" selected

  Scenario: Clearing the group sends null, never an omitted key
    Given a member is editing a record assigned to "Brokers"
    When they select the no-group choice and save
    Then the request body carried the group key with a null value
    And the record is shown as ungrouped

  Scenario: An untouched group survives an unrelated edit
    Given a member is editing a record assigned to "Brokers"
    When they change only the record's name and save
    Then the request still carried that group's id
    And the record is still in "Brokers"

Feature: OQ-27 Aggregates are displayed as received
  Scenario: Tree nodes show their counts
    Given the payload gives the node "Service Endpoint" a count of 12
    When the tree renders
    Then that node shows 12
    And no additional request was made to derive it

  Scenario: The group tables show their per-domain aggregates
    Given the group listing reports 18 annotations and 2 types used for "Infrastructure"
    And it reports 8 tasks at an average status of 58 for "Migration"
    When a member opens the Groups screen
    Then the Annotation groups panel shows 18 and 2 for "Infrastructure"
    And the Task groups panel shows 8 and a 58% status indicator for "Migration"

Feature: Standing guarantees (C-01, C-02, C-09)
  Scenario: The Navigator and Groups screen are behind the session guard
    Given no active session
    When either is opened directly by URL
    Then the shipped route guard sends the member to sign in
    And no navigation or group request was made

  Scenario: A failed tree load degrades without breaking the workspace
    Given the navigation request fails
    When the workspace renders
    Then a localized error is shown in the Navigator panel
    And the rest of the page still renders

  Scenario: Every new string resolves in both locales
    Given a member whose locale is pt
    When they open the Navigator and the Groups screen
    Then every label, button and confirmation is in Portuguese
    And no raw message key or blank string is rendered
```

## Compliance pre-flight
- [x] **C-01 · Tenant isolation** — **applies (client obligation).** Every new call goes through the
  shipped `authFetch`/`apiClient`; the tenant comes from the JWT and no client code widens it.
  *Evidence:* the guard scenario + no new fetch path outside the shipped stack.
- [x] **C-02 · Authenticated by default** — **applies.** The Navigator lives in the guarded `(app)`
  layout and the Groups screen is a guarded route. *Evidence:* the direct-URL guard scenario.
- [ ] **C-03 · Least-privilege authorization** — **n/a.** Groups are ordinary member data; the API
  imposes no elevated role, so the UI invents none. (Design 14 places the screen under an
  *Administration* tab — that is navigation placement, not authorization.)
- [ ] **C-04 · Personal data minimization** — **n/a.** No identity data beyond the existing session.
- [ ] **C-05 · Secrets never committed** — **n/a.** No new credential or config.
- [x] **C-06 · Encryption in transit** — **applies (standing).** Same origin and TLS posture; no new
  transport surface.
- [ ] **C-07 · Image upload safety** — **n/a.** No image is uploaded or rendered by this feature;
  the tree carries no icons from the API (feat-014 scoped them out).
- [ ] **C-08 · Rich-text sanitization** — **n/a.** Group names and node labels are plain text and are
  rendered as text by React's default escaping; no `dangerouslySetInnerHTML` is introduced.
- [x] **C-09 · Localization completeness** — **applies.** Every new string — including the client-owned
  `Ungrouped` label the API deliberately omits — exists in en and pt. *Evidence:* the pt scenario +
  the satellite's catalog coverage check.
- [ ] **C-10 · Audit trail** — **n/a client-side.** The API audits the group deletion; the UI only
  confirms it.
- [ ] **C-11 · Data retention & deletion path** — **n/a.** No account or tenant management.
- [ ] **C-12 · Encryption at rest for secret values** — **n/a.** Groups define no fields.

## Open Questions
- **None blocking this spec.** Two were settled before any scenario was written:
  - **OQ-23, clarified 2026-08-24** — its type→group ruling governs the **payload**; design screen 05's
    group→type nesting stands for the **UI**, produced by inverting client-side. No API change.
  - **OQ-27, resolved 2026-08-24** — the aggregate counts both design screens show are added to the
    API as additive fields.
- **⚠️ Sequencing, not an open question:** OQ-27's fields do not exist yet. feat-014 is merged and
  closed, so they need their own API slice, and it must ship **before this feature's `implement`**.
  The *Aggregates are displayed as received* scenarios are unimplementable until then; every other
  scenario here runs against what feat-014 already shipped.
- Deferred with a stated reason rather than silently dropped: the design's `af-selectManyShuttle`
  bulk assignment (the API exposes no bulk surface) and the non-Navigator two-thirds of screen 05
  (no originating FR).
