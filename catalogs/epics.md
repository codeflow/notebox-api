# Catalog — Epics & User Stories

> Living backlog, user stories grouped by epic. IDs immutable; deprecate, never reuse. Language: English.
> Status vocabulary: `todo | speccing | building | delivered | deferred`.

**Last sync with PRD:** v1 (2026-07-22)

## Epics overview

| ID | Epic | Focus | Status |
|----|------|-------|--------|
| E0 | Foundations & Harness | repo, CI/CD, agentic pipeline | todo |
| E1 | Annotation types | define typed note schemas | todo |
| E2 | Annotations | typed records + listing/detail | todo |
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
| US-1.1 | As a tenant member, I want to define an annotation type with an icon and typed fields (incl. list options), so I capture a category of notes consistently. | FR-01, FR-02, FR-03, FR-07 · BR-03, BR-04 | delivered | feat-003-annotation-types (api) — audit **pass with findings** (2026-07-23, @268fbe2); on feature/annotation-types, awaiting publish/review/promotion |
| US-1.2 | As a tenant member, I want to mark fields "visible for viewing", so listings show only the columns I care about. | FR-02, FR-05 · BR-09 | todo | — |

## E2 — Annotations

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-2.1 | As a tenant member, I want to create/edit/delete annotations of a type, so I record real data. | FR-04, FR-06 · BR-03, BR-05 | todo | — |
| US-2.2 | As a tenant member, I want a listing exposing visible fields plus a detail view exposing all fields, so grid and detail have what they need. | FR-05, FR-07 · BR-09 | todo | — |

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
