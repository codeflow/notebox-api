# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-26

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
  - **US-4.2** task details, both halves — feat-012 (api, PR #14) + feat-013 (web, PR #13). **E4 is fully delivered on develop.**
  - **US-3.1** groups & navigation — feat-014 (api, PR #16) + feat-016 counts slice (api, PR #18). **API side complete; feat-015 (web) not started.**
- **Just landed (2026-08-26):** feat-016 — the aggregate counts for the Navigator and group listings (US-3.1, OQ-27). Squash `cbd16a9`, audit Round 1 pass w/ findings (F-01/F-02 closed same day), 389 tests, branch + PR CI green. **No migration** — every value derives on read from what feat-014 stores. The tree's counts were free: `SELECT DISTINCT` already grouped, so `GROUP BY … COUNT` returns the same rows plus the number in the same statement (`/navigation` still 4 statements; `/groups` 2 → 3, one aggregate per *page*).
  - **Contract trap for feat-015:** `averageStatus: null` means *the group has no tasks*; `0` means *has tasks, all at 0%*. Rendering null as 0% reports an empty group as a stalled one. Asserted with `assertNotEquals`, so collapsing it fails the build.
  - **Audit F-01, worth remembering:** `GET /groups/{id}` shipped `itemCount: 0` for a group holding 18 records. A `GroupDto.from(Group)` overload filled the aggregates with zeros so single reads could skip the query — but **Java records serialise every component, so a record cannot abstain from a field**; it published a wrong number instead. Fixed by deleting the overload outright.

## In flight
- **feat-015-groups-navigation-notebox-web** — the Navigator sidebar (US-3.1 web half). **Spec approved (26 scenarios); `plan` is next and is now unblocked** — feat-016 shipped the fields it consumes. Contracts: `features/014-.../contracts/groups-navigation.md` + `features/016-group-counts/contracts/group-counts.md`.
  - The design nests the tree **group → type** while the API returns **type → group**: OQ-23 as clarified 2026-08-24 — the payload is a bipartite relation and the **web inverts it**. No API change; nesting is presentation (AD-06).
- **Promotion develop → main (`/wf-promote`)** — **fifteen** features stacked on develop, no compliance blockers; promote the US-4.2 pair together, and feat-014 + feat-016 + feat-015 together once the web half lands (the PUT-replace hazard is live until then).
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
- **Backlog from feat-016's audit:** **F-03** — the statement-budget scenario (*"the number of database statements is the same as before this feature"*) has **no executable test**; it was verified structurally. Needs a Hibernate `StatementInspector` test-scope bean.
- **Open Questions:** **OQ-26 open** (tactical) — the CI `lint` step was an `echo` that could not fail; removed 2026-08-24 rather than left faking a pass, and what replaces it (Spotless / Checkstyle / nothing) is undecided. `constitution/03-code-standards.md` names Checkstyle as its enforcement point, so import order, field order and naming are reviewer-enforced only for now. All other OQs carry recorded decisions.
- **Groups (feat-014):** the table is **`item_group`** — `GROUP` is reserved in MySQL and an unquoted table breaks every statement. Namespaces are separate (annotation ≠ task) and enforced by the unique key `(tenant_id, domain, name)` plus `GroupService.resolveForAssignment`, the single seam for the rule no FK can express. Deleting a group **un-groups** its members (OQ-24) — deliberately unlike OQ-14's block rule for types, whose justification is orphaned records. The nav tree's `Ungrouped` node is `groupId: null` with a null name; the web supplies the localized label (AD-05/AD-06).
- **Board automation trap:** the Projects v2 board's built-in "Status → Done closes the issue" fires even for merges into `develop`; issues must stay open until promotion, so at review close-out the **PR** card goes to Done and the **issue** card stays non-terminal (Todo, mirroring #11/#13). #13 was auto-closed once and reopened.
