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
**Status:** ✅ resolved (2026-07-24).
**Decision:** Admin-managed provisioning with invite: tenants are provisioned by a system operator (DB seed/migration or an internal ops endpoint); within a tenant, the Tenant administrator invites/creates member users. No public self-service signup. Matches the existing Tenant administrator role (OQ-01) and feat-001's assumption that users pre-exist. A dedicated provisioning/invite feature captures this later; not a blocker for the annotation/task core. — decided by rafaelsantos, 2026-07-24.

### OQ-12 — Password-reset flow
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** The login screen has a 'Forgot password?' placeholder link (feat-002) with no backing flow. Needs its own feature: request reset, deliver token, set new password. Deferred by feat-002 spec.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-07-24).
**Decision:** Admin-managed reset: a Tenant administrator resets a member's password or issues a one-time temporary credential that the member must change on next login. No email/SMTP infra required; consistent with OQ-11's admin-managed provisioning. The 'Forgot password?' link (feat-002) directs the user to contact their admin until/unless self-service is productized. Captured by the future provisioning/identity-admin feature alongside invites (OQ-11). — decided by rafaelsantos, 2026-07-24.

### OQ-13 — Remember-me persistence
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** The login screen has a 'Remember me' checkbox (feat-002) that is visual-only. Real behaviour = persist the session beyond token expiry / across tab-close (e.g. localStorage or refresh token), which the feat-002 plan deliberately avoided (chose sessionStorage). Needs its own decision + feature.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-07-24).
**Decision:** localStorage toggle (stays stateless): 'Remember me' checked stores the JWT in localStorage (persists across tab close until token expiry); unchecked keeps feat-002's sessionStorage (cleared on tab close). No refresh tokens, no server session — consistent with the stateless JWT AD. Scope is explicit: 'remember' means survive tab-close within the token's lifetime, not indefinite re-authentication. Same web-storage XSS threat model feat-002 already accepts; note it in the web feature. Durable multi-session persistence (refresh tokens) remains a separate future identity feature if ever needed. Web-only change. — decided by rafaelsantos, 2026-07-24.

### OQ-14 — Type deletion when annotation records exist (block vs cascade)
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** When a member deletes an annotation type that owns records, is the delete blocked while records exist or does it cascade? No records exist until US-2.1/FR-04; feat-003 deletes only empty types. Must be settled before US-2.1 implements record deletion.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-07-24).
**Decision:** Block (RESTRICT): deleting an annotation type is rejected with a specific error (annotation.type.has_records, 409) while any annotation record of that type exists; the member must delete the records first. Preserves BR-05's explicit/irreversible framing and prevents one call from wiping data. feat-003's empty-only delete already conforms; US-2.1 enforces the guard when record deletion lands. — decided by rafaelsantos, 2026-07-24.

### OQ-15 — Secret text fields — encryption-at-rest mechanism, key management & PRD/constitution formalization
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** New requirement (human decision 2026-07-23): Text/Free text fields can carry a 'Secret' flag; flagged VALUES are stored encrypted at rest and revealed in cleartext only to an elevated role, with each reveal audited (C-10). DECIDED: applies to Text+Free text; reveal=elevated role+audit. UNDECIDED (blocks US-2.1 value encryption + reveal endpoint): crypto algorithm (e.g. AES-256-GCM), key scope (single app key vs per-tenant), key storage (secret manager/KMS), rotation. Also REQUIRES a PRD v2 (new FR) + constitution update (new AD 'encryption at rest' + compliance C-item + BR) before US-2.1 implements it. feat-003 only adds the type-definition FLAG; encryption/decrypt/reveal + UI masking (field + datatable) are deferred to US-2.1 and the web features.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-07-24).
**Decision:** Secret VALUES (Text/Free text) encrypted at rest with AES-256-GCM: random 96-bit IV per value; ciphertext+IV+auth-tag persisted in MySQL; single application master key sourced from the secret manager (C-05 pattern, as for JWT keys); each ciphertext tagged with a key-version id to allow key rotation without rewriting old rows. Reveal in cleartext only to an elevated role, each reveal audited (C-10). Formalization pending: PRD v2 (new FR) + constitution (new AD 'encryption at rest' + a compliance C-item + a BR). — decided by rafaelsantos, 2026-07-24.

### OQ-16 — feat-001 error-handling/i18n realignment to constitution
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** feat-001 (identity, merged on develop) drifted from constitution 03-code-standards: it uses a single generic ApiException(code,status) with static factories and SCREAMING_SNAKE catalog keys (AUTH_INVALID_CREDENTIALS), instead of the mandated specific domain exceptions (line 84) and dot-namespaced i18n keys (line 25, e.g. annotation.type.name.required). feat-003+ follow the constitution; feat-001 should be realigned (specific exceptions extending a domain base + dot-namespaced keys + unify the ExceptionMapper) as a follow-up refactor. Does not block feat-003.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-07-24).
**Decision:** Realign via a dedicated follow-up refactor feature: convert feat-001 (identity) to specific domain exceptions extending a domain base class, dot-namespaced i18n keys (e.g. auth.credentials.invalid) with en+pt entries, and a unified ExceptionMapper matching feat-003+. No functional change; testable in isolation. Scheduled in the delivery backlog; does not block current features. Restores constitution 03-code-standards consistency (no permanent exception granted). — decided by rafaelsantos, 2026-07-24.

### OQ-17 — Type field mutation vs existing records — orphaned annotation values
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** feat-003 type PUT replaces TypeFields with orphanRemoval; editing/removing a field on a type that already owns annotation records (US-2.1) can orphan AnnotationValue.typeFieldId references or delete the field a value points to. Symmetric to OQ-14 (type-delete block). Options: block field removal/retype while values exist, or migrate/null affected values. Out of scope for feat-005; needs its own decision before type editing is exercised against populated types.
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
| 2026-07-23 | OQ-12 opened: Password-reset flow |
| 2026-07-23 | OQ-13 opened: Remember-me persistence |
| 2026-07-23 | OQ-14 opened: Type deletion when annotation records exist (block vs cascade) |
| 2026-07-23 | OQ-15 opened: Secret text fields — encryption-at-rest mechanism, key management & PR |
| 2026-07-23 | OQ-16 opened: feat-001 error-handling/i18n realignment to constitution |
| 2026-07-24 | OQ-15 resolved: Secret VALUES (Text/Free text) encrypted at rest with AES-256-GCM: ran… |
| 2026-07-24 | OQ-11 resolved: Admin-managed provisioning with invite: tenants are provisioned by a s… |
| 2026-07-24 | OQ-14 resolved: Block (RESTRICT): deleting an annotation type is rejected with a speci… |
| 2026-07-24 | OQ-12 resolved: Admin-managed reset: a Tenant administrator resets a member's password… |
| 2026-07-24 | OQ-13 resolved: localStorage toggle (stays stateless): 'Remember me' checked stores th… |
| 2026-07-24 | OQ-16 resolved: Realign via a dedicated follow-up refactor feature: convert feat-001 (… |
| 2026-07-25 | OQ-17 opened: Type field mutation vs existing records — orphaned annotation values |

## Rules
- IDs immutable. Resolved → mark ✅ with a reference. New → next sequential ID.
- Every feature spec lists the OQs that affect it.
