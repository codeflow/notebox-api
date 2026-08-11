# PRD — Notebox

**Version:** v2
**Date:** 2026-07-24
**Status:** Draft (awaiting approval)
**Type:** api · **Stack:** Java, Jakarta EE, JPA, CDI, MySQL, Redis
**Language:** English (en)

> The living source of truth for product intent. Every requirement cites an Origin
> (`INTAKE.md § <row>` or a human decision). Ungrounded items are `[TBD — OQ-NN]`, never invented.
> Keep IDs immutable across versions.

## 0. Changelog
| Version | Date | Main changes |
|---------|------|--------------|
| v1 | 2026-07-22 | Initial PRD from `definitions/INTAKE.md` + constitution + session decisions of 2026-07-22. |
| v2 | 2026-07-24 | Formalized the **Secret field** requirement (human decision 2026-07-23, OQ-15): new **FR-18** — Secret annotation values encrypted at rest + role-gated audited reveal, backed by new **BR-10 / AD-14 / C-12**. Recorded resolution of delivery-phase **OQ-11…OQ-16** (§8): OQ-14 refines type deletion (block while records exist); OQ-11/12/13/16 resolved as deferred future features. No existing ID changed. |

## 1. Vision, Goals & Scope

### 1.1 Vision
Notebox is a multi-tenant system that unifies two workspaces in one place: **structured, typed
annotations** (user-defined note "types" with typed fields — e.g. a *RabbitMQ* type with URL,
Environment, Description) and **task management** with subtasks, derived progress, and rich-text
detail. The product is bilingual (English + Portuguese) with runtime-editable system text.
`notebox-api` is the backend contract; the `notebox-web` (react/next) satellite renders the UI.
*(Origin: INTAKE §1, C1–C32, D4, D5.)*

### 1.2 Problem
Cross-functional teams (ops, support, product, engineering) keep structured knowledge and their task
lists scattered across ad-hoc docs, spreadsheets, and separate trackers — with no shared, typed structure
and no single bilingual home. Notebox gives a tenant one place to define **typed** note structures and
manage tasks with subtask-driven progress. *(Origin: decisão humana 2026-07-22, OQ-01.)*

### 1.3 Business goals
| # | Goal | Key indicator |
|---|------|---------------|
| G-01 | Let a tenant capture arbitrary structured knowledge as typed annotations | Activation: ≥1 annotation type created in week 1 |
| G-02 | Let a tenant manage tasks with subtask-driven progress | Activation: ≥1 task created in week 1 |
| G-03 | Operate bilingually with self-served translations | 100% of system message keys resolved in en + pt (NFR-02) |

### 1.4 Scope
**In scope (this API):**
- Annotation types, typed fields (7 field types), annotation records, and their listing/detail contracts.
- Groups and the navigation-tree data.
- Tasks, subtasks, derived status/dates, cards, rich-text details.
- i18n system-string catalog with a management (CRUD) contract.
- Multi-tenant identity, ownership, and authorization.
- Image storage as MySQL BLOBs with a binary retrieval endpoint.
- Redis caching (cache-aside) for hot read surfaces.

**Explicitly out of scope:**
- **UI rendering** — datagrids, popups, badges, sliders, the WYSIWYG editor widget, drag/drop, accessibility → owned by `notebox-web` (AD-06, D1).
- **Localization of user-created content** — type/field/group names and task text are stored as entered, not translated (D5).
- Object storage, external message brokers, real-time/push (AD-08).
- Payments, health, minors, or sector-regulated data (02-compliance "not applicable yet").

### 1.5 Personas
- **Tenant member** — a cross-functional team member (ops, support, product, engineering) who creates
  annotation types, fills annotations, and manages tasks in the shared workspace. *(decisão humana 2026-07-22, OQ-01)*
- **Tenant administrator** — manages the tenant/workspace and edits the translation catalog (C-03). *(decisão humana 2026-07-22, OQ-01)*

## 2. User Stories & Flows

### 2.1 User stories (by epic)
- **E1 — Annotation types**
  - US-1.1: As a member, I want to define an annotation type with an icon and typed fields, so that I can capture a category of notes consistently. *(C1, C2, C4)*
  - US-1.2: As a member, I want to mark fields "visible for viewing", so that listings show only the columns I care about. *(C5, D2)*
