# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-18

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
  - **US-4.2** task details, API half — feat-012 (api, PR #14).
- **Just landed (2026-08-18):** feat-012 — task dates, card link and rich-text details (US-4.2 API side). Squash `a295030`, audit Round 1 pass w/ findings (all 5 hardening items closed on the branch), 274 tests. Second observer of the AD-10 `SubtaskChange` seam (`TaskDatesRecalculator` + new `SubtaskRescheduled` fact), the codebase's first `@Embeddable` (`Card`, on task and subtask), and feat-007's sanitizer reused for `details` on write **and** read. Contract for the web side: `features/012-task-details-notebox-api/contracts/task-details.md` — additive fields only; `TaskInput` poisons `startDate`/`endDate` like `status`; PUT-replace clears omitted `card`/`details`; card `url` must be absolute http/https **with an authority** and `""` is invalid (send `null`); `""` details is stored as `""` (only `null` clears).

## In flight
- **US-4.2 web half:** `wf next` → `feat-013-task-details-notebox-web.spec` (opus·high) — consumes feat-012's contract. **Rollout hazard to carry into its spec:** the shipped feat-011 UI sends inputs without `card`/`details`, so until feat-013 lands an old-UI task edit (or a subtask checkbox tick) clears API-set cards/details — spec-conformant replace semantics; the pair should reach `main` together and feat-013's input builders must echo `card`/`details`.
- **Promotion develop → main (`/wf-promote`)** — twelve features stacked on develop, no compliance blockers.
- Remaining backlog: E3 (groups/nav, US-3.1), E5 (i18n, US-5.1/5.2). Scope via `wf feature add`. Feat-012 backlog notes: no automated test for the V6 date backfill (verified manually on mysql:8.4); `@Version` on the task aggregate (stored status **and** dates share the last-writer-wins race, inherited from feat-010).

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
- **Board automation trap:** the Projects v2 board's built-in "Status → Done closes the issue" fires even for merges into `develop`; issues must stay open until promotion, so at review close-out the **PR** card goes to Done and the **issue** card stays non-terminal (Todo, mirroring #11/#13). #13 was auto-closed once and reopened.
