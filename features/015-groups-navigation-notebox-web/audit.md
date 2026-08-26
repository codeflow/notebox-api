# Audit — Item groups and navigation-tree data (web)

**ID:** features/015-groups-navigation-notebox-web · **US:** US-3.1 · **Round:** 1
**Date:** 2026-08-26 · **Model:** opus/xhigh · **Project:** `notebox-web` (routed satellite)
**Scope:** `develop..feature/groups-navigation-web` — 6 commits, 46 files
**Verify at audit time:** `npm run verify` → exit 0, **458 tests, 63 files, 0 unhandled errors**
**Hardening closed:** 2026-08-26 — all three findings resolved, **464 tests**, 65 files, green

## Verdict — **PASS WITH FINDINGS** (all three closed 2026-08-26)

Three findings. **F-01 is an approved spec scenario that is neither implemented nor tested** — not a
polish item, and it should be closed before this merges. It is not a `fail` because 25 of the 26
scenarios hold, the gap is one clause with a bounded fix, and no substep needs reopening: the plan
is sound, it simply never reconciled two of its own decisions.

---

## 1. Traceability — FR → scenario → test → code

| Block | Scenarios | Tests | Status |
|---|---|---|---|
| FR-09 Navigator renders group → type | 6 | `viewModel.test.ts` 9, `Navigator.test.tsx` 7 | ✔ |
| FR-09 Clicking a node | 4 | `groupFilter.test.tsx` 8 | ✔ |
| FR-08 Managing groups | 7 | `GroupsPanel.test.tsx` 12 | **6 of 7** — see F-01 |
| FR-08 Assigning an item | 4 | `GroupAssignment.test.tsx` 4, `viewModel` builders 6 | ✔ |
| OQ-27 Aggregates as received | 2 | `format.test.ts` 4, panel + tree | ✔ |
| Standing guarantees | 3 | see below | ✔ |

**25 of 26 scenarios have a test that exercises them.** The twenty-sixth is F-01.

The **session-guard** scenario resolves structurally rather than by a new test: `RouteGuard` wraps
every route in `app/providers.tsx`, so `/groups` inherits it by living under `(app)`. That is
stronger than a per-route assertion — a new route cannot forget to opt in — and `RouteGuard.test.tsx`
covers the mechanism. Recorded here so the absence of a `/groups`-specific guard test is read as
deliberate.

## 2. Scenario honesty — would these fail on a regression?

**Every load-bearing claim in this feature was mutation-tested during implementation**, each
reverted after confirming the right tests failed:

| Mutation | Failures |
|---|---|
| inversion: accumulate → overwrite (`Map<groupId, type>`) | 3 |
| inversion: added a client-side `sort()` | 3 |
| `formatAverageStatus`: `?? 0` collapse | 2 |
| `GroupsPanel` cell: `(averageStatus ?? 0) + '%'` | 2 |
| grid: dropped `group` from the client call | 2 |
| `toTaskInput`: stopped emitting `groupId` | 3 |

Spot-checks of the remaining tests:

- **`groupFilter.test.tsx` — "a pasted URL reproduces the filtered view"** renders the route fresh
  from its URL and *additionally asserts `push` was never called*. That second assertion is what
  makes it a shareability test rather than a navigation test.
- **`GroupsPanel` — "offers NO domain control"** asserts an **absence** three ways (no labelled
  control, no combobox, no `ANNOTATION`/`TASK` literal). A control that merely happens to be unused
  today would pass a weaker check.
- **`GroupsPanel` — the delete dialog** asserts the *wording*, not just that a dialog opened. The
  text is the deliverable under OQ-24, and it also asserts nothing is sent before confirmation.
- **`Navigator` — "fetches exactly once"** counts MSW requests. A per-page mount fails it; inspecting
  the component would not.

**Adversarial probes run during this audit, not before it:**

1. **Are the `pt` values real translations or copies?** The satellite's parity tests compare **keys
   only** (`Object.keys(en)` vs `Object.keys(pt)`) — a copy-pasted catalog passes them. Probed all
   **35** feat-015 keys directly: **zero** have an identical en/pt value. Clean — see N-01 for the
   gap this exposed in the shared check.
