# Catalog — Open Questions (OQ)

> Living list of blockers, ambiguities, and pending decisions. Immutable IDs. Resolution comes
> from the PRD process or a written human decision. Language: English.

**Last sync with PRD:** v1 (2026-07-22)

## Overview by severity

| Severity | Criterion | IDs |
|----------|-----------|-----|
| 🔴 Blocker | blocks planning an entire feature | — |
| 🟡 Important | blocks details, not the feature | — |
| 🟢 Tactical | can wait | OQ-26 |
| ✅ Resolved | — | OQ-01 … OQ-25 (latest: OQ-23/OQ-24/OQ-25, 2026-08-22) |

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
**Severity:** 🔴 Blocker *(re-rated from 🟢 Tactical per audit F5, 2026-08-03)*
**Description:** What must happen to a record's values when the owning type's field set is edited via PUT while records exist? Audit F5 proved the current behavior is destructive on the most innocent input: feat-003's replace generates new TypeField UUIDs on **every** PUT, so even resending an *identical* definition orphans every AnnotationValue of every record of that type (GET returns `values: []`) and flips the type-delete guard to a permanent `409 has_records`. Unknown: whether field edits must be blocked while records exist (symmetric to OQ-14), whether values must be preserved by matching unchanged fields, or whether destruction is acceptable and merely audited.
**Impact:** feat-003 type PUT replaces TypeFields with orphanRemoval; editing/removing a field on a type that already owns annotation records (US-2.1) can orphan AnnotationValue.typeFieldId references or delete the field a value points to. Symmetric to OQ-14 (type-delete block). Options: block field removal/retype while values exist, or migrate/null affected values. Blocks feat-005's audit (finding F5): the destruction breaks feat-005's own delivered reads, so a decision — including whether feat-005 ships an interim guard — is required before the re-audit.
**Suggested path:** Human decision, symmetric to OQ-14. Interim (feat-005 scope): block field-set changes on a populated type with a localized 409, or preserve field identity when the definition is unchanged. Long-term: a dedicated type-evolution feature (migrate/null affected values).
**Status:** ✅ resolved (2026-08-03).
**Decision:** Guard + preserve unchanged (interim guard ships in feat-005, symmetric to OQ-14): the type PUT matches incoming fields to existing ones and preserves field identity — and therefore all existing values — for unchanged fields and for resends of an identical definition; removing or retyping a field while records exist is rejected with a localized 409; adding a new field remains allowed. Full type-evolution/migration semantics deferred to a dedicated future feature. — decided by rafaelsantos, 2026-08-03.

### OQ-18 — Secret flag flip on a field with existing values — cleartext/ciphertext mismatch
**Severity:** 🟡 Important
**Description:** What are the semantics of toggling a field's Secret flag (via the type PUT) when values already exist for that field? Encryption happens at write time, so a flip changes how *future* writes are stored but not how *existing* rows were stored — and today masking keys off the field's flag while reveal keys off the value's stored form, so the two ends of the API disagree after any flip (audit F12).
**Impact:** Audit F12: reveal keys off ciphertext presence (value.isSecret) while masking keys off the field's Secret flag. Flipping a TEXT field to Secret leaves existing cleartext in text_value while reads mask it and reveal returns 400 — the value is unreadable through any endpoint yet its cleartext stays at rest, a BR-10 hole. Un-flipping leaves ciphertext invisible on read yet still revealable. Needs decided semantics: block the flip while values exist, migrate values on flip (encrypt/decrypt), or align reveal with the field flag and accept the legacy state. Blocks feat-005 spec closure (audit verdict).
**Suggested path:** Human decision. Options: (a) block the flip while values exist for the field (guard family of OQ-14/OQ-17); (b) migrate on flip — encrypt existing cleartext when Secret turns on, decrypt (ADMIN-gated, audited) when it turns off; (c) key reveal off the field flag only and accept legacy cleartext at rest — conflicts with BR-10.
**Status:** ✅ resolved (2026-08-03).
**Decision:** Block the flip while values exist (guard family of OQ-14/OQ-17): the type PUT rejects a Secret-flag change — in either direction — on a field that still has values, with a localized 409; flipping requires the field's values to be cleared explicitly first. No cleartext-at-rest state can form (BR-10 holds); migration-on-flip deferred as a possible future feature. — decided by rafaelsantos, 2026-08-03.

