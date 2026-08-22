# Tasks — Task dates, card link and rich-text details UI (notebox-web)

**ID:** features/013-task-details-notebox-web · **US:** US-4.2 · **Date:** 2026-08-18
**Source:** plan.md + contracts/interfaces.md + data-model.md (approved 2026-08-18)
**Status:** Approved (human approval 2026-08-18)

> Strictly sequential. T-02/T-03/T-04 all append to the shared i18n catalogs (en **and** pt in the
> same task — `keysetCoverage.test.ts` fails otherwise) and to `adf-fusion.overrides.css`, and
> T-05 assembles what T-02..T-04 build, so no task pair is worktree-safe. Every task ships its
> tests; the satellite's `npm run verify` (clean + lint + prettier + tsc + vitest + build) must be
> green at the end of each task — run in `../notebox-web`. Ordered by dependency, then risk: the
> required `card` key on `SubtaskInput` and the poison `never`s ripple through every fixture and
> builder, so the wire foundation goes first while changing the plan is still cheap. All file
> paths below are satellite-relative.

- [x] **T-01 · Wire foundation — types with structural guards, view-model send rules and echo builders, date formatter, MSW fixtures** ✔ 2026-08-21, verify green (366 tests). As predicted, tsc forced minimal literal widenings beyond the file list — `TaskForm`/`SubtasksPanel` error/draft literals, `TaskForm.test`/`SubtasksPanel.test`/`tasksClient.test` fixtures and body expectations (now asserting the echoed `card: null`/`details: null` — the new contract truth, no behaviour change beyond the widened bodies).
      - files: `lib/api/types.ts` (feat-013 block: `CardInput`/`CardDto`; `TaskInput` + `card`/`details` + `startDate?: never`/`endDate?: never`; `SubtaskInput` + required `card`; DTOs + dates/card/details), `lib/tasks/viewModel.ts` (`CardDraft`, `EMPTY_CARD_DRAFT`, `EMPTY_TASK_DRAFT`, `TaskDraft`/`SubtaskDraft` + card/details, error shapes + `cardCode`/`cardUrl`, `toCardInput`, `cardToDraft`, `isEmptyRichText`, `toDetails`, `taskToDraft`, `toTaskInput`/`toSubtaskInput`/`draftToSubtaskInput`/`subtaskToDraft` deltas) + `lib/tasks/viewModel.test.ts`, `lib/tasks/formatDate.ts` + `lib/tasks/formatDate.test.ts`, `test/msw/handlers.ts` (fixtures gain dates/card/details incl. `TASK_DETAILED`; `taskUpdateSuccess` gains `onBody`; `taskProblem`), plus the two **compile-forced** page initials — `app/(app)/tasks/new/page.tsx` → `EMPTY_TASK_DRAFT`, `app/(app)/tasks/[taskId]/edit/page.tsx` → `taskToDraft(task)` (the echo source lands with the type that demands it)
      - covers: INV-W1..W4, FR-12 (structural), FR-13/FR-14 send rules · scenarios: "Task dates cannot be sent, structurally" (types), "An empty URL field is sent as null" (`toCardInput` matrix), "Editing a task echoes the card and details it does not change" (`taskToDraft` → `toTaskInput` round-trip), "Ticking a subtask echoes its card" (`toSubtaskInput`), "An emptied editor clears the details" / "A task without details … sends none" (`toDetails`), date shape `04-Aug-2026` / `04-ago-2026`
      - depends: — · parallel: no
      - verify: `npm run verify` — tsc is the first signal: every fixture and builder must carry the required `card` key before this task is green