2. **Is there a second source of truth for selection?** `Navigator` holds only `collapsed`; the
   selected node is computed from `pathname` + `?group=` on every render. No selection state exists
   to desynchronise (W-7 holds).
3. **Does the tree survive its own screen's mutations?** It does not — **F-01**.

## 3. Constitution / satellite rules

| Rule | Result |
|---|---|
| `adf-fusion.css` never edited | **CLEAN** — 0 changes; all styling is `nb-*` in the overrides sheet |
| All calls through the shipped `authFetch` stack | **CLEAN** — both new clients use it; no bare `fetch` |
| No new styling system (AD-06, satellite README) | **CLEAN** — `af-*` classes as the design uses them |
| i18n through `useTranslation`, no inline strings | **CLEAN** — 35 keys, both catalogs, line-parallel |

## 4. Compliance pre-flight — evidence where claimed

| Item | Claimed | Found |
|---|---|---|
| **C-01** tenant isolation | client obligation, shipped stack | ✔ every new request goes through `authFetch`; no client code carries a tenant |
| **C-02** authenticated by default | guarded routes | ✔ structural — `RouteGuard` in `providers.tsx` wraps `(app)` |
| **C-06** encryption in transit | standing | ✔ same origin/TLS posture, no new transport |
| **C-09** localization completeness | en + pt for every new string | ✔ 35 keys, verified genuinely translated (probe 1); pt asserted in `Navigator` and `GroupsPanel` tests |

The 8 **n/a** items were re-checked against the diff and remain n/a — no image binary, no rich text,
no admin surface, no identity data, no credential, no destructive act performed client-side.

## 5. Scope — diff vs the plan's blast radius

The plan named 11 new + 9 changed. The diff carries **two production files the plan did not name**,
both disclosed in their tick and commit at the time:

| File | Verdict |
|---|---|
| `components/groups/GroupSelect.tsx` | **refinement, not creep.** The plan put the selector "in the forms"; extracting it keeps the domain-scoping in **one** place instead of two copies that could drift apart. |
| `test/msw/handlers.ts` (baseline `/groups`) | **necessary, and it fixed a real defect** — see below. |

**7 existing test files** were touched to widen exact-body assertions and extend `next/navigation`
mocks — mechanical, predicted by plan Risk 1, and **no assertion was weakened**: the 12 exact
`toEqual` bodies were widened to include `groupId`, never loosened to `toMatchObject`. That
distinction matters, because those are precisely the tests that would catch the hazard returning.

**A real defect the verify signal caught and the tests did not:** after T-06, every suite rendering
either form requests `/groups` on mount, and with no default handler **23 requests escaped MSW to
real `localhost:3000`** while all tests still passed. Fixed in the shared fixture layer. Green tests
plus silent unhandled errors is the combination that hides the *next* failure.

## 6. Open Questions

**OQ-04, OQ-23 (as clarified 2026-08-24), OQ-24, OQ-25, OQ-27** all carry recorded human decisions
predating this feature's spec. Nothing was closed by implementer assumption. The design conflict
that produced the OQ-23 clarification and OQ-27 was surfaced **before** any scenario was written.

---

## Findings

### F-01 · The Navigator never refreshes, so a deleted group lingers in the tree
**Severity:** medium-high — **an approved spec scenario that is neither implemented nor tested.**
**Close before merge.**
**Where:** `components/navigation/Navigator.tsx` (fetch effect) × `components/groups/GroupsPanel.tsx`
**The scenario:** *"Confirming the delete removes only the group / Then the group disappears from the
panel / **And the Navigator no longer shows that group node**"* — the second clause.
**Concrete failure:** open `/groups`, delete "Infrastructure". The panel reloads and drops the row.
The Navigator still shows the node, because it fetched once at layout mount and the App Router keeps
that layout alive across route changes. Clicking the phantom node opens a listing filtered to a group
that no longer exists — which the API answers with an empty page, so the member sees an empty grid
with no explanation. Creating and renaming are stale the same way.
**Proved by construction, not inference:** `setPayload` has exactly **one** call site, inside a
`useEffect` with `[]` dependencies, and no refresh prop, event, key or `router.refresh()` exists in
`Navigator`, `GroupsPanel` or the layout. There is no mechanism.
**Why it happened:** the plan made two decisions that were each right and never reconciled — "mount
once in the layout so the tree is fetched once per view" (performance) and "the Navigator reflects
the groups" (correctness). Neither the plan's Risk section nor the task breakdown noticed they
collide the moment a group is mutated.
**Fix:** a refresh signal from the mutating screen to the tree — the smallest honest version is
`router.refresh()` after a successful create/rename/delete, or a shared counter the Navigator's
effect depends on. Either keeps the once-per-view fetch for navigation while making mutation
invalidate it. **Plus the missing test**, which is the actual gap: the scenario had no test either.

