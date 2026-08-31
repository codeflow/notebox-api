# Catalog — Epics & User Stories

> Living backlog, user stories grouped by epic. IDs immutable; deprecate, never reuse. Language: English.
> Status vocabulary: `todo | speccing | building | delivered | deferred`.

**Last sync with PRD:** v2 (2026-07-24)

## Epics overview

| ID | Epic | Focus | Status |
|----|------|-------|--------|
| E0 | Foundations & Harness | repo, CI/CD, agentic pipeline | delivered (harness + CI; lint signal added 2026-08-28, OQ-26) |
| E1 | Annotation types | define typed note schemas | delivered |
| E2 | Annotations | typed records + listing/detail | delivered |
| E3 | Groups & navigation | organize items, nav tree | delivered |
| E4 | Tasks & subtasks | tasks, derived progress, cards, rich text | delivered |
| E5 | Internationalization | localized strings + translation mgmt | delivered (FR-15 shipped; FR-16 runtime catalog 2026-08-28) |
| E6 | Identity & tenancy | JWT auth, multi-tenant isolation | delivered |
| E7 | Design conformance | notebox-web matches the design handoff | delivered (feat-017/018/019; OQ-30/OQ-31 open as API gaps) |

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
| US-2.1 | As a tenant member, I want to create/edit/delete annotations of a type, so I record real data. | FR-04, FR-06, FR-18 · BR-03, BR-05, BR-10 | delivered | feat-005-annotation-records-notebox-api (api) — **merged into `develop` 2026-08-11** (PR #6, squash `0f056d7`; audit Round 4 pass, 133 tests green); promotion to `main` pending · feat-006-annotation-records-notebox-web (notebox-web) — **merged into `develop` 2026-08-12** (PR #7, squash `1f6e34a`; audit Round 2 pass, 277 tests). Both sides delivered **on develop**; promotion to `main` pending. ~~Promotion blocker~~ **closed by feat-007** (rich-text sanitization, merged to develop 2026-08-12, PR #8, squash 060a8d2). Includes secret-value encryption + audited reveal (FR-18); enforces type-delete-block when records exist (OQ-14) |
| US-2.2 | As a tenant member, I want a listing exposing visible fields plus a detail view exposing all fields, so grid and detail have what they need. | FR-05, FR-07 · BR-09 | delivered | feat-008-annotation-listing-notebox-api (api) — **merged into `develop` 2026-08-12** (PR #10, squash `c5882d4`; audit pass w/ findings, 161 tests) · feat-009-annotation-listing-notebox-web (notebox-web) — **merged into `develop` 2026-08-12** (PR #9, squash `ad79bfe`; audit Round 1 pass, 300 tests). Both sides delivered **on develop**; promotion to `main` pending. **Absorbs US-1.2's FR-05 visible-column projection** (the `visibleForViewing` flag itself shipped in feat-003) · feat-020-records-subgrid (notebox-web) — records sub-grid inside the types list (OQ-32); **audit Round 2 pass-with-findings 2026-08-31**, 624 tests green; not yet published — PR and review pending |

## E3 — Groups & navigation

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-3.1 | As a tenant member, I want to group annotations and tasks and get a navigation tree, so I can organize my workspace. | FR-08, FR-09 | delivered | feat-014-groups-navigation-notebox-api (api, issue #15) — **merged into `develop` 2026-08-24** (PR #16, squash `043859a`; audit Round 1 pass w/ findings, F-01..F-03 + N-01 all closed; spec amended to v2; 369 tests; branch + PR CI green) · feat-015-groups-navigation-notebox-web (notebox-web, issue #14) — **merged into `develop` 2026-08-26** (PR #15, squash `4ef2a0f`; audit Round 1 pass w/ findings, F-01..F-03 all closed same day; 464 tests; branch + PR CI green) · feat-016-group-counts (api, issue #17) — **merged into `develop` 2026-08-26** (PR #18, squash `cbd16a9`; audit Round 1 pass w/ findings, F-01/F-02 closed same day, F-03 backlogged; 389 tests; branch + PR CI green). **Its fields are now available to feat-015**, which was the reason the container was reordered |

## E4 — Tasks & subtasks

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-4.1 | As a tenant member, I want tasks with subtasks whose completion drives the task's progress, so status reflects real work. | FR-10, FR-11 · BR-06 | delivered | feat-010-tasks-notebox-api (api, issue #11) — **merged into `develop` 2026-08-14** (PR #12, squash `fb71054`; 212 tests) · feat-011-tasks-notebox-web (notebox-web, issue #10) — **merged into `develop` 2026-08-15** (PR #11, squash `6324257`; audit Round 1 pass w/ findings closed by hardening; 355 tests + live pass). Both sides delivered **on develop**; promotion to `main` pending |
| US-4.2 | As a tenant member, I want task dates derived from subtasks, a card link, and rich-text details, so a task is self-contained. | FR-12, FR-13, FR-14 · BR-07 | delivered | feat-012-task-details-notebox-api (api, issue #13) — **merged into `develop` 2026-08-18** (PR #14, squash `a295030`; audit Round 1 pass w/ findings, hardening closed; 274 tests) · feat-013-task-details-notebox-web (notebox-web, issue #12) — **merged into `develop` 2026-08-22** (PR #13, squash `47da16e`; audit Round 1 pass w/ findings, 6 hardening items closed; 402 tests + live pass). Both sides delivered **on develop**; promotion to `main` pending — ship the pair together (the web half closes the PUT-replace rollout hazard). E4 → delivered |

## E5 — Internationalization

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-5.1 | As a user, I want all system text in my language (en/pt), so the product is usable bilingually. | FR-15 · BR-08 | todo | — |
| US-5.2 | As a tenant administrator, I want to edit translations at runtime, so wording changes need no deploy. | FR-16 · BR-08 | todo | — |

## E6 — Identity & tenancy

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-6.1 | As a tenant member, I want my data isolated from other tenants behind JWT auth, so my organization's data stays private. | FR-17 · BR-01, BR-02 | delivered | feat-001-identity-tenancy (api) ✔ delivered on develop (PR #2, awaiting promotion); feat-002-identity-tenancy-web (notebox-web) ✔ delivered on develop (PR #2, CI green, awaiting promotion) |

## E7 — Design conformance

| ID | User Story | FR/BR | Status | Feature |
|----|------------|-------|--------|---------|
| US-7.1 | As a tenant member, I want every screen to match the agreed design, so the product looks and behaves like the thing that was designed. | FR-19 · NFR-09 | speccing | **feat-017-login-layout** (notebox-web) — login layout A, screens 02/03; **created 2026-08-26**, spec pending · _two more to create_: the shell chrome (breadcrumbs on 13 screens, menu bar, `af-tbSep`, sub-headers, footer, drawer dock) and the form widgets (`af-comboField`, `af-iconButton`, `af-spinButtons`, `af-choiceGroup`, `af-noteWindow`, `af-messages`, task-detail drawer). Scoped by `features/_design-conformance-report.md`; the screen-14 shuttle stays deferred (no bulk API surface) and screen 17 belongs to E5 |

## History

| Date | PRD version | Change |
|------|-------------|--------|
| 2026-07-19 | v1 | Initial creation. |
| 2026-07-22 | v1 | Derived E1–E6 and US-1.1…US-6.1 from PRD v1; every FR maps to a US and every US to ≥1 FR. |
| 2026-07-24 | v1 | US-1.2 deferred into US-2.2: its FR-02 "mark visible" concern was already delivered by feat-003; the residual FR-05 visible-column listing needs US-2.1 records and is US-2.2's scope. E1 → delivered. |
| 2026-07-24 | v2 | Synced to PRD v2: US-2.1 now carries FR-18 (secret-value encryption + audited reveal) and BR-10, plus the OQ-14 type-delete-block rule. |
| 2026-08-11 | v3 | US-2.1 speccing → building: feat-005 (API) passed its audit at Round 4 after three remediation rounds. The story stays `building`, not `delivered` — feat-006 (web) has not started, and US-2.1 is only delivered when both sides ship. |
| 2026-08-12 | v4 | US-2.1 building → delivered (on develop): feat-006 (web) merged via PR #7 after audit pass at Round 2. Promotion to `main` gated on scoping the API-side C-08 rich-text sanitization obligation (OQ-19). |
| 2026-08-12 | v5 | feat-007 (rich-text sanitization) merged to develop via PR #8 — the C-08 promotion blocker on US-2.1 is closed; /wf-promote has no remaining compliance obstacle. |
| 2026-08-12 | v6 | US-2.2 building → delivered (on develop): feat-009 (web) merged via PR #9 (squash `ad79bfe`) after audit Round 1 pass (300 tests), joining feat-008 (api, PR #10). E2 → delivered — US-1.2's absorbed FR-05 visible-column projection ships with it. Promotion to `main` pending. |
| 2026-08-13 | v7 | US-4.1 todo → speccing: feature pair created (feat-010 api, feat-011 web; issues #11/#10) after US-4.1 was chosen over US-3.1 — FR-09's nav tree serves tasks, so tasks ship first to keep US-3.1's contract non-speculative. E4 → speccing. |
| 2026-08-13 | v8 | US-4.1 speccing → building: feat-010 (API) passed its audit at Round 1 (PASS with findings — 2 test-strength backlog items, 0 blockers; 209 tests). The story stays `building` — feat-011 (web) has not started; delivered only when both sides ship (US-2.1 precedent). |
| 2026-08-15 | v9 | US-4.1 building → delivered (on develop): feat-011 (web) merged via PR #11 (squash `6324257`) after audit Round 1 pass w/ findings closed by same-day hardening (355 tests + live pass against the real API), joining feat-010 (api, PR #12). E4 → building (US-4.2 still todo). Promotion to `main` pending. |
| 2026-08-15 | v10 | US-4.2 todo → speccing: feature pair created (feat-012 api, feat-013 web; issues #13/#12, slug `task-details`) — chosen over US-3.1 to finish E4 while the task aggregate is hot: FR-12's date derivation exercises AD-10's `SubtaskChange` seam early, FR-14 reuses feat-007 sanitization, and a complete task model keeps US-3.1's nav-tree contract non-speculative. |
| 2026-08-17 | v11 | US-4.2 speccing → building: feat-012 (API) passed its audit at Round 1 (PASS with findings — 0 blockers; 5 same-day hardening items incl. tightening `@AbsoluteHttpUrl` to require an authority; 268 tests). The story stays `building` — feat-013 (web) has not started; delivered only when both sides ship (US-4.1 precedent). Rollout hazard recorded: PUT-replace × legacy web clears API-set cards/details until feat-013 lands. |
| 2026-08-18 | v12 | feat-012 (US-4.2 API) merged into `develop` via PR #14 (squash `a295030`; 274 tests; branch + PR CI green). US-4.2 stays `building` pending feat-013; twelve features now stacked on `develop` awaiting promotion. |
| 2026-08-21 | v13 | feat-013 (US-4.2 web) passed its audit at Round 1 (PASS with findings — 0 blockers; 6 same-day hardening items incl. empty-document normalisation for details; 402 tests + live pass against the real API: derived span, card survives PUT-replace, details edit/clear). The story stays `building` until feat-013 merges (US-4.1 precedent). |
| 2026-08-22 | v14 | US-4.2 building → delivered (on develop): feat-013 (web) merged via PR #13 (squash `47da16e`), joining feat-012 (api, PR #14). **E4 → delivered** — every task story (US-4.1, US-4.2) is on develop. Thirteen features stacked on `develop` awaiting promotion; promote the US-4.2 pair together. |
| 2026-08-22 | v15 | US-3.1 todo → speccing: feature pair created (feat-014 api, feat-015 web; slug `groups-navigation`) — chosen over E5 because its blocking precondition just cleared. FR-09's nav tree projects annotations by type **and** tasks by group, so it was deferred twice (v7, v10) until the task model was complete; with E4 delivered the contract is no longer speculative. OQ-04 (single-membership, flat, per-domain namespaces) was decided 2026-07-22, so the pair has no open question. E5 (i18n) stays last: it is cross-cutting over every string and would be redone if run before the feature surface is final. E3 → speccing. |
| 2026-08-23 | v16 | US-3.1 speccing → building: feat-014 (API) passed its audit at Round 1 (PASS with findings — 0 blockers; 3 backlog items: a spec-wording mismatch on the nesting scenario needing a human decision, one test fixture the API cannot produce, one redundant query; 368 tests). The story stays `building` — feat-015 (web) has not started, and US-3.1 is delivered only when both sides ship (US-2.1/US-4.1/US-4.2 precedent). **Rollout hazard recorded:** `groupId` rides replace-update semantics, so a legacy client that PUTs without it un-groups the item — the feat-012 PUT-replace hazard repeating; promote the feat-014/feat-015 pair together. |
| 2026-08-24 | v17 | feat-014 (US-3.1 API) merged into `develop` via PR #16 (squash `043859a`; 369 tests; branch + PR CI green). US-3.1 stays `building` pending feat-015 — the story is delivered only when both sides ship. **Fourteen features now stacked on `develop` awaiting promotion.** Promote the feat-014/feat-015 pair together: `groupId` rides replace-update semantics, so a legacy web client that PUTs without it un-groups the item. Also closed here: the CI `lint` step was an `echo` that could not fail — removed rather than left faking a pass, replacement tracked as OQ-26. |
| 2026-08-24 | v18 | feat-015 (US-3.1 web) spec approved — 26 scenarios. Two conflicts surfaced against the satellite's design reference and were decided before any scenario was written: **OQ-23 clarified** — its type→group ruling governs the *payload*; design screen 05 nests the Navigator group→type and that stands, produced by inverting client-side (no API change, AD-06 gives rendering to the satellite); **OQ-27 opened+resolved** — both design screens show aggregates feat-014 deliberately scoped out, so they are added to the API as additive fields. Because feat-014 is merged and closed, those fields became **feat-016-group-counts** (issue #17), and the delivery container was **reordered** so feat-016 precedes feat-015 — the API slice is a dependency of the web half. No ids or statuses changed in the swap. |
| 2026-08-24 | v19 | feat-016 (US-3.1 counts slice) passed its audit at Round 1 (PASS with findings — 0 blockers; 389 tests). **F-01 was a real defect caught by probing rather than reasoning:** `GET /groups/{id}` published `itemCount: 0` for a group holding 18 records, because T-03's `GroupDto.from(Group)` overload filled the aggregates with zeros — Java records serialise every component, so it could not abstain. Fixed by deleting the overload outright and computing on single reads. F-02 (unanchored cross-check) closed too; **F-03 backlogged**: the statement-budget scenario still has no executable test and needs a Hibernate `StatementInspector`. US-3.1 stays `building` — feat-015 has not started. |
| 2026-08-26 | v20 | feat-016 (US-3.1 counts slice) merged into `develop` via PR #18 (squash `cbd16a9`; 389 tests; branch + PR CI green). **feat-015's blocking dependency is cleared** — the aggregate fields its approved spec consumes now exist, so its `plan` is unblocked. US-3.1 stays `building`: the story is delivered only when the web half ships. **Fifteen features now stacked on `develop` awaiting promotion**; promote feat-014 + feat-016 + feat-015 together, since the PUT-replace hazard is still live until the web half lands. Backlog carried forward: **F-03** — the statement-budget scenario has no executable test and needs a Hibernate `StatementInspector`. |
| 2026-08-26 | v21 | **US-3.1 building → delivered (on develop)**: feat-015 (web) merged via PR #15 (squash `4ef2a0f`; 464 tests; branch + PR CI green), joining feat-014 (api, PR #16) and feat-016 (counts, PR #18). **E3 → delivered.** feat-015's audit found **F-01: an approved spec scenario that shipped unimplemented and untested** — deleting a group left a phantom node in the Navigator. Closed test-first; root cause was two plan decisions (mount-once vs. the tree reflecting the groups) that were each right and never reconciled. **Sixteen features now stacked on `develop`.** Promote **feat-014 + feat-016 + feat-015 together** — the PUT-replace hazard is only closed when all three are on the same branch. **E5 (i18n, US-5.1/US-5.2) is now the only untouched epic.** |
| 2026-08-26 | v22 | **E7 Design conformance opened** with US-7.1, from FR-19/NFR-09 (direct human decision — the source hierarchy ranks an explicit chat decision with the PRD). The handoff was always intended as the target for the whole `notebox-web` surface, but no requirement said so: features cited it only for the slice their own FR named, and the delta accumulated silently across E1–E4. A 20-screen gap report (`features/_design-conformance-report.md`) scoped it into three features; **feat-017-login-layout created first** at the human's direction. The clearest instance of the failure: feat-015's spec scoped out screen 05's chrome as *'no originating FR'* — right against the catalogs, wrong against intent, and recorded as settled instead of raised as a question. Also corrected here: E6 read `speccing` while US-6.1 has been delivered since PR #2. |