- [x] **T-02 · Card and dates on the list and the task form — `CardLink`, `CardFields`, `TasksTable` columns, `TaskForm` card fields + routing** ✔ 2026-08-21, verify green (377 tests)
      - files: `components/tasks/CardLink.tsx` + `.test.tsx`, `components/tasks/CardFields.tsx` + `.test.tsx`, `components/tasks/TasksTable.tsx` + `.test.tsx` (+ Start · End · Card columns after Status; `formatIsoDate`; `—` for null), `components/tasks/TaskForm.tsx` + `.test.tsx` (`CardFields`; `routeTaskProblem` card routing — `card.code`/`card.url` tails checked before `name`; edit initial via `taskToDraft` echoes details silently), `lib/i18n/messages/en.ts` + `pt.ts` (`tasks.list.column.start|end|card`, `tasks.card.title|code|url|open|none|note`, `tasks.form.card.*`), `src/styles/adf-fusion.overrides.css` (`.nb-cardFields`)
      - covers: FR-12 (list display), FR-13 (form + rows), C-09 · scenarios: "The list shows the served dates per row", "The list shows the task card link per row", "A code-only card shows no link" (`CardLink`), "Creating a task with a card sends it", "An empty URL field is sent as null, never as an empty string" (POST body), "Server card errors surface at their fields", "Clearing both card fields removes the card" (PUT body `card: null`; the panel repaint is T-05), create sends `details: null`
      - depends: T-01 · parallel: no
      - verify: `npm run verify`

