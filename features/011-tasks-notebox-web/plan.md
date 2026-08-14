# Plan — Tasks & subtasks UI with derived progress (notebox-web)

**ID:** features/011-tasks-notebox-web
**User Story:** US-4.1 · **Project:** notebox-web (satellite; react/next)
**Version:** v1
**Status:** Approved (human approval 2026-08-14)
**Date:** 2026-08-14

## Origin
- **Spec:** `features/011-tasks-notebox-web/spec.md` (approved 2026-08-14; 18 scenarios).
- **Wire truth:** `features/010-tasks-notebox-api/contracts/tasks.md` — consumed, never altered.
- **Design truth:** `design/handoff` screens 15 (`#s15`) and 16 (`#s16`), ported **US-4.1 subset only**.
- **Companion artifacts:** `data-model.md` (TS types/state), `contracts/interfaces.md` (client + component APIs + i18n keys).

## Approach (three sentences)

Four new guarded routes under `app/(app)/tasks/` (list, detail, `new/`, `[taskId]/edit/` — the
feat-006 route-per-mode precedent), one API client cloned from the `annotationRecordsClient` idiom,
a pure view-model module owning the two contract-critical conversions, and six components under
`components/tasks/`, all on the shipped `useEffect`+`LoadState` pattern. The two wire disciplines
are enforced **structurally in the types**: `TaskInput` declares `status?: never` (a spread of a DTO
into a body is a compile error) and `SubtaskInput` declares all four fields required (an omitted
date is unconstructable), with `toSubtaskInput(source, overrides)` as the only body builder for
checkbox ticks — every subtask mutation repaints task state wholesale from the returned parent
`TaskDto`, no client percent math, no refetch. Navigation chrome is net-new: a `components/AppNav.tsx`
(`af-navTabs`, three tabs — Overview / Annotations / Tasks, selected by `usePathname` prefix) inserted
as one line in the shared `(app)` layout — the only shared-chrome touch, guarded by two new tests
because **no existing test renders that layout**.

## Design (seams)

1. **Wire discipline in types + viewModel, not components** — `lib/api/types.ts` block +
   `lib/tasks/viewModel.ts` (`toTaskInput` maps `'' → null` priority so the *server* answers
   `task.priority.required`; `toSubtaskInput` echoes name/dates/done in full, then applies
   overrides; `draftToSubtaskInput(draft, done)` takes `done` explicitly so no call site can forget
   it). Serialization tests pin both bodies.