### F-02 · The Navigator's failure state is silent about what failed
**Severity:** low
**Where:** `components/navigation/Navigator.tsx`
**Concrete failure:** any rejection — 500, network, an aborted request — renders the same
`navigator.error` text. A member cannot tell "the server is down" from "your session expired", and a
401 arriving here is indistinguishable from a genuine outage even though the guard would normally
handle it.
**Why it is only low:** the spec asks that the failure "degrades without breaking the workspace", and
it does — the rest of the page renders and the error is localized. This is about the quality of the
message, not the behaviour.
**Fix:** distinguish an `ApiError` 401 from other failures, or leave as-is with the limitation
recorded. Backlog.

### F-03 · The catalog parity tests compare keys only, so a copy-pasted translation passes
**Severity:** low — **pre-existing, not introduced here**
**Where:** `lib/i18n/i18n.test.ts`, `lib/i18n/keysetCoverage.test.ts`
**Concrete failure:** both assert `Object.keys(en)` equals `Object.keys(pt)`. A `pt.ts` whose values
were copied verbatim from `en.ts` passes both, and ships English to a Portuguese member with a green
build. The API side has exactly this check (`GroupMessageCoverageTest.portugueseIsActuallyTranslated`,
feat-014); the web side does not.
**Verified not currently violated:** probed all 35 feat-015 keys — none has an identical en/pt value.
So this is a missing guard, not a live defect.
**Fix:** one assertion that no value is identical across catalogs, with an explicit allow-list for
the handful that legitimately match (proper nouns, symbols). Backlog — it touches every existing key,
so it is its own small piece of work.

---

## Hardening — all three closed 2026-08-26

Nothing carried to backlog. Verify green at **464 tests** (458 → 464), 65 files.

| Item | Resolution |
|---|---|
| **F-01** | **Closed, test-first.** The failing test was written and confirmed **red** against the old code before any fix, so it demonstrably covers the gap rather than merely coexisting with it. The mechanism is a revision counter (`lib/navigation/treeRevision.ts`) read through `useSyncExternalStore`. `router.refresh()` was rejected — the Navigator fetches in a client `useEffect`, which a server-component refresh does not re-run. It is deliberately a **signal, not shared state**: no tree data lives in it, so the API stays the single source of truth and no second copy can disagree. Once-per-view survives for navigation; only a mutation invalidates. `invalidateNavigationTree()` fires **after** the mutation resolves — optimistic invalidation would show a change that did not happen when a delete fails. |
| **F-02** | **Closed.** A 401 now reads as the session ending rather than a generic load failure, with its own key in both catalogs and a test asserting the two messages are distinct. |
| **F-03** | **Closed.** The guard the web side lacked while the API has had it since feat-014. The allow-list is **explicit, not heuristic** — "short strings are probably fine" would let a real miss through — and all 10 entries were inspected by hand: a product name, a mask, a URL sample, and cognates genuinely identical in Portuguese. Two extra tests keep the list honest: an entry that stops being identical must be removed, and it may only name keys that exist. |

### What F-01 says about the plan, not just the code

The gap was not a slip during implementation. Two plan decisions — *"mount once so the tree is
fetched once per view"* and *"the Navigator reflects the groups"* — were each correct and never
reconciled, and the task breakdown inherited the blind spot. The scenario had no test, so nothing
downstream could catch it. **A scenario without a test is not covered by the tasks that cite it**;
that is the reusable lesson here, and it is why the audit's traceability section counts tests rather
than intentions.

## Not found

No weakened test — 12 exact assertions widened, **zero** loosened. No `adf-fusion.css` edit. No bare
`fetch`. No inline user-facing string. No second source of truth for selection. No OQ closed by
assumption. No scope creep — the two unplanned files are a refinement and a defect fix, both
disclosed when they landed.
