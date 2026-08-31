# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-08-31 (feat-020 records sub-grid merged to develop)

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- **Everything delivered so far sits on `develop`, unpromoted** — `main` is still at the initial scaffold. Awaiting `/wf-promote`:
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types — feat-003 (api, PR #4) + feat-004 (web, PR #5).
  - **US-2.1** annotation records — feat-005 (api, PR #6) + feat-006 (web, PR #7) + feat-007 rich-text sanitization (api, PR #8 — closed the C-08 promotion blocker).
  - **US-2.2** listing & detail — feat-008 (api, PR #10) + feat-009 (web, PR #9) + **feat-020 records sub-grid** (web, PR #19).
  - **US-4.1** tasks, both halves — feat-010 (api, PR #12) + feat-011 (web, PR #11).
  - **US-4.2** task details, both halves — feat-012 (api, PR #14) + feat-013 (web, PR #13). **E4 is fully delivered on develop.**
  - **US-3.1** groups & navigation, all three parts — feat-014 (api, PR #16) + feat-016 counts (api, PR #18) + feat-015 (web, PR #15). **E3 delivered.**
- **Just landed (2026-08-31):** feat-020 — the records sub-grid inside the annotation-types list (OQ-32, US-2.2 web). Squash `06a1b55`, PR #19, issue #18, 624 tests, branch + PR CI green.
  - **The band reuses `RecordGridCell`, never re-renders values.** That component is the single place where a Secret field becomes a mask (C-12) and a Free-text value becomes a plain-text preview (C-08). The whole compliance argument rests there — do not let a second caller render values itself.
  - **Two structures, not one:** an `open: Set<typeId>` (what is showing) and a `bands: Map<typeId, BandState>` (what was fetched). Collapsing keeps the map entry — that *is* the cache. Conflating them makes the "reopening does not refetch" scenario fail.
  - **Audit lesson worth keeping — a test can assert the wrong mock and look thorough.** F-01: the assertion the spec named as C-12's evidence watched `imagesClient.fetchObjectUrl` (the IMAGE path) while the reveal path is `annotationRecordsClient.reveal`, unmocked in that file. It could not fail. Fixing it needed *two* parts: mock the right client **and** render as ADMIN, since the reveal affordance is role-gated and a roleless test would have been just as vacuous. **Ask of every "asserts X does not happen": can it fail?**
  - **The live browser pass found what no test could:** `border: 1px solid` (the shorthand) resets `border-color` to `currentColor`, so the failure notice drew a red edge instead of the theme's `#d98c8c`. `classCoverage` only proves a rule *mentions* a class; jsdom computes no styles.
  - **Local trap, cost two false diagnoses:** a running dev server and `npm run verify` share `.next`; `npm run clean` deletes it under the dev server. Kill the dev server before verifying, or the build fails with `Cannot find module for page: /_not-found` and the dev server 500s.
- **Previously (2026-08-26):** feat-015 — the Navigator, the Groups screen and group assignment (US-3.1 web half). Squash `4ef2a0f`, audit Round 1 pass w/ findings (F-01..F-03 closed same day), 464 tests, branch + PR CI green. **US-3.1 is delivered on both sides; E3 is done.**
  - **`groupId` is required-and-nullable** on both item input types, never optional. That compile error *is* what closes feat-014's PUT-replace hazard — an omitted key clears the group, so optional would let a caller forget and silently un-group an item.
  - **The tree inverts client-side.** The API returns `type → groups`; design 05 nests `group → type`. OQ-23 as clarified: the payload is a bipartite relation, nesting is presentation (AD-06). `lib/navigation/viewModel.ts` is a pure function — the whole inversion is tested with no rendering.
  - **The Navigator refreshes on group mutation** via `lib/navigation/treeRevision.ts`, a revision counter read through `useSyncExternalStore`. It is a *signal*, not shared state: no tree data lives in it, so the API stays the single source of truth. `router.refresh()` does not work here — the tree fetches in a client `useEffect`.
  - **Audit lesson worth keeping:** F-01 was an approved scenario that shipped unimplemented *and untested*. Two plan decisions — mount-once for performance, and the tree reflecting the groups — were each right and never reconciled. **A scenario without a test is not covered by the tasks that cite it.**

## In flight
- **feat-021 — Navigator root nodes navigate to their list screens (OQ-33).** Next up, at `spec`. Raised from live use of feat-020: the tree's `Annotations` root is an inert `<span>`, and its type leaves jump straight to a type's records — so the types list, where the new sub-grid lives, has no route from the tree. Decided option (a): root nodes navigate (`Annotations` → `/annotation-types`, `Tasks` → `/tasks`), a **recorded deviation from design 05**, which draws them inert.
- **OQ-34 open** — two small gaps feat-020 left, decision pending: the band link's route string is asserted only by hand, and two toggles in one tick fire two identical records requests (proved; **not** reachable by a real double-click). Candidates to fold into feat-021, which already touches navigation.
- **E5 (i18n, US-5.1/US-5.2)** remains the only untouched epic.
- **Promotion develop → main (`/wf-promote`)** — **sixteen** features stacked on develop, no compliance blockers. Promote the US-4.2 pair together, and **feat-014 + feat-016 + feat-015 together**: the PUT-replace hazard is only closed when all three are on the same branch.
- **Satellite housekeeping:** `notebox-web` still carries dead branches — `feature/identity-tenancy-web` (local + remote), `feature/annotation-types-web` and `fix/login-header-band` (remote). Verify content before deleting, as the hub's cleanup did.
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
- **Backlog from feat-015's audit:** none — F-01/F-02/F-03 all closed on the branch.
- **Backlog from feat-016's audit:** **F-03** — the statement-budget scenario (*"the number of database statements is the same as before this feature"*) has **no executable test**; it was verified structurally. Needs a Hibernate `StatementInspector` test-scope bean.
- **Open Questions:** **OQ-26 open** (tactical) — the CI `lint` step was an `echo` that could not fail; removed 2026-08-24 rather than left faking a pass, and what replaces it (Spotless / Checkstyle / nothing) is undecided. `constitution/03-code-standards.md` names Checkstyle as its enforcement point, so import order, field order and naming are reviewer-enforced only for now. All other OQs carry recorded decisions.
- **Groups (feat-014):** the table is **`item_group`** — `GROUP` is reserved in MySQL and an unquoted table breaks every statement. Namespaces are separate (annotation ≠ task) and enforced by the unique key `(tenant_id, domain, name)` plus `GroupService.resolveForAssignment`, the single seam for the rule no FK can express. Deleting a group **un-groups** its members (OQ-24) — deliberately unlike OQ-14's block rule for types, whose justification is orphaned records. The nav tree's `Ungrouped` node is `groupId: null` with a null name; the web supplies the localized label (AD-05/AD-06).
- **Board automation trap:** the Projects v2 board's built-in "Status → Done closes the issue" fires even for merges into `develop`; issues must stay open until promotion, so at review close-out the **PR** card goes to Done and the **issue** card stays non-terminal (Todo, mirroring #11/#13). #13 was auto-closed once and reopened.