2. **Repaint from the parent DTO** — `SubtasksPanel` owns no fetch: it receives `task: TaskDto`,
   calls the client (whose subtask signatures all resolve to the **parent** `TaskDto`; subtask
   DELETE is `200 + TaskDto`, unlike task delete's `204`) and lifts the response via
   `onTaskUpdated`; the detail page replaces its `ready` state wholesale.
3. **Server is the validator** — forms are `noValidate` (RecordForm posture); the lightweight
   per-feature routers (`routeTaskProblem`, `routeSubtaskProblem`) place the server's verbatim
   localized message. The routing is exact because feat-010's error matrix is closed: for tasks,
   path tails `.name`/`.priority` cover all field violations; for subtasks, `.name` → name and
   **any other violation is the date-range error** (there is no third constraint), residue → form.
4. **Nav** — `AppNav.tsx` (`'use client'`, `usePathname`+`useRouter`), `nav.af-navTabs` with
   `button.af-navTab` (+`.selected`, `aria-current="page"`); theme CSS already exists; buttons get
   a tiny `nb-navTab` reset. The layout diff is exactly one line.
5. **Screens ported (US-4.1 subset)** — list: `af-panelCollection` toolbar (New task) + `af-table`
   `rowHdr | Task (link) | Priority | Status` (`af-progress` + `%` label) + `af-statusBar` pager.
   Detail: `nb-detailHeader` (Edit/Delete) + `af-panelBox highlight` "Derived metrics" (Status +
   Priority only) + subtasks table (Done 60px | Subtask | Start date | End date | Actions) +
   count line. **Dropped from the mockups:** Group column/tree, Subtasks count (no wire backing in
   `TaskListItemDto`), Start/End min-max, Card, filter row, Details/History tabs, Notes drawer —
   US-4.2/E3. **"Mark all done"/"Reset" excluded by recommendation:** in none of the 18 scenarios
   and no wire support (each would be N sequential PUTs, breaking the one-round-trip repaint); a
   future declared scope needing a bulk endpoint.
6. **Dates** — native `input[type=date]` (its value IS the wire's `yyyy-MM-dd`; `''` maps to
   explicit `null`); display renders the wire string verbatim, null renders the glyph `—`.

## Alternatives rejected

1. **Nav inline in `layout.tsx`** — `usePathname` would force `'use client'` onto the whole layout
   and the strip would be untestable in isolation; also rejected: an Administration tab (dead
   chrome — no route) and a two-tab set (strands the home route).
2. **Dialog / inline-select task editing** — screen 16's inline priority select is US-4.2 chrome;
   the spec fixes create/edit as a validated form; dialogs in this repo are confirm-only; routes
   give edit its natural not-found idiom (feat-006's proven shape).
3. **Generalizing `DeleteRecordDialog` into a shared ConfirmDialog** — task delete needs its own
   subtasks-go-too copy (a spec obligation) and refactoring shipped components widens blast radius
   into shipped tests; per-feature dialogs are the precedent.
4. **Subtask add/edit as dialog or route** — subtasks are row-scale in a table whose repaint source
   is the mutation response; leaving the panel breaks the in-place UX. Inline editor row matches
   the child-collection precedent.
5. **`TaskInput` merely omitting `status`** — excess-property checks don't catch spreads;
   `status?: never` does, at compile time; the runtime serialization test remains as second lock.
6. **Locale-formatted dates** — locale-dependent test surface for zero spec value; ISO verbatim is
   deterministic and round-trips the inputs; revisit with US-4.2.
7. **The heavyweight feat-004 `violationRouting.ts`** — the closed error matrices make the
   RecordForm-style router exact in ~20 lines.
8. **Client-side "M done" arithmetic in the status bar** — keeps the panel free of derived-progress
   math; the count line shows only `subtasks.length`, a display fact.

## Reversibility

Everything is additive; deleting the new files, keys, CSS block and the one layout line restores
today's tree exactly. Shared-file touches: `app/(app)/layout.tsx` (one line), `lib/api/types.ts`
(append-only), the two catalogs (append-only, keyset-enforced together), `test/msw/handlers.ts`
(append-only), `adf-fusion.overrides.css` (append-only). `adf-fusion.css` untouched (read-only
rule). No new dependencies, no config changes; the API contract is consumed, not altered. The
**one-way** pieces are UX-contract-level: the route names (`/tasks`, `/tasks/[taskId]`), the nav's
existence, and the client's structural type guards that feat-011 tests will pin.

## Blast radius

- **Created (25 files):** 4 route pages + 1 route integration test under `app/(app)/tasks/`;
  `components/AppNav.tsx` + test; `app/(app)/layout.test.tsx` (new regression smoke);
  `components/tasks/{TasksTable,TaskForm,SubtasksPanel,PriorityLabel,TaskProgress,DeleteTaskDialog,DeleteSubtaskDialog}.tsx`
  + 5 test files; `lib/api/tasksClient.ts` + test; `lib/tasks/viewModel.ts` + test.
- **Modified (6):** `app/(app)/layout.tsx` (insert `<AppNav />` — the only shared-chrome touch);
  `lib/api/types.ts`; `lib/i18n/messages/en.ts` + `pt.ts` (**52 keys** each, same commit —
  `keysetCoverage.test.ts` fails otherwise); `src/styles/adf-fusion.overrides.css` (`nb-navTab`,
  `nb-progressNarrow` 130px, subtask editor rows, `nb-taskDetail` panel-width pin — the
  `.af-panelBox` `flex: 1 1 0%` trap already pinned once for login); `test/msw/handlers.ts`
  (task fixtures + 10 parent-returning-semantics factories).
- **Existing-test exposure:** verified — no existing test renders `app/(app)/layout.tsx` (route
  suites import page defaults directly), so the nav insertion is invisible to today's suite; the
  new `AppNav.test.tsx` + `layout.test.tsx` close that gap. Only `keysetCoverage` can fail on
  catalog drift. No shipped component or test is modified.

## Risk

| # | Risk | Guard / signal |
|---|---|---|
| 1 | Nav regression — one shared-layout line re-chromes every authed screen, zero existing coverage | `AppNav.test.tsx` + `layout.test.tsx` smoke; `npm run verify` build; manual pass over `/`, `/annotation-types/*` |
| 2 | PUT-replace omission wipes dates on a tick | Required-field `SubtaskInput` (omission unconstructable) + `toSubtaskInput` as sole body builder + MSW test deep-equalling the captured PUT body |
| 3 | `status` leaks into a body via DTO spread | `status?: never` (compile-time) + serialization tests asserting no `status` key on create and update |
| 4 | Stale list after detail mutations on back-nav | None needed by construction — no cache layer; list refetches on mount; integration test asserts the fresh GET |
| 5 | MSW drift vs real API (e.g. 204 habit for subtask DELETE where the contract says 200 + parent) | Handlers written against the contract table; client test asserts `removeSubtask` resolves to a `TaskDto` |
| 6 | Class-level `@DateRangeValid` violation path (bean-level, no field tail) misroutes | Closed-matrix router: non-`.name` → dates by exhaustion, code-independent; MSW fixture mirrors the raw Jakarta path; verify against the real mapper at implement |
| 7 | Theme discipline — mockup inline styles must become `nb-*`; `adf-fusion.css` read-only | All new CSS in overrides; only sanctioned inline style is the data-driven `bar` width%; diff must never touch `adf-fusion.css` |
| 8 | `.af-panelBox` `flex:1 1 0%` distorts the metrics box | `nb-taskDetail` pin (login precedent); visual check |
| 9 | pt gaps breach C-09 | `MessageKey` compile error + `keysetCoverage` + the pt end-to-end case |
| 10 | jsdom `type=date` typing unreliable under `user-event` | Date inputs driven with `fireEvent.change` (pinned in the test plan) |

## Test plan — 18 scenarios → 10 test files

`viewModel.test.ts` (no-status-key + full-echo units) · `tasksClient.test.ts` (verbs/URLs/bodies,
parent-returning subtask DELETE, 404 → `ApiError`) · `TasksTable.test.tsx` (S1 order-as-served, S2
pager facts + `page=1` capture, S3 empty, S12 list-half) · `TaskForm.test.tsx` (S4–S7: unchosen
priority, field routing, POST/PUT bodies, pt case) · `SubtasksPanel.test.tsx` (S10 full-object tick
+ repaint, S11, S13, S14 date-range surfacing, S15) · the two dialog tests (S8/S15 halves) ·
`AppNav.test.tsx` + `layout.test.tsx` (chrome + regression guard) · `tasks.integration.test.tsx`
(S9 unswapped dates, S12, S17 not-found, S18 pt end-to-end, S8 flow, create/edit route wiring,
list-mount refetch). **S16 (guard):** no new test — the global `RouteGuard` covers all `(app)`
routes; `RouteGuard.test.tsx` owns the redirect assertion (reused evidence, noted per precedent).
Gate: `npm run verify` (lint + prettier + tsc + vitest + build) — tsc itself enforces the two
structural guards.