- **E2 — Annotations**
  - US-2.1: As a member, I want to create/edit/delete annotations of a type, so that I record real data. *(C13, C16, C17)*
  - US-2.2: As a member, I want a listing of a type's annotations exposing visible fields, plus a detail view exposing all fields, so that grid and detail have the data they need. *(C14, C18, BR-09)*
- **E3 — Groups & navigation**
  - US-3.1: As a member, I want to group annotations and tasks and get a navigation tree, so that I can organize my workspace. *(C28, C29, C30)*
- **E4 — Tasks & subtasks**
  - US-4.1: As a member, I want tasks with subtasks whose completion drives the task's progress, so that status reflects real work. *(C19, C22, C23, C24)*
  - US-4.2: As a member, I want task dates derived from subtasks and a card link and rich-text details, so that a task is self-contained. *(C21, C25, C26, C27)*
- **E5 — Internationalization**
  - US-5.1: As a user, I want all system text in my language (en/pt), so that the product is usable bilingually. *(C31, BR-08)*
  - US-5.2: As an administrator, I want to edit translations at runtime, so that wording changes need no deploy. *(C32, AD-05)*
- **E6 — Identity & tenancy**
  - US-6.1: As a tenant member, I want my data isolated from other tenants, so that my organization's notes stay private. *(D4, BR-01, BR-02)*

### 2.2 Main flows
1. **Define type → fill annotation → list/detail:** create type with fields → create annotation conforming to the type (BR-03) → list returns visible-field columns; detail returns all fields (BR-09).
2. **Task with subtasks:** create task → add subtasks → completing subtasks recomputes status % (BR-06); task dates track subtask span (BR-07).
3. **Translate:** administrator edits message catalog entries per locale (AD-05); readers get localized strings with no raw keys (BR-08).

## 3. Functional & Non-Functional Requirements

