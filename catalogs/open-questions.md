# Catalog — Open Questions (OQ)

> Living list of blockers, ambiguities, and pending decisions. Immutable IDs. Resolution comes
> from the PRD process or a written human decision. Language: English.

**Last sync with PRD:** v1 (2026-07-22)

## Overview by severity

| Severity | Criterion | IDs |
|----------|-----------|-----|
| 🔴 Blocker | blocks planning an entire feature | — |
| 🟡 Important | blocks details, not the feature | — |
| 🟢 Tactical | can wait | — |
| ✅ Resolved | — | OQ-01 … OQ-10 (all, 2026-07-22) |

## List

### OQ-01 — Target users & personas
**Severity:** 🟡 Important
**Description:** The brief describes *what* Notebox does but never names *who* uses it or the pain observed. Personas (individual knowledge-keeper? engineering team? ops?) are unstated.
**Impact:** Problem statement, Outcome, and KPI framing (PRD §1, §6) cannot be grounded.
**Suggested path:** Human names the primary and secondary personas and the pain each feels today.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Primary persona: cross-functional teams (ops, support, product, engineering) using Notebox as a shared structured knowledge base plus task tracker, within a tenant/workspace. Secondary: tenant administrator managing the workspace and translations. — decided by rafaelsantos, 2026-07-22.

### OQ-02 — Measurable success outcome
**Severity:** 🟡 Important
**Description:** No measurable definition of success (adoption, notes/tasks created, retention, time saved) is stated.
**Impact:** Outcome and Success Metrics (PRD §6) are `[TBD]`.
**Suggested path:** Human defines 1–3 target metrics with numbers.
**Depends on:** Human decision; may follow OQ-01.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Success anchored on activation & active usage. Metrics: (1) Activation — % of tenants that create >=1 annotation type AND >=1 task within week 1; (2) Weekly active members per tenant. Exact targets proposed as defaults (activation >=60%; WAU/tenant trending up) to confirm during delivery. — decided by rafaelsantos, 2026-07-22.

### OQ-03 — List/choice options & badge colour storage
**Severity:** 🟡 Important
**Description:** For List / Single choice / Multiple choice fields, where are the allowed options defined — on the type-field definition (design time) or free at data entry? Badge background colour is chosen per list item from the fixed palette (red, green, blue, black, gray, yellow) — stored on the option definition? (INTAKE G4, C7, C10, C11, K4)
**Impact:** Data model of `TypeField` / option entity; validation of annotation values (FR-02, FR-03).
**Suggested path:** Confirm options live on the field definition with an ordered option list carrying label + optional badge colour.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Options for List/Single choice/Multiple choice fields are defined on the field definition (design time) as an ordered option list. Each option carries a label and, for List badge rendering, an optional background colour from the fixed palette (red, green, blue, black, gray, yellow). Annotation values reference these predefined options. — decided by rafaelsantos, 2026-07-22.

### OQ-04 — Group semantics
**Severity:** 🟡 Important
**Description:** Can an item belong to multiple groups? Are groups nested? Are Annotation groups and Task groups the same entity or separate? (INTAKE G5, C29)
**Impact:** `Group` entity model and the navigation-tree contract (FR-09, FR-10).
**Suggested path:** Confirm single-membership, flat (non-nested), separate group namespaces per domain — or state otherwise.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Groups are single-membership (an item belongs to at most one group), flat (no nesting), and separate per domain (Annotation groups and Task groups are distinct namespaces). — decided by rafaelsantos, 2026-07-22.

### OQ-05 — Subtask ordering for task-date derivation
**Severity:** 🟡 Important
**Description:** Task start = first subtask's start, end = last subtask's end (BR-07). "First/last" implies an ordering — by start date, by end date, or by insertion order? (INTAKE G6, C25)
**Impact:** The derived-date rule (FR-13, BR-07) is ambiguous without it.
**Suggested path:** Confirm min(subtask.startDate) and max(subtask.endDate) rather than positional ordering.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Task dates derive by date, not position: task.startDate = min(subtask.startDate), task.endDate = max(subtask.endDate). Robust to insertion/display order. — decided by rafaelsantos, 2026-07-22.

### OQ-06 — Card: shared entity or inline value object
**Severity:** 🟡 Important
**Description:** Is Card (code id + optional URL) a shared entity referenced by many tasks/subtasks, or an inline value object owned by one task/subtask? (INTAKE G7, C21)
**Impact:** `Card` modelling and referential integrity (FR-14).
**Suggested path:** Confirm inline value object per task/subtask unless cross-task reuse is required.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Card is an inline value object (code id + optional URL) stored directly on its task or subtask; not a shared entity. No cross-task referencing. — decided by rafaelsantos, 2026-07-22.