### OQ-19 — Free text record values: plain text or rich text?
**Severity:** 🟡 Important
**Description:** The satellite's design handoff (screen 13, "Annotation create/edit") specifies a **rich editor** for Free text **values** — bold/italic/underline/strike, colour, lists, quote, link, image, inline code and a syntax-highlighted code block with language select. The PRD instead binds rich text (**FR-14**) to **Task details** in US-4.2, and feat-004 deliberately treated "Free text" as a plain field declaration, not an editor. Nothing states which one governs an annotation record's Free text value.
**Impact:** Material to feat-006 (web half of US-2.1) and to feat-005's storage posture. A rich editor makes stored values carry **markup**, which (a) flips **C-08 rich-text sanitization** from *not applicable* to *applies* for this feature, (b) raises where sanitization happens — feat-005 stores and returns the Free text value verbatim today and specifies no sanitization, so a decision for rich text implies a change on the API side too, and (c) materially changes feat-006's size and its plan. feat-006's spec v1 **assumes plain multi-line text** and is written on that assumption; a rich-text answer requires a spec v2.
**Suggested path:** Human decision. Options: (a) **plain text now** — Free text values stay plain, the rich editor stays with FR-14/US-4.2 where the PRD put it, and the design's screen 13 is amended; (b) **rich text now** — accept the design, open the sanitization question against C-08 and revisit feat-005's storage/return contract; (c) **plain now, rich later** — ship plain in feat-006 and schedule a dedicated rich-value feature carrying its own sanitization design.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-11).
**Decision:** Rich text now, in feat-006: annotation Free-text VALUES are rich text (WYSIWYG HTML — styles, colour, bold, underline, lists, quote, link, inline code, syntax-highlighted code block, embedded images), matching design handoff screen 13. C-08 rich-text sanitization therefore applies to annotation values as it does to task details (FR-14): sanitized on input/output to an allow-list. Since C-08 binds the feature that STORES OR RETURNS rich text, the API side (feat-005, already merged) now carries a sanitization obligation it does not implement — surfaced separately for a scoping decision. feat-006 spec goes to v2. — decided by rafaelsantos, 2026-08-11.

### OQ-20 — Record listing sort order
**Severity:** 🟡 Important
**Description:** FR-05/NFR-08 define the paginated listing of a type's records (feat-008) but no source names its sort order. Pagination makes the order contractual: an undefined order breaks page stability and makes the grid's pager (design screen 11) non-deterministic.
**Impact:** Blocks feat-008's plan/implement (tests need a deterministic order; the wire contract must document it). The web grid (feat-009) inherits whatever is decided.
**Suggested path:** Human decision. Options: (a) **newest first** (`createdAt` desc — recommended: a record just created appears on page 1, matching the create→grid flow of PRD §84); (b) name asc (alphabetical browsing); (c) updated-at desc (recently-touched first, but rows jump pages on edit).
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-12).
**Decision:** Newest first: the listing is ordered by createdAt descending (ties broken by id for total stability). A just-created record appears at the top of page 1, matching the PRD §84 create-to-grid happy path; rows never move on edit. Applies to feat-008's wire contract and inherited by the feat-009 grid. — decided by rafaelsantos, 2026-08-12.

### OQ-21 — Task listing sort order
**Severity:** 🟡 Important
**Description:** NFR-08 paginates the task listing (feat-010) but its ordering is unfixed: the OQ-20 decision ("newest first, `createdAt` desc, id tiebreak") was scoped to **record** listings. Pagination makes order contractual — an undefined order breaks page stability and test determinism.
**Impact:** Blocks the listing scenario of feat-010's spec and the wire contract its plan must document; feat-011's task list UI inherits the decision.
**Suggested path:** Human decision. Options: (a) **newest first** (`createdAt` desc, id tiebreak — recommended: mirrors OQ-20, one uniform listing contract across the product); (b) priority desc then recency (urgent work first, but rows jump when priority is edited); (c) name asc.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-13).
**Decision:** Newest first, mirroring OQ-20: the task listing is ordered by `createdAt` descending, ties broken by id — one uniform listing contract across the product (records and tasks alike). Applies to feat-010's wire contract; feat-011's task list inherits it. — decided by rafaelsantos, 2026-08-13.

### OQ-22 — Task status representation for non-integer proportions
**Severity:** 🟡 Important
**Description:** BR-06 fixes the status *rule* — the proportion of subtasks marked done, 0% with none — but not its wire *representation* when the division is not exact: 1 of 3 done → 33? 33.33? 34? The PRD's only example (2 of 4 → 50%, §4) never exercises a remainder.
**Impact:** Blocks the derived-status contract detail in feat-010 (tests need an exact expected value; feat-011's progress bar renders whatever the API returns).
**Suggested path:** Human decision. Options: (a) **integer percent, round half up** (1/3 → 33, 2/3 → 67 — recommended: matches a progress-bar consumer, keeps the contract integer-typed); (b) two-decimal percent (33.33); (c) raw fraction (done/total) letting clients format.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-13).
**Decision:** Integer percent, rounded half up: status = round(done ÷ total × 100) with .5 rounding up (1/3 → 33, 2/3 → 67, 1/8 → 13); 0% with no subtasks per BR-06. The wire contract stays integer-typed, matching the progress-bar consumer. — decided by rafaelsantos, 2026-08-13.