### 3.1 Functional (FR)
| ID | Description | Priority | Origin |
|----|-------------|----------|--------|
| FR-01 | Create, read, update, delete **annotation types** (name, icon image, ordered fields). | Must | INTAKE C1,C2,C4 + BR-03 |
| FR-02 | A type field declares: name, **field type ∈ {Text, List, Number, Free text, Single choice, Multiple choice, Image}**, optional icon, and a "visible for viewing" flag with per-type defaults. | Must | INTAKE C4,C6–C12,K3,D2 + BR-04 |
| FR-03 | For List/Single choice/Multiple choice fields, define selectable **options on the field definition** (design time) as an ordered list; each option carries a label and, for List badges, an optional background colour from the fixed palette {red, green, blue, black, gray, yellow}. Annotation values reference these predefined options. | Must | INTAKE C7,C10,C11,K4 + decisão humana 2026-07-22 (OQ-03) |
| FR-04 | Create, read, update, delete **annotation records** that conform to their type (values only for defined fields, each respecting the field's type). | Must | INTAKE C13,C16,C17 + BR-03 |
| FR-05 | **List annotations** of a type returning the **visible** fields (for grid) and a **detail** view returning **all** fields including non-visible ones. | Must | INTAKE C14,C18 + BR-09 |
| FR-06 | Delete of an annotation is an explicit operation (confirmation is a UI concern; the API performs the irreversible delete). | Must | INTAKE C17 + BR-05 |
| FR-07 | Store **images** (type icons, image-field values, rich-text embeds) as MySQL BLOBs with content-type/size metadata; retrieve binaries via a **dedicated binary endpoint**; support small/thumbnail rendering. | Must | INTAKE C3,C12,K5,D6 + AD-04 |
| FR-08 | Create and manage **groups**; assign an annotation or task to **at most one** group. Groups are **flat** (no nesting) and **separate per domain** (Annotation groups ≠ Task groups). | Must | INTAKE C29 + decisão humana 2026-07-22 (OQ-04) |
| FR-09 | Provide the **navigation-tree data**: annotations by type and tasks, organized by group. | Must | INTAKE C28,C30 |
| FR-10 | Create, read, update, delete **tasks** with: name, **priority ∈ {Low, Medium, High, Critical}**, optional card, optional rich-text details; **status is a derived completion %** and **dates are derived** from subtasks. | Must | INTAKE C19,D7 + BR-06,BR-07 |
| FR-11 | Manage **subtasks** (name, dates, optional card, done flag); completing/adding subtasks **recomputes the parent task's status %**. | Must | INTAKE C23,C24 + BR-06 |
| FR-12 | Derive a task's **start date = min(subtask start date)** and **end date = max(subtask end date)** — by date, not by position/insertion order. | Must | INTAKE C25 + BR-07 + decisão humana 2026-07-22 (OQ-05) |
| FR-13 | Attach a **card** as an **inline value object** (code id + optional URL) directly on a task or subtask; expose the id and its link. Not a shared entity. | Must | INTAKE C21 + decisão humana 2026-07-22 (OQ-06) |
| FR-14 | Store and return a task's **rich-text details** (WYSIWYG HTML with styles, colour, bold, underline, embedded images), **sanitized** on input/output. | Must | INTAKE C26,C27 + C-08 |
| FR-15 | Resolve and serve **localized system strings** for every user-facing message; never emit a raw key or blank. **Locale = user preference → `Accept-Language` → English**; **missing translation falls back to English**. | Must | INTAKE C31 + BR-08,AD-05 + decisão humana 2026-07-22 (OQ-07) |
| FR-16 | Provide a **translation-management** contract: CRUD message-catalog entries per locale, restricted to **tenant administrators**. **en and pt are fixed for v1** (no runtime locale creation). | Must | INTAKE C32 + AD-05,C-03 + decisão humana 2026-07-22 (OQ-07) |
| FR-17 | Enforce **multi-tenant identity and isolation**: authenticate the caller via a **stateless signed JWT** (no server session), resolve their tenant from the token, and authorize every access against it. | Must | INTAKE D4 + BR-01,BR-02 + decisão humana 2026-07-22 (OQ-08) |
| FR-18 | **Secret field values are encrypted at rest and revealed under audit.** The value of any field flagged "Secret" (Text/Free-text only — flag declared in FR-02) is stored **encrypted** (never cleartext) and returned in cleartext only to a caller holding the **elevated reveal role**, with **every reveal recorded in the audit trail**. Listings and ordinary reads return secret values **masked**; a dedicated reveal action returns the cleartext. Mechanism: **AES-256-GCM**, random per-value IV, single **application master key from the secret manager**, key-version tag for rotation (AD-14). | Must | decisão humana 2026-07-23 (OQ-15) + BR-10, AD-14, C-12 |

**Validation baseline** *(decisão humana 2026-07-22, OQ-09; feature specs may refine):* annotation type
name unique per tenant; type/field names and field type required; Number fields carry optional per-field
min/max bounds; image uploads limited to PNG/JPEG/GIF/WebP; task and annotation names required; rich-text
details optional. **Deleting an annotation type is rejected while it still owns annotation records**
(block, not cascade — the member deletes the records first) *(decisão humana 2026-07-24, OQ-14; refines
FR-01)*. **Editing a type's fields while it owns annotation records preserves field identity — and
therefore all existing values — for unchanged fields and for resends of an identical definition; removing
or retyping a field while records exist is rejected (block, localized error); adding a new field remains
allowed** *(decisão humana 2026-08-03, OQ-17; refines FR-01/FR-02 — full type-evolution/migration
semantics deferred to a dedicated future feature)*. **Changing a field's Secret flag — in either
direction — is rejected while the field still has values (block, localized error); the values must be
cleared explicitly first** *(decisão humana 2026-08-03, OQ-18; refines FR-02/FR-18 — guarantees no
cleartext-at-rest state can form, BR-10)*.

### 3.2 Non-Functional (NFR)
| ID | Category | Description | Target | Origin |
|----|----------|-------------|--------|--------|
| NFR-01 | Security / isolation | No cross-tenant read or write on any tenant-owned endpoint. | 0 leaks; automated cross-tenant test on every such endpoint passes in CI. | BR-01,BR-02 + C-01 |
| NFR-02 | Localization | Every user-facing system message key resolves in each supported locale. | 100% key coverage en + pt; catalog-coverage check in CI. | BR-08 + C-09 |
| NFR-03 | Performance | Cached hot reads (message catalog, type schemas) served from Redis cache-aside. | Cache-hit read **p95 < 50 ms**; measured by load test. *(OQ-10)* | AD-13 |
| NFR-04 | Integrity / safety | Image uploads are content-type validated and size-bounded before persistence. | **Max 5 MB**; oversize rejected; validation test. *(OQ-10)* | AD-04,AD-07 + C-07 |
| NFR-08 | Performance / contract | List endpoints (annotations, tasks) are paginated. | Default page size **50**, max **200**. *(OQ-10)* | decisão humana 2026-07-22 |
| NFR-05 | Resilience | A Redis outage or cache miss degrades to MySQL without error. | Fault-injection test: 0 errors on cache unavailability. | AD-13 |
| NFR-06 | Contract | Every HTTP endpoint is documented in OpenAPI, in sync with the code. | 100% endpoint coverage; contract lint in CI. | 03-standards (api_docs) |
| NFR-07 | Observability | Structured logging with a correlation id; no secrets/PII/cross-tenant data in logs. | Log review + test; correlation id present on every request. | 01-arch, 02-compliance |

## 4. Acceptance Criteria (Gherkin)
```gherkin
Feature: FR-04 Annotation conforms to its type
  Scenario: Reject a value for an undefined field
    Given an annotation type "RabbitMQ" with fields URL, Environment, Description
    When a client creates an annotation with a value for field "Region"
    Then the API rejects it with a validation error and a localized message

Feature: FR-05 Visible vs detail fields
  Scenario: Listing shows only visible fields
    Given type "RabbitMQ" where Description is not "visible for viewing"
    When a client lists annotations of type "RabbitMQ"
    Then each row exposes URL and Environment but not Description
    And the detail view of a row exposes URL, Environment and Description

Feature: FR-11 Derived task status
  Scenario: Completing a subtask updates progress
    Given a task with 4 subtasks, 1 completed
    When a client marks a second subtask done
    Then the task status is 50%

Feature: NFR-01 Tenant isolation
  Scenario: Foreign tenant cannot read another tenant's annotation
    Given tenant A owns annotation X
    When a member of tenant B requests annotation X
    Then the API responds 404/403 and no data of X is returned

Feature: FR-18 Secret values encrypted at rest and revealed under audit
  Scenario: A secret value is never stored in cleartext
    Given a type with a Text field "API key" flagged Secret
    When a member creates an annotation with "API key" = "s3cr3t-token"
    Then the value persisted in the database is ciphertext, not "s3cr3t-token"

  Scenario: Ordinary read masks the secret value
    Given an annotation whose Secret field "API key" holds a value
    When a member reads or lists that annotation without requesting a reveal
    Then the "API key" value is returned masked, never in cleartext

  Scenario: Reveal requires the elevated role and is audited
    Given an annotation whose Secret field "API key" holds a value
    When a caller holding the reveal role requests the cleartext of "API key"
    Then the cleartext is returned
    And an audit entry records who revealed which value and when

  Scenario: Reveal denied without the elevated role
    Given an annotation whose Secret field "API key" holds a value
    When a member without the reveal role requests the cleartext of "API key"
    Then the request is rejected as unauthorized
    And no cleartext is returned
```

## 5. Architecture & Stack
Governed by the constitution (`constitution/01-architecture.md`): layered architecture (AD-01),
repository-only persistence (AD-02), tenant-scoped repository choke point (AD-03), BLOB images via a
binary endpoint (AD-04), i18n message catalog (AD-05), stateless REST/JSON (AD-06), Bean Validation at
the edge (AD-07), declarative `@Transactional` (AD-09), CDI events for change notification (AD-10),
JAX-RS response/exception providers (AD-11), CDI DI/IoC with explicit bean scopes (AD-12), and Redis
cache-aside (AD-13). **Authentication: stateless signed JWT** (decisão humana 2026-07-22, OQ-08) —
`@SessionScoped`/`@Stateful` beans are not used; this confirms AD-06.

## 6. Success Metrics / KPIs
*(Origin: decisão humana 2026-07-22, OQ-02. Target numbers are proposed defaults — confirm during delivery.)*
| Metric | How measured | Target (proposed) | Cadence |
|--------|--------------|-------------------|---------|
| Tenant activation | % of new tenants creating ≥1 annotation type AND ≥1 task within week 1 | ≥ 60% | Weekly |
| Weekly active members | Distinct members performing a create/edit action per tenant per week | Trending up | Weekly |

## 7. Roadmap & Phases (proposed — confirm at delivery)
1. **Foundation** — tenancy + identity (FR-17, OQ-08), i18n catalog (FR-15/16), image storage (FR-07).
2. **Annotations** — types & fields (FR-01/02/03), records & listing (FR-04/05/06), secret-value encryption & reveal (FR-18).
3. **Organization** — groups & navigation (FR-08/09).
4. **Tasks** — tasks/subtasks/derived metrics (FR-10/11/12), cards & rich-text (FR-13/14).

## 8. Assumptions & Open Questions
See `catalogs/open-questions.md`. **No open questions remain (0 pending).**

**Audit-phase set — opened by the feat-005 audit, both resolved (2026-08-03):**
- **OQ-17** → type field edits vs existing records: **preserve field identity for unchanged fields /
  identical resends; block field removal or retype while records exist (localized 409); field additions
  allowed**. Interim guard ships in feat-005 (symmetric to OQ-14); full type-evolution/migration deferred
  to a dedicated future feature. *(Refines §3.1 validation baseline.)*
- **OQ-18** → Secret-flag flip (audit F12): **blocked — in either direction — while the field still has
  values (localized 409)**; flipping requires the field's values to be cleared explicitly first, so no
  cleartext-at-rest state can form (BR-10 holds). Migration-on-flip deferred as a possible future feature.
  *(Refines §3.1 validation baseline.)*

**v1 set — all 10 resolved (2026-07-22):** OQ-01 personas (cross-functional teams); OQ-02 activation/usage
KPIs; OQ-03 options on field definition; OQ-04 single-membership flat groups; OQ-05 dates by min/max;
OQ-06 inline Card; OQ-07 i18n (preference→header→English, admin-edited, en/pt fixed); OQ-08 stateless JWT;
OQ-09 validation baseline; OQ-10 NFR targets.

**Delivery-phase set — OQ-11…OQ-16, all resolved (2026-07-24):**
- **OQ-15** → drives **FR-18** / BR-10 / AD-14 / C-12 (secret-value encryption + audited reveal). *Formalized in this v2.*
- **OQ-14** → refines type deletion: **block while records exist** (validation baseline, §3.1).
- **OQ-11** *(deferred → future feature)* — provisioning is **admin-managed with invite**; no public self-service signup.
- **OQ-12** *(deferred → future feature)* — password reset is **admin-managed** (temporary credential); no email infra.
- **OQ-13** *(deferred → small web feature)* — "Remember me" = **localStorage token toggle**, staying stateless (survives tab-close within token lifetime, not indefinite).
- **OQ-16** *(deferred → refactor feature)* — realign feat-001 error-handling/i18n to `03-code-standards` (specific domain exceptions + dot-namespaced keys).

The four deferred items (OQ-11/12/13/16) define **future features**, not new requirements of this version, and are not `[TBD]` blockers on any current FR.

## 9. Traceability
### 9.1 BR → FR map
| BR | Theme | FRs |
|----|-------|-----|
| BR-01/02 | Tenant isolation & ownership | FR-17 (+ all, enforced) |
| BR-03 | Annotation conforms to type | FR-01, FR-04, FR-05 |
| BR-04 | Closed field-type set | FR-02, FR-03 |
| BR-05 | Explicit, irreversible delete | FR-06 |
| BR-06 | Derived task status | FR-10, FR-11 |
| BR-07 | Derived task dates | FR-12 |
| BR-08 | No untranslated text | FR-15, FR-16 |
| BR-09 | Visible-for-viewing is display-only | FR-05 |
| BR-10 | Secret values confidential at rest | FR-18 |

### 9.2 FR → NFR / compliance
| Area | FRs | NFR / C |
|------|-----|---------|
| Isolation | FR-17 + all | NFR-01, C-01/02/03 |
| i18n | FR-15, FR-16 | NFR-02, C-09 |
| Images | FR-07 | NFR-04, C-07 |
| Rich text | FR-14 | C-08 |
| Secret values at rest | FR-18 | AD-14, C-12 (+ C-05 key handling, C-10 reveal audit) |
| Performance | (reads) | NFR-03, NFR-05 |
| Contract/observability | all endpoints | NFR-06, NFR-07 |
