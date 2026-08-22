# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-22

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
  - **US-4.2** task details, both halves — feat-012 (api, PR #14) + feat-013 (web, PR #13). **E4 is fully delivered on develop.**
- **Just landed (2026-08-22):** feat-013 — the task dates / card / rich-text details UI (US-4.2 web side). Squash `47da16e`, audit Round 1 pass w/ findings (6 hardening items closed on the branch), 402 tests + a live pass against the real API. Derived dates displayed never typed (`startDate?/endDate?: never`, `dd-MMM-yyyy` incl. subtask cells), inline card on task and subtask (`CardLink`/`CardFields`), a Details tab editing rich text inline with the shipped TipTap/DOMPurify stack (one dialect; `useRichImageUpload` extracted from `ValueField`). **Closes feat-012's rollout hazard:** `SubtaskInput.card` is a required key and every update body comes from full-echo builders (`taskToDraft`/`toSubtaskInput`). Client conventions: empty URL → `null`; `isEmptyRichText` (`''`, `<p></p>`, stacked/break-only paragraphs) → `details: null`.

## In flight
- Nothing mid-feature. `wf next` → the `delivery` container (pick the next story) or close it toward `release`.
- **Promotion develop → main (`/wf-promote`)** — thirteen features stacked on develop, no compliance blockers; promote the US-4.2 pair (API `a295030` + web `47da16e`) together.
- Remaining backlog: E3 (groups/nav, US-3.1), E5 (i18n, US-5.1/5.2). Scope via `wf feature add`. Recorded backlog notes: feat-012 — no automated test for the V6 date backfill (verified manually on mysql:8.4), `@Version` on the task aggregate (stored status **and** dates share the last-writer-wins race, inherited from feat-010); feat-013 — `tasksClient.ts` stale doc comment (plan-untouched file), detail tabs lack `aria-controls`, a pre-existing `LoginForm` under-load timing flake.

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
