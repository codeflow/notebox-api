# ROADMAP — the project

> Executive index of what's delivered, what's next, and where decisions live. Language: English.
> **Last updated:** 2026-08-22

This is an index — it does not duplicate canonical sources:
- 🔗 Open questions: [`catalogs/open-questions.md`](catalogs/open-questions.md)
- 🔗 Requirements: [`catalogs/requirements.md`](catalogs/requirements.md)
- 🔗 Epics & user stories: [`catalogs/epics.md`](catalogs/epics.md)
- 🔗 PRD: [`prd/PRD_v2.md`](prd/PRD_v2.md)

## 1. General state
| Layer | Stack | State |
|-------|-------|-------|
| API | Java 17 / Quarkus / Jakarta EE / MySQL | E1, E2, E6 delivered on `develop`; promotion to `main` pending |
| Web (satellite) | react / next (`notebox-web`) | E1, E2, E6 delivered on `develop`; promotion to `main` pending |

## 2. Delivered ✅ (on `develop`, promotion to `main` pending)
- [x] Repo scaffolded with SDD + Harness pipeline (2026-07-19).
- [x] **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web) (PR #2, each repo).
- [x] **US-1.1** annotation types — feat-003 (api, PR #4, 2026-07-23) + feat-004 (web, PR #5, 2026-07-24).
- [x] **US-2.1** annotation records — feat-005 (api, PR #6, 2026-08-11) + feat-006 (web, PR #7, 2026-08-12) + feat-007 rich-text sanitization (api, PR #8, closed C-08).
- [x] **US-2.2** listing & detail — feat-008 (api, PR #10, 2026-08-12) + feat-009 (web, PR #9, 2026-08-12; audit Round 1 pass, 300 tests). Absorbs US-1.2's visible-column projection.

## 2b. Delivered this wave ✅
- [x] **US-4.1** tasks & subtasks with derived progress — feat-010 (api, PR #12, 2026-08-14; AD-10 CDI events inaugurated) + feat-011 (web, PR #11, 2026-08-15; first nav chrome, 355 tests + live pass). On `develop`, promotion pending.

## 2c. Delivered this wave ✅
- [x] **US-4.2** task dates, card link and rich-text details — feat-012 (api, PR #14, 2026-08-18; second AD-10 observer, first `@Embeddable`, feat-007 sanitizer both ways) + feat-013 (web, PR #13, 2026-08-22; derived dates displayed never typed, inline card, Details tab on the shipped rich-text stack, full-echo builders close the rollout hazard; 402 tests + live pass). On `develop`, promotion pending. **E4 delivered.**

## 3. Next waves 🌊
### Deferred / dependencies
- [ ] **Promote develop → main** (`/wf-promote`) — thirteen features stacked, no compliance blockers remaining; ship feat-012 + feat-013 together (PUT-replace × legacy-web rollout note).
- [ ] **E3** groups & navigation (US-3.1), **E5** i18n (US-5.1, US-5.2) — no features yet; scope via `wf feature add`.
- [ ] **`release` pipeline step** — final handoff, roadmap and memory close-out.

## 4. Open questions
➡️ See [`catalogs/open-questions.md`](catalogs/open-questions.md) — single source of truth (none pending as of 2026-08-12).