### OQ-07 — i18n mechanics
**Severity:** 🟡 Important
**Description:** (a) How is the request locale chosen — `Accept-Language`, user preference, query param? (b) Missing-translation fallback behaviour (fall back to which locale)? (c) Who may edit the message catalog (admin role)? (d) Can new locales beyond en/pt be added at runtime? (INTAKE G10, C31, C32, AD-05, BR-08)
**Impact:** i18n endpoint contract and admin authorization (FR-16, FR-17, C-03, C-09).
**Suggested path:** Propose: locale from user preference then `Accept-Language`; fallback to English; tenant-admin role edits; en/pt fixed for v1.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** i18n mechanics: (a) request locale = user's saved preference, else Accept-Language header, else English default; (b) missing translation falls back to English; (c) only tenant-administrators may edit the message catalog; (d) en and pt are fixed for v1 (no runtime locale creation). — decided by rafaelsantos, 2026-07-22.

### OQ-08 — Authentication model: stateless token vs server-side session
**Severity:** 🔴 Blocker
**Description:** The API is described as stateless REST (AD-06), but the human requested well-used `@SessionScoped`/`@Stateful` beans (AD-12), which imply server-side session state. These are mutually exclusive defaults. Also unspecified: identity provider, login mechanism, tenant-membership resolution.
**Impact:** Blocks planning the identity/authentication feature (FR-18) and determines whether session/stateful bean scopes are used at all.
**Suggested path:** Decide token-based stateless (JWT/opaque token, request/application-scoped beans) vs server-side session (`@SessionScoped`). Recommendation: token-based stateless for a REST API consumed by `notebox-web`.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Stateless JWT token authentication. Client sends a signed JWT per request; the server keeps no session. Beans remain @RequestScoped/@ApplicationScoped (AD-12); @SessionScoped/@Stateful are NOT used. Confirms AD-06 stateless REST. — decided by rafaelsantos, 2026-07-22.

### OQ-09 — Validation rules & invariants
**Severity:** 🟡 Important
**Description:** Which fields are mandatory; uniqueness constraints (e.g. annotation type name per tenant); numeric (Number field) slider min/max bounds; allowed image content types. (INTAKE G8)
**Impact:** Bean Validation contracts at the API edge (AD-07, C-07) and FR acceptance criteria.
**Suggested path:** Gather per-entity validation rules during feature specs; set sensible defaults confirmed by the human.
**Depends on:** Human decision, per feature.
**Status:** ✅ resolved (2026-07-22).
**Decision:** Baseline validation defaults (feature specs may refine): annotation type name unique per tenant; type name, field name and fieldType required; Number fields carry optional min/max bounds defined per field; image uploads limited to PNG/JPEG/GIF/WebP; task and annotation names required; rich-text details optional. — decided by rafaelsantos, 2026-07-22.

### OQ-10 — Non-functional numeric targets
**Severity:** 🟡 Important
**Description:** No numbers given for: cache-hit read latency target (NFR-03), maximum image upload size (NFR-04), pagination/list size limits for annotation and task listings.
**Impact:** NFR targets (PRD §3.2) are `[TBD]` and cannot be verified.
**Suggested path:** Human sets targets, or accept proposed defaults (e.g. cached read p95 < 50 ms; image ≤ 5 MB; page size 50).
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-07-22).
**Decision:** NFR numeric targets: cached read p95 < 50 ms; max image upload 5 MB; default list page size 50 (max 200). — decided by rafaelsantos, 2026-07-22.

### OQ-11 — Tenant & user provisioning mechanism
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** How tenants and users are created (self-service signup vs admin/seeded provisioning) is unspecified; this feature's auth assumes users already exist. Blocks a future provisioning feature and the end-to-end login flow.
**Suggested path:** <how to resolve>
**Status:** open.

## History

| Date | Change |
|------|--------|
| 2026-07-19 | Initial creation. |
| 2026-07-22 | Opened OQ-01…OQ-10 from INTAKE.md §6 gaps and PRD-authoring (personas, outcome, auth model). |
| 2026-07-22 | OQ-08 resolved: Stateless JWT token authentication. Client sends a signed JWT per requ… |
| 2026-07-22 | OQ-01 resolved: Primary persona: cross-functional teams (ops, support, product, engine… |
| 2026-07-22 | OQ-02 resolved: Success anchored on activation & active usage. Metrics: (1) Activation… |
| 2026-07-22 | OQ-03 resolved: Options for List/Single choice/Multiple choice fields are defined on t… |
| 2026-07-22 | OQ-04 resolved: Groups are single-membership (an item belongs to at most one group), f… |
| 2026-07-22 | OQ-05 resolved: Task dates derive by date, not position: task.startDate = min(subtask.… |
| 2026-07-22 | OQ-06 resolved: Card is an inline value object (code id + optional URL) stored directl… |
| 2026-07-22 | OQ-07 resolved: i18n mechanics: (a) request locale = user's saved preference, else Acc… |
| 2026-07-22 | OQ-09 resolved: Baseline validation defaults (feature specs may refine): annotation ty… |
| 2026-07-22 | OQ-10 resolved: NFR numeric targets: cached read p95 < 50 ms; max image upload 5 MB; d… |
| 2026-07-22 | OQ-11 opened: Tenant & user provisioning mechanism |

## Rules
- IDs immutable. Resolved → mark ✅ with a reference. New → next sequential ID.
- Every feature spec lists the OQs that affect it.
