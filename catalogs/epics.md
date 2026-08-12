# Catalog — Epics & User Stories

> Living backlog, user stories grouped by epic. IDs immutable; deprecate, never reuse. Language: English.
> Status vocabulary: `todo | speccing | building | delivered | deferred`.

**Last sync with PRD:** v2 (2026-07-24)

## Epics overview

| ID | Epic | Focus | Status |
|----|------|-------|--------|
| E0 | Foundations & Harness | repo, CI/CD, agentic pipeline | todo |
| E1 | Annotation types | define typed note schemas | delivered |
| E2 | Annotations | typed records + listing/detail | speccing |
| E3 | Groups & navigation | organize items, nav tree | todo |
| E4 | Tasks & subtasks | tasks, derived progress, cards, rich text | todo |
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
| US-2.1 | As a tenant member, I want to create/edit/delete annotations of a type, so I record real data. | FR-04, FR-06, FR-18 · BR-03, BR-05, BR-10 | delivered | feat-005-annotation-records-notebox-api (api) — **merged into `develop` 2026-08-11** (PR #6, squash `0f056d7`; audit Round 4 pass, 133 tests green); promotion to `main` pending · feat-006-annotation-records-notebox-web (notebox-web) — **merged into `develop` 2026-08-12** (PR #7, squash `1f6e34a`; audit Round 2 pass, 277 tests). Both sides delivered **on develop**; promotion to `main` pending. ~~Promotion blocker~~ **closed by feat-007** (rich-text sanitization, audit pass 2026-08-12; pending its merge to develop). Includes secret-value encryption + audited reveal (FR-18); enforces type-delete-block when records exist (OQ-14) |
| US-2.2 | As a tenant member, I want a listing exposing visible fields plus a detail view exposing all fields, so grid and detail have what they need. | FR-05, FR-07 · BR-09 | todo | — · **absorbs US-1.2's FR-05 visible-column projection** (the `visibleForViewing` flag itself ships in feat-003); depends on US-2.1 records |

## E3 — Groups & navigation

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-3.1 | As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace. | FR-08, FR-09 | todo | — |

## E4 — Tasks & subtasks

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-4.1 | As a tenant member, I want tasks with subtasks whose completion drives the task's progress, so status reflects real work. | FR-10, FR-11 · BR-06 | todo | — |
| US-4.2 | As a tenant member, I want task dates derived from subtasks, a card link, and rich-text details, so a task is self-contained. | FR-12, FR-13, FR-14 · BR-07 | todo | — |

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