### OQ-23 — Navigation-tree composition: how group nodes and type nodes compose
**Severity:** 🟡 Important
**Description:** FR-08 assigns "an annotation or task" to a group — in PRD vocabulary an *annotation* is a record (FR-04/FR-05), so groups hold records. But C28 says that under Annotations "each annotation **type** is a node". FR-09's "annotations by type and tasks, organized by group" does not say which axis nests inside the other, and the two readings imply different entity models (the group reference sits on the record vs. on the type). OQ-04 fixed group *semantics* (single-membership, flat, per-domain) but explicitly left the navigation-tree contract open in its own Impact line.
**Impact:** Blocks feat-014's spec — the tree contract and which entity carries the group reference. feat-015's Navigator sidebar inherits the shape.
**Suggested path:** Human decision. Options: (a) **type → group** (recommended: matches FR-08's literal "an annotation", honours C28's type nodes, keeps the tree bounded); (b) group → type (would require amending FR-08); (c) type → group → individual record leaves (unbounded payload, collides with NFR-08).
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-22).
**Clarified 2026-08-24 (feat-015 spec):** this decision governs the **payload**, not the UI. The design reference (`notebox-web/design/handoff`, screen 05) nests the Navigator **group → type**, and that stands: `/navigation` returns a bipartite type↔group relation and the web inverts it for display. No API change — nesting is presentation, which AD-06 assigns to the satellite. Decided by rafaelsantos.
**Decision:** Type → group. Groups hold annotation **records** and **tasks**, as FR-08 and OQ-04 word it. Under the Annotations root the primary axis is the annotation **type** (C28); each type node then carries the groups that its own records occupy, plus an `Ungrouped` node. Under the Tasks root, group nodes sit directly beneath. The tree **stops at group nodes** — individual records and tasks are never enumerated as leaves, so the payload stays bounded and clicking a group node opens the existing US-2.2 / feat-010 paginated listing filtered by group rather than a new contract. — decided by rafaelsantos, 2026-08-22.

### OQ-24 — Deleting a group that still holds annotations or tasks
**Severity:** 🟡 Important
**Description:** FR-08 grants group CRUD but does not say what deleting a non-empty group does. OQ-14 set a *block* rule for deleting an annotation **type** that still owns records, but that rule was justified by data loss — a record without its type is meaningless. A group is a label, not a schema, so the precedent does not transfer automatically.
**Impact:** Blocks feat-014's group-delete scenario and the BR-05 reading for this surface.
**Suggested path:** Human decision. Options: (a) **un-group the members** (recommended: the group is deleted, its items survive as ungrouped — no annotation or task data is destroyed); (b) block while non-empty, mirroring OQ-14.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-22).
**Decision:** Un-group the members. Deleting a group removes the group only; every annotation record and task that referenced it survives and becomes ungrouped, surfacing under the `Ungrouped` node. No annotation or task is deleted, so BR-05's irreversible-delete concern is met by the group's own removal, and reorganizing stays cheap. Deliberately **diverges from OQ-14**, whose block rule exists to prevent orphaned records. — decided by rafaelsantos, 2026-08-22.

### OQ-25 — Navigation-tree node ordering
**Severity:** 🟡 Important
**Description:** FR-09 fixes no ordering for sibling nodes (types under Annotations; groups under a type or under Tasks). OQ-20 and OQ-21 fixed `createdAt` desc for the paginated **record** and **task** listings, but their rationale was page stability — a concern an unpaginated tree does not have. Undefined order makes the Navigator non-deterministic and its tests unwritable.
**Impact:** Blocks feat-014's tree scenarios and the wire contract; feat-015's sidebar inherits the decision.
**Suggested path:** Human decision. Options: (a) **name ascending**, case-insensitive, id tiebreak (recommended: a navigator is a browse surface — you find a type by scanning alphabetically); (b) `createdAt` desc, mirroring OQ-20/OQ-21 for one uniform ordering contract.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-22).
**Decision:** Name ascending — case-insensitive, ties broken by id — for annotation-type nodes and for group nodes alike. The synthetic `Ungrouped` node is **pinned last**, after every named group, regardless of collation. Deliberately diverges from OQ-20/OQ-21: their recency rule exists to keep rows from jumping between *pages*, which an unpaginated browse tree never does. — decided by rafaelsantos, 2026-08-22.

### OQ-26 — Lint/static-analysis signal for the API harness
**Severity:** 🟢 Tactical
**Description:** The `lint` CI signal shipped from setup as `echo 'lint placeholder - Checkstyle/Spotless pending (F3)'` — a step that cannot fail, reporting a green check that verified nothing. It was **removed** from `harness.signals` on 2026-08-24 (feat-014 publish) rather than left faking a pass: visible absence over fake presence, per `instructions/feature-publish.md`. The "F3" the comment referenced was never tracked anywhere. The open question is what should replace it, and when.
**Impact:** No lint or format enforcement runs in CI. `constitution/03-code-standards.md` names Checkstyle as the enforcement point for import order, field-declaration order and naming conventions — all of which are currently reviewer-enforced only, so they drift silently between audits.
**Suggested path:** Human decision. Options: (a) **Spotless + a Checkstyle ruleset** matching 03-code-standards, wired with `wf harness signal lint "mvn -B spotless:check checkstyle:check"` — the fullest fit, but expect a first run flagging violations across the existing codebase, so it needs its own feature; (b) Spotless formatting only (cheaper, catches import order and whitespace, not naming); (c) leave it unwired and keep the standards reviewer-enforced, accepting the drift.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-28).
**Decision:** Wire Checkstyle into the Maven build at the validate phase and register 'lint' as a harness signal, so it runs in verify and as its own CI step. The config enforces the enforceable half of constitution/03-code-standards.md — naming, import grouping, Javadoc on the public surface, line length — and deliberately omits field declaration order, which the constitution itself says no formatter can express and leaves to review and /audit. It uses ImportOrder rather than the CustomImportOrder the constitution names, because the documented rule has four package families and CustomImportOrder expresses only three. — decided by rafaelsantos, 2026-08-28.