- [x] **T-03 · Subtasks panel — Card column, editor card inputs, routing, formatted date cells, span footer, tick echoes card** ✔ 2026-08-21, verify green (383 tests). The plan's predicted assertion update landed in TWO places — `SubtasksPanel.test` and the integration suite both pinned the raw ISO cells; both now pin `dd-MMM-yyyy`.
      - files: `components/tasks/SubtasksPanel.tsx` + `.test.tsx` (Card column with `CardLink`; `CardFields` inside the editor cell; `routeSubtaskProblem` card routing; date cells via `formatIsoDate` — feat-011's one ISO assertion updated; footer `N subtasks · Span … → …` when both task dates exist; `toggleDone` body asserted via `onBody`), `lib/i18n/messages/en.ts` + `pt.ts` (`tasks.subtasks.column.card`, `tasks.subtasks.span`), `src/styles/adf-fusion.overrides.css` (editor cell hooks)
      - covers: FR-13 (subtask card), FR-12 (span footer), the rollout fix · scenarios: "A subtask carries a card under the same rules", "Ticking a subtask echoes its card", "A task without derived dates shows the empty marks" (no span line), "A subtask change repaints the dates from the returned parent" (footer from the prop, no request)
      - depends: T-02 · parallel: no
      - verify: `npm run verify`

- [x] **T-04 · Details tab — `useRichImageUpload` extraction, `TaskDetailsTab` view + inline edit with the shipped rich-text stack** ✔ 2026-08-21, verify green (389 tests). Test strategy per feat-006 precedent: the editor is a textarea stub in the tab's suite (jsdom cannot host TipTap), so the tab's contract is what is pinned; the editor itself stays covered by `RichTextEditor.test`. Observed once under full-suite load: a pre-existing `LoginForm` in-flight-disabled timing flake (passes 2/2 in isolation; component untouched here) — backlog, same class as feat-011's `TypeBuilderForm` note.
      - files: `components/annotationRecords/useRichImageUpload.ts` (extracted; `components/annotationRecords/ValueField.tsx` imports it — behaviour identical, its tests untouched), `components/tasks/TaskDetailsTab.tsx` + `.test.tsx` (view: `RichTextValue` | empty state + "Edit details"; edit: `RichTextEditor` + Save/Cancel; Save → `tasksClient.update(task.id, toTaskInput({ ...taskToDraft(task), details }))` → `onTaskUpdated`; `ApiError` → `routeTaskProblem(...).form`), `lib/i18n/messages/en.ts` + `pt.ts` (`tasks.details.empty|edit|save|cancel|saveFailed`, `tasks.detail.tabs.*`), `src/styles/adf-fusion.overrides.css` (`.nb-detailsTab`)
      - covers: FR-14, C-07/C-08 client share, INV-W4/W5/W6 · scenarios: "Served details render through the client sanitizer" (no script in the DOM; image requested by `data-image-id`), "Editing details uses the shipped editor and saves its dialect" (PUT `details` with `<strong>`; repaint from the response), "An emptied editor clears the details" (PUT `details: null`; empty state), "The server's size rejection surfaces at the form", "A task without details shows the empty state"
      - depends: T-03 · parallel: no
      - verify: `npm run verify`

- [x] **T-05 · Detail page assembly + integration suite + live pass — metrics rows, Card panel, tabs, page-level scenarios** ✔ 2026-08-21, verify green (397 tests — the `implement` step's `verify_green` gate) **+ live pass against the real API on develop** (quarkus:dev + Dev Services MySQL at V6, seeded dev member): login → new task with card (POST carried `card`) → detail: metrics with hints, Card panel with "Open OPS-2481", tabs → inline subtask with dates + card → the API derived `04-Aug-2026 → 06-Aug-2026` (metrics + span footer + Card column) → tick: status 100% **and the subtask card survived the real PUT-replace** (the feat-012 rollout hazard closed live) → Details tab: typed into the real TipTap editor, saved, rendered `<p>…</p>` → emptied editor → `details: null` → "No details yet." → list: Start/End/Card columns populated. No console errors; the `af-panelBox` flex trap did not bite (`.nb-taskCardBox`).
      - files: `app/(app)/tasks/[taskId]/page.tsx` (metrics `af-nameValue` + Start/End rows with `min`/`max` hints via `.nb-dateHint`; `.af-panelBox.nb-taskCardBox` Card panel — `CardLink label="open"`, code, URL, `tasks.card.note`, empty state; `af-panelTabbed` Subtasks (count) / Details hosting `SubtasksPanel` / `TaskDetailsTab`, both mounted, `af-tabPanel.active` toggles), `lib/i18n/messages/en.ts` + `pt.ts` (`tasks.detail.start|end|startHint|endHint`), `src/styles/adf-fusion.overrides.css` (`.nb-taskCardBox { flex: none; min-width }`, `.nb-taskTabs`, `.nb-dateHint`), `app/(app)/tasks/tasks.integration.test.tsx` (the page-level scenarios with `onBody` request assertions + the pt pass + guard/not-found regression)
      - covers: FR-12/FR-13/FR-14 at the page, C-01/C-02/C-09 · scenarios: "The detail shows the served span with its derivation hints", "A task without derived dates shows the empty marks", "A subtask change repaints the dates from the returned parent", "Task dates cannot be sent, structurally" (no date input; PUT body has no date keys), "The detail shows the card with an Open link", "A code-only card shows no link" (panel), "Editing a task echoes the card and details it does not change" (integration PUT body), "Clearing both card fields removes the card" (panel empty state after repaint), "Portuguese locale end to end", "Guard and not-found idioms are unchanged"
      - depends: T-04 · parallel: no
      - verify: `npm run verify` (full suite green = the `implement` step's `verify_green` gate) **+ a live pass against the real API on develop** (feat-011 precedent: login → tasks → create with card → detail: derived span, card panel, subtask card, tick echo keeps the card, Details tab edit/clear) — the panel-width trap and the sanitizer round-trip are only provable live

## Coverage check

Every one of the spec's 22 scenarios is cited by at least one task; each has exactly one **primary**
owner (T-01: 6 at the pure level · T-02: 6 · T-03: 4 · T-04: 5 · T-05: 10 at the page level), and
the deliberate overlaps — send rules proven pure in T-01 and again on the wire in T-02/T-03,
"clearing the card" split into body (T-02) and panel repaint (T-05), "empty marks" split into
footer (T-03) and panel (T-05) — are complementary assertions of the same behaviour, as in
feat-011. No task cites zero scenarios; no scenario is uncovered.

## Implement close-out (2026-08-21)

All five tasks `[x]`, `npm run verify` green at **397 tests** (T-01 366 → T-02 377 → T-03 383 →
T-04 389 → T-05 397; feat-011's 353 baseline +44), build compiled, plus the live pass above. Beyond
the plan's file list, each noted on its task: type-forced literal widenings in feat-011's
tests/components (T-01), the integration suite's second pinned-ISO assertion (T-03), the
`LoginForm` under-load flake observation (T-04, pre-existing, untouched). Everything the plan
marked "explicitly untouched" stayed untouched: `tasksClient.ts`, `RichTextEditor`/`RichTextValue`/
`sanitize.ts`/`editorExtensions.ts`, `authFetch`/guard, `imagesClient`, the annotation screens,
`TaskProgress`, `PriorityLabel`, both delete dialogs, the base `adf-fusion.css`.
