# ROADMAP — the project

> Executive index of what's delivered, what's next, and where decisions live. Language: English.
> **Last updated:** 2026-07-23

This is an index — it does not duplicate canonical sources:
- 🔗 Open questions: [`catalogs/open-questions.md`](catalogs/open-questions.md)
- 🔗 Requirements: [`catalogs/requirements.md`](catalogs/requirements.md)
- 🔗 Epics & user stories: [`catalogs/epics.md`](catalogs/epics.md)
- 🔗 PRD: [`prd/PRD_v1.md`](prd/PRD_v1.md)

## 1. General state
| Layer | Stack | State |
|-------|-------|-------|
| API | Java 17 / Quarkus / Jakarta EE / MySQL | 3 features on `develop`, awaiting promotion to `main` |
| Web (satellite) | react / next (`notebox-web`) | US-6.1 delivered; US-1.1 web pending |

## 2. Delivered ✅ (on `develop`, promotion to `main` pending)
- [x] Repo scaffolded with SDD + Harness pipeline (2026-07-19).
- [x] **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web) (PR #2).
- [x] **US-1.1** annotation types (api) — feat-003 (PR #4, 2026-07-23; audit pass-with-findings).

## 3. Next waves 🌊
### Wave — E1 Annotation types (in progress)
- [ ] **feat-004-annotation-types-web** (US-1.1, notebox-web) — the type-builder UI over feat-003's contract. ← next
### Deferred / dependencies
- [ ] **Promote** develop → main for feat-001/002/003 (`/wf-promote`).
- [ ] **US-2.1** annotation records — unblocks Secret value encryption (OQ-15) + type-delete policy (OQ-14).
- [ ] **feat-001 error/i18n realignment** (OQ-16).

## 4. Open questions
➡️ See [`catalogs/open-questions.md`](catalogs/open-questions.md) — single source of truth.
