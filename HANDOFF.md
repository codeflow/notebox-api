# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-15

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
- **Just landed (2026-08-15):** feat-011 — the tasks & subtasks UI (US-4.1 web side). Squash `6324257`, audit Round 1 pass w/ findings closed by same-day hardening, 355 tests + a live pass against the real API. Introduces the app's first navigation chrome (`AppNav`, af-navTabs) in the shared layout. **US-4.1 is now fully delivered on develop.** Contract gotchas the UI enforces structurally: `status?: never` on `TaskInput` (never serialized) and all-required `SubtaskInput` (PUT-replace omissions unconstructable).

## In flight
- Nothing mid-feature. `wf next` → the `delivery` container (pick next story) or close it toward `release`.
- **Promotion develop → main (`/wf-promote`)** — eleven features stacked on develop, no compliance blockers.
- Remaining backlog: E3 (groups/nav, US-3.1), US-4.2 (dates/cards/rich text — its date derivation will observe feat-010's `SubtaskChange` seam; its web half extends screens 15/16's already-fenced-off regions), E5 (i18n, US-5.1/5.2). Scope via `wf feature add`.

## How to resume
1. Read `CLAUDE.md` (rules + working language en) and render the pipeline (`./bin/wf status`).
2. Check `catalogs/open-questions.md` (none pending as of 2026-08-12) and `ROADMAP.md`.
3. Continue the pipeline with `/wf-next`.

## Gotchas / decisions
- **Validation error handling is unified** on `api/error/ConstraintViolationMapper` (envelope `validation.failed` + per-field `violations[]`, dot-namespaced i18n keys). **Do not reintroduce a second `ExceptionMapper<ConstraintViolationException>`.**
- **Error convention (constitution 03 §Errors):** business/stateful errors = specific `domain/error/*Exception` (extend `DomainException`, mapped by `DomainExceptionMapper`); input shape = Bean Validation, custom constraints preferred; messages = dot-namespaced keys in en+pt.
- **Type evolution is guarded (OQ-17/OQ-18 decisions, 2026-08-03):** type PUT preserves field identity for unchanged fields; removing/retyping a field — or flipping its Secret flag — while values exist is rejected with a localized 409.
- **Secret fields shipped in US-2.1:** value encryption at rest + role-gated audited reveal (FR-18, BR-10); the web masks values. Rich Free-text values are sanitized on input and output (feat-007, C-08).
- **Open Questions:** none pending — OQ-14/15/16/17/18/19 all carry recorded decisions in `catalogs/open-questions.md`.
