# Tasks — Tasks & subtasks UI with derived progress (notebox-web)

**ID:** features/011-tasks-notebox-web · **US:** US-4.1 · **Date:** 2026-08-14
**Source:** plan.md + contracts/interfaces.md + data-model.md (approved 2026-08-14)
**Status:** Approved (human approval 2026-08-14)

> Strictly sequential. T-02/T-03/T-04 all append to the shared i18n catalogs (en **and** pt in the
> same task — `keysetCoverage.test.ts` fails otherwise) and to `adf-fusion.overrides.css`, so no
> task pair is worktree-safe. Every task ships its tests; the satellite's `npm run verify`
> (lint + prettier + tsc + vitest + build) must be green at the end of each task. All file paths
> below are satellite-relative (`../notebox-web`).

- [x] **T-01 · Wire foundation — types with structural guards, tasksClient, viewModel, MSW factories** ✔ 2026-08-14, verify green (314 tests; pre-existing TypeBuilderForm flake flagged as separate backlog task)
      - files: `lib/api/types.ts` (append feat-011 block: `TaskPriority`, `TaskInput` with `status?: never`, all-required `SubtaskInput`, 3 DTos), `lib/api/tasksClient.ts` + `lib/api/tasksClient.test.ts`, `lib/tasks/viewModel.ts` + `lib/tasks/viewModel.test.ts`, `test/msw/handlers.ts` (10 task factories, parent-returning semantics)
      - covers: BR-06 client guard + PUT-replace safety · scenarios: serialization halves of "Create a task with the minimum shape" / "Edit changes name and priority only" (no status key), "Ticking a checkbox sends the full subtask" (full-echo unit), parent-`TaskDto` resolution on subtask DELETE
      - depends: — · parallel: no
      - verify: `npm run verify`

- [x] **T-02 · Navigation chrome — AppNav, the one-line layout insert, regression guards** ✔ 2026-08-14, verify green (321 tests)
      - files: `components/AppNav.tsx` + `components/AppNav.test.tsx`, `app/(app)/layout.tsx` (insert `<AppNav />`), `app/(app)/layout.test.tsx` (new smoke), `src/styles/adf-fusion.overrides.css` (`nb-navTab` reset), `lib/i18n/messages/en.ts` + `pt.ts` (the 3 `nav.*` keys, both catalogs)
      - covers: the guarded-route reachability chrome · scenarios: route-and-nav half of "The Tasks route is guarded" (guard itself is reused `RouteGuard` evidence); the layout smoke guards every existing route's chrome (plan risk 1 — zero prior layout coverage)
      - depends: — · parallel: no (shared catalogs/CSS with T-03/T-04)
      - verify: `npm run verify`

- [x] **T-03 · List and task lifecycle — TasksTable, TaskForm, PriorityLabel, TaskProgress, routes** ✔ 2026-08-14, verify green (333 tests; `tasks.notFound` key pulled forward for the edit route, noted)
      - files: `components/tasks/TasksTable.tsx` + `.test.tsx`, `components/tasks/TaskForm.tsx` + `.test.tsx` (exports `routeTaskProblem`), `components/tasks/PriorityLabel.tsx`, `components/tasks/TaskProgress.tsx`, `app/(app)/tasks/page.tsx`, `app/(app)/tasks/new/page.tsx`, `app/(app)/tasks/[taskId]/edit/page.tsx`, `adf-fusion.overrides.css` (`nb-progressNarrow`), `en.ts`/`pt.ts` (list + priorities + form keys, both catalogs)
      - covers: FR-10 UI · scenarios: "The list renders the served page, newest first", "The pager reflects PageDto facts", "Empty state", "Create a task with the minimum shape", "Priority starts unchosen", "Server validation surfaces at the fields", "Edit changes name and priority only", list-half of the 33% display
      - depends: T-01 · parallel: no
      - verify: `npm run verify`

- [x] **T-04 · Detail and subtasks — SubtasksPanel, dialogs, detail route, integration suite** ✔ 2026-08-14, verify green (353 tests) + live pass against the real API (login → nav → create → subtasks → tick → 50% recompute → list)
      - files: `components/tasks/SubtasksPanel.tsx` + `.test.tsx` (exports `routeSubtaskProblem`), `components/tasks/DeleteTaskDialog.tsx` + `.test.tsx`, `components/tasks/DeleteSubtaskDialog.tsx` + `.test.tsx`, `app/(app)/tasks/[taskId]/page.tsx`, `app/(app)/tasks/tasks.integration.test.tsx`, `adf-fusion.overrides.css` (subtask editor rows + `nb-taskDetail` flex pin), `en.ts`/`pt.ts` (detail + subtasks + delete keys, both catalogs)
      - covers: FR-11 UI + BR-05 confirms · scenarios: "Detail renders subtasks and the derived status" (dates unswapped), "Ticking a checkbox…" (wire + repaint), "Unticking recomputes downward", "A non-integer proportion displays the server's integer", "Adding a subtask", "The server's date-range rejection surfaces localized", "Deleting a subtask is explicit and repaints", "Deleting a task is explicit and warns about its subtasks", "A foreign or missing task id shows not-found", "Portuguese locale end to end", list-mount refetch (plan risk 4)
      - depends: T-03 · parallel: no
      - verify: `npm run verify` (full suite green = the `implement` step's `verify_green` gate)

## Coverage check

All 18 scenarios are cited: T-01 carries the two wire-discipline halves at unit level, T-02 the
chrome, T-03 scenarios 1–8's list/form set, T-04 scenarios 9–15 + 17–18 end-to-end. Scenario 16
(guard redirect) is reused `RouteGuard` evidence per the plan — no new test, noted explicitly.