### OQ-27 — Aggregate counts for the Navigator and the groups screens
**Severity:** 🟡 Important
**Description:** Both design screens show aggregates the shipped feat-014 API does not return. Screen 05's tree carries a count per node (`Service Endpoint (12)`, `Migration (8)`, `Ungrouped (3)`); screen 14's group tables show **Annotations** and **Types used** per annotation group, and **Tasks** and **Avg. status** per task group. feat-014's spec deliberately put node counts out of scope ("no FR names them; adding them would put an unbounded aggregate behind every node"), so the web has no source for any of it.
**Impact:** Blocks feat-015's tree and groups screens as designed. The alternative — deriving each count client-side from the filtered listing — is one HTTP round trip per node on first paint, and `Avg. status` would require fetching every task in the group.
**Suggested path:** Human decision. Options: (a) **add the aggregates to the API** — additive fields on the `/navigation` nodes and the group listing (recommended: computed where the data lives, one round trip); (b) drop counts from the web and amend both design screens; (c) compute client-side, accepting the N+1.
**Depends on:** Human decision.
**Status:** ✅ resolved (2026-08-24).
**Decision:** Add the aggregates to the API. `/navigation` group nodes gain a member count; the group listing gains `itemCount` plus `typesUsed` (annotation domain) and `averageStatus` (task domain). **Additive only** — no existing field changes shape, so nothing feat-014 shipped breaks. Because feat-014 is already merged and closed, this lands as its own small API slice that must ship **before** feat-015 can implement the count display. — decided by rafaelsantos, 2026-08-24.

### OQ-28 — Locale control on the sign-in screen (design 02/03): dead affordance, wired for the login screen only, or deferred to E5?
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** Blocks feat-017-login-layout.plan — the spec's action-block scenario names the control. Handoff screens 02/03 place an English/Português select beside Remember me; the app has no locale switcher and runtime locale selection is FR-15/FR-16 (E5), unbuilt. A select that visibly changes and does nothing is a worse lie than an inert link.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-27).
**Decision:** Option (b): the locale control works for the sign-in screen only. It switches that screen's own language (labels, placeholders, validation errors, brand-pane copy), persists nothing, and makes no request. After sign-in the member's stored locale preference supersedes it. No locale switcher is introduced on any other screen — full runtime locale selection stays with FR-15/FR-16 (E5). — decided by rafaelsantos, 2026-08-27.

### OQ-29 — Flaky in-flight assertion in notebox-web LoginForm.test.tsx: adopt the deferred-promise pattern the repo already uses, or something else?
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** `disables the submit control while a sign-in is in flight (scenario A5)` races a synchronous getByRole against an MSW handler's delay(60): under full-suite load the login resolves before the assertion runs and the button has already reverted to 'Entrar'. Observed red once during feat-017 T-01's verify, green on isolation and on re-run — a load-dependent flake, not a regression. It will keep breaking CI at random. The repo already solves this shape in components/annotationTypes/TypeBuilderForm.test.tsx with a manually released promise instead of a timed delay. Out of scope for feat-017's diff (feature-implement.md: bugs noticed elsewhere go to backlog, not into the feature diff).
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-28).
**Decision:** Adopt the repo's existing deferred-promise pattern (TypeBuilderForm.test.tsx): the login handler now awaits a promise released by hand instead of a timed delay(60), and the in-flight assertion uses findByRole. The clock is out of the test entirely. — decided by rafaelsantos, 2026-08-28.

### OQ-30 — The branding bar shows the tenant UUID where design 05 shows the tenant slug ('acme-ops') — add tenant name/slug to the /me response, or render nothing?
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** notebox-web renders `me.tenantId` because the /me contract carries no tenant name or slug. Design screen 05 shows 'Tenant: acme-ops'. Fixing it properly is an API change (feat-006's /me contract), so it is out of scope for a web-only conformance feature. Rendering a raw UUID to the member is worse than useless — it is unreadable and it publishes an internal id in the chrome of every protected screen. Found in feat-018's live pass 2026-08-27.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-28).
**Decision:** Add tenantName and tenantSlug to the /me response. A new TenantRepository reads the tenant row directly — deliberately NOT tenant-scoped, since the tenant is not a row inside a tenant — and the isolation rests on the caller passing the id from the token, which MeResource does. notebox-web now names the workspace instead of printing its UUID. — decided by rafaelsantos, 2026-08-28.

### OQ-31 — Design 05's Overview screen has summary boxes (types defined, records, with images, secret fields; tasks in flight) that the app's home screen does not render — build them, or drop them from the conformance scope?
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** The workspace home currently shows only a greeting. Design 05 fills the main panel with af-panelBox summary cards and an 'af-subHeader: Tasks in flight' section. The numbers would need aggregate endpoints that do not exist (annotations count by type, records with images, secret-field count), so this is an API feature, not a styling gap. Found in feat-018's live pass 2026-08-27.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-28).
**Decision:** Build them. A read-only GET /overview returns the counts the design's boxes need — types, records, records with images, secret fields, open tasks, subtasks done/total, tasks ending this week — computed in the database and scoped to the caller's tenant with no caller-supplied filter. The home screen renders the two panels from it. — decided by rafaelsantos, 2026-08-28.

