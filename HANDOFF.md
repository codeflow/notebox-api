# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-24

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
  - **US-4.2** task details, both halves — feat-012 (api, PR #14) + feat-013 (web, PR #13). **E4 is fully delivered on develop.**
  - **US-3.1** groups & navigation — feat-014 (api, PR #16). **API half only; feat-015 (web) not started.**
- **Just landed (2026-08-24):** feat-014 — item groups and navigation-tree data (US-3.1 API side). Squash `043859a`, audit Round 1 pass w/ findings (F-01..F-03 + N-01 all closed on the branch), 369 tests, branch + PR CI green. One `Group` entity discriminated by `domain` — table **`item_group`**, since `GROUP` is reserved in MySQL. Membership is a nullable FK on the *item* (`annotation_record.group_id`, `task.group_id`) with **`ON DELETE SET NULL`**, which *is* the OQ-24 rule: deleting a group destroys the label only. `GET /navigation` assembles both roots from four bounded queries and **stops at group nodes** — no record or task is ever enumerated (OQ-23). Both listings gained one optional `group` param (`<uuid>` | `none`).
  - ⚠️ **Rollout hazard, live:** `groupId` rides replace-update semantics, so a PUT omitting it **clears the group**. The deployed web forms do exactly that until feat-015 ships — the feat-012 PUT-replace hazard repeating. **Promote feat-014 + feat-015 together.**

## In flight
- **feat-015-groups-navigation-notebox-web** — the Navigator sidebar (US-3.1 web half). Not started; `wf next` points at its `spec`. Its contract is `features/014-groups-navigation-notebox-api/contracts/groups-navigation.md`.
- **Promotion develop → main (`/wf-promote`)** — **fourteen** features stacked on develop, no compliance blockers; promote the US-4.2 pair (API `a295030` + web `47da16e`) and, once feat-015 lands, the US-3.1 pair together.
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
- **Open Questions:** **OQ-26 open** (tactical) — the CI `lint` step was an `echo` that could not fail; removed 2026-08-24 rather than left faking a pass, and what replaces it (Spotless / Checkstyle / nothing) is undecided. `constitution/03-code-standards.md` names Checkstyle as its enforcement point, so import order, field order and naming are reviewer-enforced only for now. All other OQs carry recorded decisions.
- **Groups (feat-014):** the table is **`item_group`** — `GROUP` is reserved in MySQL and an unquoted table breaks every statement. Namespaces are separate (annotation ≠ task) and enforced by the unique key `(tenant_id, domain, name)` plus `GroupService.resolveForAssignment`, the single seam for the rule no FK can express. Deleting a group **un-groups** its members (OQ-24) — deliberately unlike OQ-14's block rule for types, whose justification is orphaned records. The nav tree's `Ungrouped` node is `groupId: null` with a null name; the web supplies the localized label (AD-05/AD-06).
- **Board automation trap:** the Projects v2 board's built-in "Status → Done closes the issue" fires even for merges into `develop`; issues must stay open until promotion, so at review close-out the **PR** card goes to Done and the **issue** card stays non-terminal (Todo, mirroring #11/#13). #13 was auto-closed once and reopened.
