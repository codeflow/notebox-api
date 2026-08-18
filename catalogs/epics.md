# Catalog — Epics & User Stories

> Living backlog, user stories grouped by epic. IDs immutable; deprecate, never reuse. Language: English.
> Status vocabulary: `todo | speccing | building | delivered | deferred`.

**Last sync with PRD:** v2 (2026-07-24)

## Epics overview

| ID | Epic | Focus | Status |
|----|------|-------|--------|
| E0 | Foundations & Harness | repo, CI/CD, agentic pipeline | todo |
| E1 | Annotation types | define typed note schemas | delivered |
| E2 | Annotations | typed records + listing/detail | delivered |
| E3 | Groups & navigation | organize items, nav tree | todo |
| E4 | Tasks & subtasks | tasks, derived progress, cards, rich text | building |
| E5 | Internationalization | localized strings + translation mgmt | todo |
| E6 | Identity & tenancy | JWT auth, multi-tenant isolation | speccing |

## E0 — Foundations & Harness

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-0.1 | As the team, I want the SDD + Harness pipeline configured so agents ship predictably. | — | delivered | — |

## E1 — Annotation types

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-1.1 | As a tenant member, I want to define an annotation type with an icon and typed fields (incl. list options), so I capture a category of notes consistently. | FR-01, FR-02, FR-03, FR-07 · BR-03, BR-04 | delivered | feat-003-annotation-types (api) ✔ on develop (PR #4, merged 2026-07-23, audit pass-with-findings), awaiting promotion to main; feat-004-annotation-types-web (notebox-web) ✔ on develop (PR #5, squash-merged 2026-07-24, audit pass after fixing server-violation routing), awaiting promotion to main |
| US-1.2 | As a tenant member, I want to mark fields "visible for viewing", so listings show only the columns I care about. | FR-02, FR-05 · BR-09 | deferred | **Deferred into US-2.2 (2026-07-24).** The FR-02 "mark" concern — declaring/updating the `visibleForViewing` flag with per-type defaults and exposing it on read — was already delivered by feat-003 (`AnnotationTypeInput`/`AnnotationTypeDto`). BR-09 makes the flag display-only, so there is no separate API surface. The only residual — a listing that projects visible-field columns (FR-05) — is annotation-record behaviour that needs records (US-2.1) and is already US-2.2's scope. Provisional feat-005/feat-006 were created then removed once this overlap was found. |

## E2 — Annotations

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-2.1 | As a tenant member, I want to create/edit/delete annotations of a type, so I record real data. | FR-04, FR-06, FR-18 · BR-03, BR-05, BR-10 | delivered | feat-005-annotation-records-notebox-api (api) — **merged into `develop` 2026-08-11** (PR #6, squash `0f056d7`; audit Round 4 pass, 133 tests green); promotion to `main` pending · feat-006-annotation-records-notebox-web (notebox-web) — **merged into `develop` 2026-08-12** (PR #7, squash `1f6e34a`; audit Round 2 pass, 277 tests). Both sides delivered **on develop**; promotion to `main` pending. ~~Promotion blocker~~ **closed by feat-007** (rich-text sanitization, merged to develop 2026-08-12, PR #8, squash 060a8d2). Includes secret-value encryption + audited reveal (FR-18); enforces type-delete-block when records exist (OQ-14) |
| US-2.2 | As a tenant member, I want a listing exposing visible fields plus a detail view exposing all fields, so grid and detail have what they need. | FR-05, FR-07 · BR-09 | delivered | feat-008-annotation-listing-notebox-api (api) — **merged into `develop` 2026-08-12** (PR #10, squash `c5882d4`; audit pass w/ findings, 161 tests) · feat-009-annotation-listing-notebox-web (notebox-web) — **merged into `develop` 2026-08-12** (PR #9, squash `ad79bfe`; audit Round 1 pass, 300 tests). Both sides delivered **on develop**; promotion to `main` pending. **Absorbs US-1.2's FR-05 visible-column projection** (the `visibleForViewing` flag itself shipped in feat-003) |

## E3 — Groups & navigation

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-3.1 | As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace. | FR-08, FR-09 | todo | — |

## E4 — Tasks & subtasks

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-4.1 | As a tenant member, I want tasks with subtasks whose completion drives the task's progress, so status reflects real work. | FR-10, FR-11 · BR-06 | delivered | feat-010-tasks-notebox-api (api, issue #11) — **merged into `develop` 2026-08-14** (PR #12, squash `fb71054`; 212 tests) · feat-011-tasks-notebox-web (notebox-web, issue #10) — **merged into `develop` 2026-08-15** (PR #11, squash `6324257`; audit Round 1 pass w/ findings closed by hardening; 355 tests + live pass). Both sides delivered **on develop**; promotion to `main` pending |
| US-4.2 | As a tenant member, I want task dates derived from subtasks, a card link, and rich-text details, so a task is self-contained. | FR-12, FR-13, FR-14 · BR-07 | building | feat-012-task-details-notebox-api (api, issue #13) — **merged into `develop` 2026-08-18** (PR #14, squash `a295030`; audit Round 1 pass w/ findings, all 5 hardening items closed on the branch; 274 tests); promotion to `main` pending · feat-013-task-details-notebox-web (notebox-web, issue #12) — not started; **rollout note:** under PUT-replace semantics the shipped feat-011 UI clears API-set cards/details until feat-013 ships — the pair should reach `main` together |

## E5 — Internationalization

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-5.1 | As a user, I want all system text in my language (en/pt), so the product is usable bilingually. | FR-15 · BR-08 | todo | — |
| US-5.2 | As a tenant administrator, I want to edit translations at runtime, so wording changes need no deploy. | FR-16 · BR-08 | todo | — |

## E6 — Identity & tenancy

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-6.1 | As a tenant member, I want my data isolated from other tenants behind JWT auth, so my organization's data stays private. | FR-17 · BR-01, BR-02 | delivered | feat-001-identity-tenancy (api) ✔ delivered on develop (PR #2, awaiting promotion); feat-002-identity-tenancy-web (notebox-web) ✔ delivered on develop (PR #2, CI green, awaiting promotion) |

## History

| Date | PRD version | Change |
|------|-------------|--------|
| 2026-07-19 | v1 | Initial creation. |
| 2026-07-22 | v1 | Derived E1–E6 and US-1.1…US-6.1 from PRD v1; every FR maps to a US and every US to ≥1 FR. |
| 2026-07-24 | v1 | US-1.2 deferred into US-2.2: its FR-02 "mark visible" concern was already delivered by feat-003; the residual FR-05 visible-column listing needs US-2.1 records and is US-2.2's scope. E1 → delivered. |
| 2026-07-24 | v2 | Synced to PRD v2: US-2.1 now carries FR-18 (secret-value encryption + audited reveal) and BR-10, plus the OQ-14 type-delete-block rule. |
| 2026-08-11 | v3 | US-2.1 speccing → building: feat-005 (API) passed its audit at Round 4 after three remediation rounds. The story stays `building`, not `delivered` — feat-006 (web) has not started, and US-2.1 is only delivered when both sides ship. |
| 2026-08-12 | v4 | US-2.1 building → delivered (on develop): feat-006 (web) merged via PR #7 after audit pass at Round 2. Promotion to `main` gated on scoping the API-side C-08 rich-text sanitization obligation (OQ-19). |
| 2026-08-12 | v5 | feat-007 (rich-text sanitization) merged to develop via PR #8 — the C-08 promotion blocker on US-2.1 is closed; /wf-promote has no remaining compliance obstacle. |
| 2026-08-12 | v6 | US-2.2 building → delivered (on develop): feat-009 (web) merged via PR #9 (squash `ad79bfe`) after audit Round 1 pass (300 tests), joining feat-008 (api, PR #10). E2 → delivered — US-1.2's absorbed FR-05 visible-column projection ships with it. Promotion to `main` pending. |
| 2026-08-13 | v7 | US-4.1 todo → speccing: feature pair created (feat-010 api, feat-011 web; issues #11/#10) after US-4.1 was chosen over US-3.1 — FR-09's nav tree serves tasks, so tasks ship first to keep US-3.1's contract non-speculative. E4 → speccing. |
| 2026-08-13 | v8 | US-4.1 speccing → building: feat-010 (API) passed its audit at Round 1 (PASS with findings — 2 test-strength backlog items, 0 blockers; 209 tests). The story stays `building` — feat-011 (web) has not started; delivered only when both sides ship (US-2.1 precedent). |
| 2026-08-15 | v9 | US-4.1 building → delivered (on develop): feat-011 (web) merged via PR #11 (squash `6324257`) after audit Round 1 pass w/ findings closed by same-day hardening (355 tests + live pass against the real API), joining feat-010 (api, PR #12). E4 → building (US-4.2 still todo). Promotion to `main` pending. |
| 2026-08-15 | v10 | US-4.2 todo → speccing: feature pair created (feat-012 api, feat-013 web; issues #13/#12, slug `task-details`) — chosen over US-3.1 to finish E4 while the task aggregate is hot: FR-12's date derivation exercises AD-10's `SubtaskChange` seam early, FR-14 reuses feat-007 sanitization, and a complete task model keeps US-3.1's nav-tree contract non-speculative. |
| 2026-08-17 | v11 | US-4.2 speccing → building: feat-012 (API) passed its audit at Round 1 (PASS with findings — 0 blockers; 5 same-day hardening items incl. tightening `@AbsoluteHttpUrl` to require an authority; 268 tests). The story stays `building` — feat-013 (web) has not started; delivered only when both sides ship (US-4.1 precedent). Rollout hazard recorded: PUT-replace × legacy web clears API-set cards/details until feat-013 lands. |
| 2026-08-18 | v12 | feat-012 (US-4.2 API) merged into `develop` via PR #14 (squash `a295030`; 274 tests; branch + PR CI green). US-4.2 stays `building` pending feat-013; twelve features now stacked on `develop` awaiting promotion. |