### OQ-32 — Records sub-grid inside the annotation types list (screen 07)
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** The types list shows a Records count that leads nowhere; seeing what a type holds costs two clicks through an intermediate screen. Product-owner asked for an expandable sub-grid per row, in the ADF Fusion idiom (af:table detailStamp: a disclosure column, the detail rendered full-width beneath the row). DECIDED 2026-08-30, option A over a uniform preview: each row expands into that type's OWN record grid, with the columns its visible fields define. Cost accepted: the column set changes per expanded row, so rows are not comparable, and a type with no records still draws a header over nothing. UNDECIDED and blocking the spec: (1) pagination inside an expanded row — NFR-08 defaults list endpoints to 50, and a row cannot hold a 50-row grid; cap, page-within-row, or truncate-with-a-link? (2) what an empty type shows when expanded; (3) whether more than one row may be open at once; (4) keyboard reachability of the nested grid's row actions. Deviation from the handoff, which draws 07 and 11 as separate screens — NFR-09 measures against it, so this needs recording like the Administration tab did.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-30).
**Decision:** Option A confirmed, in the af:table detailStamp idiom: a disclosure column on the types grid, the detail rendered full-width beneath the row, carrying that type's OWN record grid with the columns its visible fields define. (1) VOLUME — the expanded row lists the first 10 records and closes with a link naming the true total ('see all 47 records'), which navigates to the full grid. One request per opened row at size=10; no pagination state inside a row. Honest about what it is not showing, and NFR-08's default is respected rather than excepted. (2) MULTIPLE ROWS — each row keeps its own open state and several may be open at once, as the navigator's folders already do and as ADF's detailStamp does; nothing closes that the user did not close. (3) EMPTY TYPE — the expanded band shows the grid's existing empty row, no new widget. (4) KEYBOARD — not a product choice: the disclosure control is in the tab order, aria-expanded reflects its state, and the nested grid's row actions are reachable. Accepted cost, restated so it is not relitigated: the column set changes per expanded row, so rows are not comparable across the list, and a type with no records still draws its header over an empty band. — decided by rafaelsantos, 2026-08-30.

### OQ-34 — feat-020 left two small test-coverage gaps: the band link's route string is asserted only by hand, and two toggles in one tick fire two identical records requests
**Severity:** 🟢 Tactical
**Description:** <what is unknown and why it matters>
**Impact:** F-04: a typo in page.tsx's route template ships a 404 from the band's only control with the whole suite green — the live pass caught it by navigating, no test does. F-05: toggle() reads open/bands from the render closure, so a second toggle before React re-renders sees an empty cache and calls load() again; measured as two identical GET /annotation-records requests, violating INV-B5. NOT reachable by a real double-click (verified: one request), so it is a latent path, not a user-facing defect. Both are cheap: an integration assertion on the route, and functional setState plus an in-flight guard. Decide whether to fix them in feat-021 (which already touches navigation) or leave them tracked.
**Suggested path:** <how to resolve>
**Status:** ✅ resolved (2026-08-31).
**Decision:** Fold both into feat-021, which already touches navigation — the route assertion lands next to the tree's own destination tests, and the double-toggle guard next to the band's fetch. Neither is worth a feature of its own, and leaving them tracked-but-unfixed would let a known-latent path age into a surprise. — decided by rafaelsantos, 2026-08-31.

### OQ-35 — In-grid editing across the whole system: inline edit of visibleForViewing fields in every data grid, plus a marker that opens an expandable side panel with all fields
**Severity:** 🟡 Important
**Description:** <what is unknown and why it matters>
**Impact:** Raised by rafaelsantos 2026-08-31 during the /wf-fix visual collect, and deliberately kept OUT of the fix lane: it adds behaviour to every grid on every screen, so it needs a spec. Open decisions, none of them mine to make: (a) what starts the edit — clicking the cell, a row control, a keyboard key; (b) save granularity — per field on blur, or per row on commit, and what happens when a save fails mid-row; (c) whether BR-05's confirmation applies to an inline destructive edit; (d) Secret fields (C-12) — a listing must never offer reveal, so an inline editor must not become one; (e) what 'expand' means for the existing af-drawer (NotesDrawer, design 16), which today is open/closed with no second width; (f) whether the band's inner grid in the types list is editable too, or reading only as feat-020 decided. Reuse target identified: af-drawer / af-drawerDock / af-drawerTab, not a new pattern. Sequenced AFTER the current visual fix lane finishes. **EXCEPTIONS named by the human 2026-09-01:** the TASKS grid **and the SUBTASKS grid** must NOT edit inline — it opens a popup for editing instead, plus an icon to delete. Recorded because the spec would otherwise have generalised 'inline everywhere' across every screen and got this one wrong; the reason for these exceptions is not stated and should be captured when specced (plausibly a task carries fields a row cannot hold — dates, card, subtasks — but that is inference, not the human's words). Also from the same collect: the RECORD detail and RECORD edit screens, and the NEW TASK screen, are all to be replaced by the side panel, so the panel is not an add-on to the grids — it becomes the primary surface for viewing and editing records. **OPEN DESIGN QUESTION raised by the human 2026-09-01, and the sharpest one:** if a screen opens inside the side panel and one of its actions is destructive, BR-05 requires a confirmation — so what does a dialog inside a panel look like? Options to weigh, none chosen: the confirmation replaces the panel's content with back/confirm; a small dialog centred over the panel alone; a normal app-level dialog over everything; or an inline confirmation within the panel beside the row. Every destructive action moved into the panel hits this, so it is decided once here rather than improvised per screen. **And it generalises (human, same day): the CREATION dialogs — new annotation, new task, new group — will also be opened from inside the panel, so the question has TWO cases, not one. They may need different answers: a confirmation is transient, while a creation form holds unsaved input, so replacing the panel's content risks losing what was typed.** **ICON VOCABULARY (human, 2026-09-01):** the Actions column ends up carrying four icons — enter-edit, confirm-edit, cancel-edit, delete-row — all with localized tooltips. The human explicitly requires cancel-edit to be VISUALLY DISTINCT from delete, having first misread the message catalog's cancel `✕` as a delete button before correcting themselves: both icons sit in the same column and one of them is irreversible. The message-catalog grid already ships a working confirm/cancel inline-edit pair, so the spec has a real precedent to copy rather than a pattern to invent. **ENTRY POINTS (human, 2026-09-01):** the Workspace menu's New annotation type / New task / Manage groups open in the panel as well, so the same destination is reachable from a menu item, a grid action icon and an empty-state button. The spec must treat 'open X in the panel' as ONE thing many callers invoke, or the three paths will drift apart.
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
| 2026-08-03 | OQ-18 opened: Secret flag flip on a field with existing values — cleartext/ciphertex |
| 2026-08-03 | OQ-17 resolved: Guard + preserve unchanged (interim guard ships in feat-005, symmetric… |
| 2026-08-03 | OQ-18 resolved: Block the flip while values exist (guard family of OQ-14/OQ-17): the t… |
| 2026-08-11 | OQ-19 resolved: Rich text now, in feat-006: annotation Free-text VALUES are rich text … |
| 2026-08-12 | OQ-20 resolved: Newest first: the listing is ordered by createdAt descending (ties bro… |
| 2026-08-13 | OQ-21 opened: Task listing sort order (feat-010 spec; OQ-20 covered records only) |
| 2026-08-13 | OQ-22 opened: Task status representation for non-integer proportions (feat-010 spec) |
| 2026-08-13 | OQ-21 resolved: Newest first mirroring OQ-20 (createdAt desc, id tiebreak) — uniform li… |
| 2026-08-13 | OQ-22 resolved: Integer percent, rounded half up (1/3 → 33); 0% with no subtasks per BR… |
| 2026-08-26 | OQ-28 opened: Locale control on the sign-in screen (design 02/03): dead affordance,  |
| 2026-08-27 | OQ-28 resolved: Option (b): the locale control works for the sign-in screen only. It s… |
| 2026-08-27 | OQ-29 opened: Flaky in-flight assertion in notebox-web LoginForm.test.tsx: adopt the |
| 2026-08-27 | OQ-30 opened: The branding bar shows the tenant UUID where design 05 shows the tenan |
| 2026-08-27 | OQ-31 opened: Design 05's Overview screen has summary boxes (types defined, records, |
| 2026-08-28 | OQ-29 resolved: Adopt the repo's existing deferred-promise pattern (TypeBuilderForm.te… |
| 2026-08-28 | OQ-30 resolved: Add tenantName and tenantSlug to the /me response. A new TenantReposit… |
| 2026-08-28 | OQ-31 resolved: Build them. A read-only GET /overview returns the counts the design's … |
| 2026-08-28 | OQ-26 resolved: Wire Checkstyle into the Maven build at the validate phase and registe… |
| 2026-08-30 | OQ-32 opened: Records sub-grid inside the annotation types list (screen 07) |
| 2026-08-30 | OQ-32 resolved: Option A confirmed, in the af:table detailStamp idiom: a disclosure co… |
| 2026-08-31 | OQ-33 opened: the Navigator's Annotations root node reaches no list screen, so feat-020's sub-grid has no route from the tree |
| 2026-08-31 | OQ-33 resolved: option (a) — the Navigator's root nodes navigate (Annotations → the types list, Tasks → the tasks list); recorded deviation from design 05's inert root labels |
| 2026-08-31 | OQ-34 opened: feat-020 left two small test-coverage gaps: the band link's route stri |
| 2026-08-31 | OQ-34 resolved: Fold both into feat-021, which already touches navigation — the route … |
| 2026-09-02 | OQ-37 opened: the message catalog reaches 93 server messages and none of the 425 interface strings — US-5.2 is half true, and feat-026 depends on the answer |
| 2026-09-01 | OQ-36 opened: a dark theme needs 169 colours tokenised and a palette designed — raised with its cost rather than half-shipped |
| 2026-09-01 | OQ-35 opened: In-grid editing across the whole system: inline edit of visibleForView |

### OQ-33 — The Navigator's `Annotations` node does not reach the types list, where feat-020's sub-grid lives
**Severity:** 🟡 Important
**Description:** In the Navigator, the `Annotations` root is a plain `<span className="nb-treeLabel">` (`components/navigation/Navigator.tsx:163`): only its twisty responds, and the node itself navigates nowhere. Its leaves — the types — go straight to `/annotation-types/{id}/records`, skipping the types list entirely. So from inside the tree there is no route to `/annotation-types`.

**Not a regression, and not introduced by feat-020.** The tree has been shaped this way since feat-015, and `/annotation-types` IS reachable: the level-1 `Annotations` tab in `AppNav` goes there (verified live 2026-08-31), as does the `Annotations` breadcrumb on the records screen. What changed is the cost of the gap: feat-020 put a records sub-grid on the types list, so that screen is now worth visiting, while the tree's only annotation destination is the one screen that bypasses it. The same asymmetry exists for `Tasks`, whose root node is also inert.

**Impact:** A member working from the tree — the primary navigation surface on every annotation screen — cannot reach the sub-grid without going up to the tab strip. Discoverability of the feature just built depends on a control in a different region of the chrome.

**Suggested path:** Three options, in ascending cost:
(a) make the `Annotations` root node navigate to `/annotation-types` (and `Tasks` to `/tasks`), matching what the tab already does — the tree gains a destination it visibly lacks, one component, no new screen;
(b) leave the tree as an index of *content* and treat the tab strip as the only route to list screens — record the asymmetry as intentional so no future audit reopens it;
(c) point the type leaves at the types list with the row pre-expanded, making the sub-grid the tree's landing surface — the largest change, and it would displace today's records destination.

Design screen 05 draws the tree without root-node links, which is why it was built this way; a deviation here would need recording like the Administration tab did.

**Status:** ✅ resolved (2026-08-31). Raised by rafaelsantos from live use during feat-020's audit stage. **Deliberately NOT folded into feat-020's diff** (CLAUDE.md §7: a defect noticed elsewhere goes to the backlog or an OQ, never into a feature diff under audit) — it ships as its own feature.
**Decision:** Option (a). The Navigator's root nodes navigate: `Annotations` → `/annotation-types`, `Tasks` → `/tasks`, matching the destinations the level-1 tabs already carry. The tree keeps its shape and its twisties; what changes is that the root label becomes a control rather than dead text, and the tree stops being the one surface from which a member cannot reach a list screen. **Recorded deviation from design 05**, which draws the tree with inert root labels — the same kind of deviation as the Administration tab (NFR-09 measures against the handoff, so it is written down rather than applied silently). — decided by rafaelsantos, 2026-08-31.

## Rules
- IDs immutable. Resolved → mark ✅ with a reference. New → next sequential ID.
- Every feature spec lists the OQs that affect it.
| 2026-08-22 | OQ-23 opened+resolved: navigation tree composes type → group; groups hold records/tasks, tree stops at group nodes. |
| 2026-08-22 | OQ-24 opened+resolved: deleting a non-empty group un-groups its members; no record or task is deleted (diverges from OQ-14). |
| 2026-08-22 | OQ-25 opened+resolved: tree siblings ordered by name asc (case-insensitive, id tiebreak); `Ungrouped` pinned last. |
| 2026-08-24 | OQ-26 opened: lint signal removed from CI rather than left faking a pass; what replaces it is undecided. |
| 2026-08-24 | OQ-23 clarified: its type→group ruling governs the payload; the Navigator renders group→type per design screen 05, inverted client-side. |
| 2026-08-24 | OQ-27 opened+resolved: aggregate counts added to the API (additive), as a slice that must precede feat-015's implement. |

### OQ-36 — A dark theme means tokenising 169 hardcoded colours and designing an ADF Fusion dark palette

**Opened:** 2026-09-01, during feat-027 · **Status:** open, needs the product owner

The product owner asked for Preferences to hold *"idioma + tema (claro/escuro)"*. The language half
shipped in feat-027. The theme did not, and the reason is a measurement rather than a preference:

```
src/styles/adf-fusion.css   169 distinct hex colours,  0 CSS custom properties
```

A theme toggle needs three things, and only the first is mechanical:

1. **The mechanism** — a `data-theme` attribute, a stored choice, a `prefers-color-scheme` default.
   Small, and genuinely a few hours.
2. **Tokenisation** — every one of those 169 colours replaced by a variable named for its *role*
   (surface, border, header, selected row, badge). Naming by role means reading each usage; a
   mechanical `--c-17: #123456` mapping would be worse than the hardcoding it replaces.
3. **A dark palette, designed.** Which of the theme's blues becomes which in the dark is a design
   decision. Inventing it is inventing product.

**Why it was not half-shipped.** Darkening the shell while the Fusion widgets stay light does not
read as unfinished — it reads as broken, and it would be reported as a bug against every screen.

**The question for the product owner:** is (2)+(3) worth its own feature, and if so, is there a
dark palette they want followed — an ADF Fusion dark skin, or the design handoff extended? Without
an answer to the palette, the work cannot start honestly.

### OQ-37 — The message catalog reaches 93 server messages and none of the 425 interface strings

**Opened:** 2026-09-02, while specifying feat-026 · **Status:** open, blocks feat-026

Brief item 24.3 asks for a way to add a locale at runtime. Specifying it surfaced that the
foundation it needs is not there, and that **US-5.2's promise is currently half true**:

> *"As a tenant administrator, I want to edit translations at runtime, so wording changes need no
> deploy."*

| | keys | reachable from the catalog screen? |
|---|---|---|
| API messages (`messages.properties`) | **93** | **yes** — `TranslationCatalog` lists them, overrides apply, and the UI shows the API's messages verbatim (AD-05) |
| Interface strings (`lib/i18n/messages/en.ts`) | **425** | **no** — `I18nProvider` never fetches anything; `t()` reads a bundled TypeScript object |

So an administrator can reword *"Invalid email or password."* without a deploy, and cannot reword
a single column header, button or tooltip. Adding a locale is impossible for the interface at all:
`type Locale = 'en' | 'pt'` is a compile-time union over two bundled catalogs.

**Nothing is broken** — every test is honest about what it tests, and the catalog screen does
exactly what it says for the keys it lists. What is wrong is the *scope* of the promise.

### What making it true would take

1. `I18nProvider` fetches `/translations?locale=X` after sign-in and merges the tenant's overrides
   **over** the bundled catalog, which stays as the default and the fallback.
2. The catalog screen must list the interface keys, which means the web's 425 keys have to reach
   the API — a build step that ships them, or an endpoint the web registers them through.
3. A decision about first paint: the bundled strings render immediately and the overrides arrive a
   moment later, so a reworded label visibly changes after load unless the fetch blocks the shell.
4. Only then does "add a locale" mean anything: a new locale starts as English throughout and is
   translated key by key.

### The question

Is (1)–(4) worth a feature — it spans both projects and touches every screen's first paint — or is
the honest move to **narrow US-5.2** to what it actually delivers (server messages), and say so in
the PRD? Either answer is defensible. Inventing the architecture without asking is not.

---

### OQ-38 — Does a subtask record when it was completed?

**Opened:** 2026-09-02, from a direct product-owner request · **Status:** ✅ resolved (2026-09-02)

The product owner asked for an indicator, with a localized tooltip, telling a member that a subtask was
finished **after its planned end date**. Specifying it surfaced that the product stores no such fact:
a subtask carries a `done` boolean and nothing else about completion.

**Why deriving it does not work.** Without a stored moment, "finished late" can only be computed as
*done, and the end date is in the past*. That expression is **false when it should be true and true when
it should be false**: a subtask finished on time starts reporting itself late the day today walks past
its end date, and nothing distinguishes it from one genuinely delivered late. The information is
destroyed at the moment of completion unless it is written down then.

**Decision.** A completion moment is **persisted** when a subtask becomes done, and **cleared** when it
stops being done. The API records and exposes the moment; the late/on-time comparison belongs to the
client, which already holds the planned end date in the same payload — recorded as a spec decision in
`features/029-subtask-completion-notebox-api/spec.md` (Out of scope) so the approval gate can overturn
it. Subtasks completed before this exists carry no moment and are never reported as late; the moment is
unrecoverable and inventing one would manufacture a verdict.

— decided by rafaelsantos, 2026-09-02.

**Delivered by:** feat-029-subtask-completion-notebox-api (this fact) · feat-030-subtask-completion-notebox-web
(the question about a missing start date, the auto-filled end date, and the indicator).

---

### OQ-39 — No test can tell a correct completion moment from a wrong one

**Opened:** 2026-09-03, while planning feat-029 · **Status:** open, non-blocking

The twelve scenarios in `features/029-subtask-completion-notebox-api/spec.md` all assert the moment's
**presence or absence**, because there is no clock seam anywhere in this codebase — ten entities call
`Instant.now()` directly and no test bounds an instant between two captured values.

The consequence is uncomfortable and worth writing down: an implementation that stored `getCreatedAt()`,
or `Instant.EPOCH`, would **pass all twelve**. The one thing this feature exists to get right — that the
recorded moment is the moment completion actually happened — is the one thing no automated check covers.

**Impact.** A wrong-but-present moment produces a wrong lateness verdict in feat-030, on every subtask,
silently. A moment equal to creation time would mark work completed the same day as finished late.

**Suggested path.** The live pass in feat-030 is the only check available today: complete a subtask whose
planned end is in the future and confirm no late indicator appears. A real fix is a `Clock` seam, which
feat-029's plan rejects as out of proportion for one field but prices at two call sites
(`markDone(boolean, Instant)`) with no schema change — see its rejected alternative 4.

**Depends on:** nothing. **Raised by:** the design panel's own advocate for the clock-seam option, which
is the only one of the three proposals that found this hole.

---

### OQ-40 — Nothing sets a JDBC time zone, and the stored timestamps carry none

**Opened:** 2026-09-03, while planning feat-029 · **Status:** open, non-blocking

`DATETIME(6)` carries no zone and nothing in the configuration sets a JDBC time zone, so every `Instant`
round-trip rides the JVM default zone. This is **pre-existing** and equally true of every `created_at`
and `updated_at` in the schema — feat-029 introduces nothing new here.

It is recorded now because feat-029 is the first feature whose entire value is the **correctness of a
moment**. A deployment whose application JVM and database disagree on zone would show a systematic offset
that, for created/updated timestamps, nobody would notice — and that, for a completion moment compared
against a planned date, silently shifts lateness verdicts near midnight.

**Suggested path.** Decide and pin the zone explicitly (UTC everywhere is the obvious candidate) as a
cross-cutting change, not inside a feature that only surfaced it.

**Depends on:** nothing. **Related:** OQ-39 — both are about the moment being right rather than present.
