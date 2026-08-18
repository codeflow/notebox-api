# Plan — Task dates, card link and rich-text details UI (notebox-web)

**ID:** features/013-task-details-notebox-web
**User Story:** US-4.2
**Version:** v1
**Status:** Approved (human approval 2026-08-18)
**Date:** 2026-08-18
**Project:** notebox-web (satellite; react/next — Next.js app router, TipTap, DOMPurify, MSW/Vitest)

## Origin
- **Spec:** `features/013-task-details-notebox-web/spec.md` (approved 2026-08-18; 22 scenarios; no
  new OQs — two client conventions declared: empty URL → `null`, emptied editor → `null`).
- **Consumes:** `features/012-task-details-notebox-api/contracts/task-details.md` (wire truth,
  incl. its *Documented edges*), design handoff screens 15/16 (US-4.2 regions), feat-011's shipped
  task module (`lib/tasks/viewModel.ts` full-echo builders, `TaskForm`, `SubtasksPanel`,
  `TasksTable`, `TaskProgress`, the detail/edit pages and their `LoadState` idiom, MSW handler
  roster), feat-006's rich-text stack (`RichTextEditor`, `RichTextValue`, `sanitizeRichText`,
  `recordEditorExtensions`, `imagesClient.fetchObjectUrl`/`upload`).
- **Companion artifacts:** `data-model.md` (wire mirrors + view-model deltas + invariants),
  `contracts/interfaces.md` (client, view model, component APIs, i18n keys, MSW additions).

## Approach (three sentences)
The wire mirrors in `lib/api/types.ts` grow by exactly feat-012's additive fields — with the poison
dates made *unconstructable* (`startDate?: never; endDate?: never` on `TaskInput`, the shipped
`status?: never` idiom) and `card` a **required** key on `SubtaskInput` so PUT-replace omission stays
a compile error — and the view model gains the two contract-edge conventions as pure functions
(`toCardInput`: `null` iff both fields blank, empty URL → `null`; `toDetails`: `''`/`<p></p>` → `null`)
plus `taskToDraft`, so a name-only edit and a checkbox tick echo `card`/`details` verbatim (the
rollout fix, scenarios 10/13). On screen, feat-011's components are *extended, not replaced*:
`TasksTable` gains Start/End/Card columns, `TaskForm` gains the shared `CardFields` pair, the
detail's *Derived metrics* `af-nameValue` gains the dated rows with their `min`/`max` hints, a
second `af-panelBox` shows the card with a `CardLink` ("Open <code>", `target=_blank`,
`rel=noopener noreferrer`), and the subtask grid gains a Card column, card inputs in its inline
editor row and the "Span … → …" footer — every repaint still coming from the returned parent
`TaskDto`, never from client math. Details live where design 16 puts them — a **Details tab** next
to Subtasks in an `af-panelTabbed` on the detail page — rendered read-only through `RichTextValue`
and edited **inline in that tab** with the shipped `RichTextEditor` (Save = `tasksClient.update`
with a full-echo body → `onTaskUpdated(response)`), so the tab literally repaints from the response;
the create/edit `TaskForm` carries no details editor (create sends `details: null`, edit echoes the
loaded value silently).

## Design (seams)

**Types (`lib/api/types.ts`, feat-013 block appended).** `CardInput`/`CardDto` (`{ code; url:
string | null }`), `TaskInput` + `card: CardInput | null; details: string | null; startDate?: never;
endDate?: never`, `SubtaskInput` + `card: CardInput | null` (required key), `TaskDto` +
`startDate/endDate/card/details`, `TaskListItemDto` + `startDate/endDate/card` (no details —
contract), `SubtaskDto` + `card`. `tasksClient.ts` is untouched — same eight calls, richer types.

**View model (`lib/tasks/viewModel.ts`, pure).** `CardDraft { code; url }` (`''` = empty);
`TaskDraft` + `card: CardDraft; details: string` (dialect HTML, `''` = none); `SubtaskDraft` +
`card`. New: `toCardInput(draft): CardInput | null` — `null` iff `code.trim() === '' &&
url.trim() === ''`, else `{ code, url: url === '' ? null : url }` (whitespace-only code with a URL is
*sent* so the server answers `task.card.code.required` — the unchosen-priority posture);
`isEmptyRichText(html)` = `html.trim() === '' || html === '<p></p>'` (feat-006's `EMPTY_RICH`
convention); `toDetails(html)` → `null` when empty; `cardToDraft(card: CardDto | null): CardDraft`;
`taskToDraft(task: TaskDto): TaskDraft` (the edit page's initial — echo source);
`toTaskInput(draft)` now emits `card` + `details`; `toSubtaskInput(source, overrides)` echoes
`card: source.card`; `draftToSubtaskInput(draft, done)` emits `card: toCardInput(draft.card)`;
`subtaskToDraft` fills the card draft. `TaskFormErrors` + `cardCode/cardUrl`; `SubtaskFormErrors`
+ `cardCode/cardUrl`. Routing (`routeTaskProblem` in `TaskForm.tsx`, `routeSubtaskProblem` in
`SubtasksPanel.tsx`): violation field tails `/(?:^|\.)card\.code$/` → `cardCode`,
`/(?:^|\.)card\.url$/` → `cardUrl` (checked **before** the generic `name`/other fallbacks so
`card.code` never lands on `name`); bare codes `task.card.code.required|too_long` → `cardCode`,
`task.card.url.invalid|too_long` → `cardUrl`, `task.details.too_long` and `task.dates.not_writable`
→ `form`.

**Date display (`lib/tasks/formatDate.ts`).** `formatIsoDate(iso: string, locale: 'en' | 'pt'):
string` → `dd-MMM-yyyy` with the month from `Intl.DateTimeFormat(locale, { month: 'short' })`
(trailing period stripped: `04-Aug-2026` / `04-ago-2026`) — the design's ADF date shape, locale
month. Applied to the new task dates (list columns, metrics panel, span footer) **and to the
existing subtask date cells** so one grid never mixes shapes; feat-011's single pinned ISO
assertion is updated (named in the blast radius — a presentation change on the same screen, not a
behaviour change; veto here if you'd rather keep raw ISO everywhere).

**Components (`components/tasks/`).** New `CardLink({ card, label? })` — `null` card → `—`; code
without URL → plain code; with URL → `<a href target="_blank" rel="noopener noreferrer">` whose text
is the code, or `t('tasks.card.open', { code })` when `label="open"` (detail panel). New
`CardFields({ draft, onChange, errors, idPrefix })` — Code + URL `af-inputText` inputs with
labels, `aria-invalid`, and the `nb-fieldError` spans, reused by `TaskForm` and by the subtask
editor cell. New `TaskDetailsTab({ task, onTaskUpdated })` — view mode: `RichTextValue` or the
empty state + "Edit details"; edit mode: `RichTextEditor` (`uploadImage` from the shared
`useRichImageUpload()` hook, extracted from `ValueField.tsx`), Save → `tasksClient.update(task.id,
toTaskInput({ ...taskToDraft(task), details }))` → `onTaskUpdated(parent)`; Cancel discards;
`ApiError` → `routeTaskProblem` → form-level `nb-msg-error`. Extended: `TasksTable` (+3 columns
after Status: `tasks.list.column.start/end/card`), `TaskForm` (+`CardFields`; initial from
`taskToDraft` on edit — details echoed, no editor), `SubtasksPanel` (+Card column, card inputs in
`editorCells`, `routeSubtaskProblem` card routing, formatted date cells, footer span line).
Detail page `[taskId]/page.tsx`: metrics `af-nameValue` + Start/End rows (value + hint span);
`.af-boxRow` gains `.af-panelBox.nb-taskCardBox` (Card panel — `CardLink label="open"`, code, URL,
the *not a shared entity* note, empty state; `flex: none; min-width` to dodge the panel-width
trap); below, `af-panelTabbed` with tabs `tasks.detail.tabs.subtasks` (count) /
`tasks.detail.tabs.details` hosting `SubtasksPanel` / `TaskDetailsTab`; the whole page still
repaints via `setState({ status: 'ready', task })`. Edit page: `initial={taskToDraft(task)}`; new
page: `initial={EMPTY_TASK_DRAFT}`.

**i18n.** ~24 new keys en + pt (`tasks.list.column.start|end|card`, `tasks.detail.start|end`,
`tasks.detail.startHint|endHint`, `tasks.detail.tabs.subtasks|details`, `tasks.card.title|code|
url|open|none|note`, `tasks.form.card.code|url|codePlaceholder|urlPlaceholder`,
`tasks.subtasks.column.card`, `tasks.subtasks.span`, `tasks.details.empty|edit|save|cancel|
saveFailed`); the keyset-parity test pins en/pt.

**Tests (Vitest + RTL + MSW, `npm run verify`).** Pure: `viewModel.test.ts` (send rules, empty
detection, echo builders), `formatDate.test.ts`. Component: `CardLink`, `CardFields`,
`TaskDetailsTab`, `TasksTable` (columns), `TaskForm` (card fields + routing), `SubtasksPanel`
(card column, editor, tick echo via `onBody`, span line). Integration
(`tasks.integration.test.tsx`): the spec's page-level scenarios with request-body assertions
through the `onBody` handler idiom and the pt pass. MSW fixtures gain dates/card/details.

## Alternatives rejected
1. **Details editor inside `TaskForm` (create/edit pages):** duplicates the editing surface, puts
   the editor where design 16 doesn't, and can't "repaint from the response" (the edit page
   navigates away and the detail re-fetches). The Details tab hosts view *and* inline edit — the
   subtask-panel precedent, one surface, response repaint for free.
2. **Client-side date derivation / card validation:** the spec forbids it; the API is the truth
   and answers localized. Only the two null-vs-empty conventions live client-side.
3. **A second editor/sanitizer or a "task dialect":** one dialect project-wide (C-08); reuse
   feat-006's stack unchanged, extract only the upload adapter into a hook.
4. **Keeping raw ISO dates for the new columns:** the approved spec and the design show
   `04-Aug-2026`; mixing shapes in one grid is worse than updating one pinned assertion.
5. **`card` optional on `SubtaskInput`:** would make omission constructable and reopen the exact
   rollout hazard this feature closes; required key + full-echo builder is the whole point.
6. **Trimming/normalizing card values client-side:** the server validates length/blank/URL and
   answers with catalog keys; the client only decides `null` vs value at the two contract edges.
7. **Passing the saved task from the edit page to the detail page (router state/cache):** not
   needed once details edit inline; the edit page keeps feat-011's navigate-then-load flow.

## Reversibility
| Decision | Kind |
|---|---|
| Wire mirrors (types) incl. `startDate?/endDate?: never`, required `card` on `SubtaskInput` | **one-way-ish** (mirror of the API contract; drift = compile error) |
| Send rules: `toCardInput` (null iff both blank; `''` url → null), `toDetails` (empty → null) | reversible conventions (pure functions, one place each) |
| Details edited inline in the Details tab (not in `TaskForm`) | reversible UI decision |
| `formatIsoDate` dd-MMM-yyyy incl. subtask cells | reversible presentation |
| `CardLink`/`CardFields`/`TaskDetailsTab` as separate components; `useRichImageUpload` hook extraction | reversible |
| Card panel display-only on the detail page (editing on the form, per the approved spec) | reversible UI decision |

## Blast radius (satellite `notebox-web`)
- **New (7 + tests):** `components/tasks/CardLink.tsx`, `components/tasks/CardFields.tsx`,
  `components/tasks/TaskDetailsTab.tsx`, `components/annotationRecords/useRichImageUpload.ts`
  (extracted), `lib/tasks/formatDate.ts`; tests `CardLink.test.tsx`, `CardFields.test.tsx`,
  `TaskDetailsTab.test.tsx`, `formatDate.test.ts`.
- **Modified (13):** `lib/api/types.ts` (feat-013 block), `lib/tasks/viewModel.ts` (+ test),
  `components/tasks/TaskForm.tsx` (+ test), `components/tasks/SubtasksPanel.tsx` (+ test — incl.
  the one ISO date assertion → formatted), `components/tasks/TasksTable.tsx` (+ test),
  `components/annotationRecords/ValueField.tsx` (imports the extracted hook; behaviour identical),
  `app/(app)/tasks/[taskId]/page.tsx`, `app/(app)/tasks/[taskId]/edit/page.tsx`,
  `app/(app)/tasks/new/page.tsx`, `app/(app)/tasks/tasks.integration.test.tsx`,
  `test/msw/handlers.ts` (fixtures + `onBody` handlers), `lib/i18n/messages/en.ts` + `pt.ts`,
  `src/styles/adf-fusion.overrides.css` (`.nb-taskCardBox`, `.nb-cardFields`, `.nb-taskTabs`,
  `.nb-detailsTab`, `.nb-dateHint`).
- **Hub:** `features/013-task-details-notebox-web/{plan,data-model,contracts/interfaces}.md`.
- **Explicitly untouched:** `lib/api/tasksClient.ts`, `authFetch`, guard/routing, `imagesClient`,
  `RichTextEditor`, `RichTextValue`, `sanitize.ts`, `editorExtensions.ts`, every annotation screen,
  `TaskProgress`, `PriorityLabel`, both delete dialogs, the base `adf-fusion.css`.

## Risk
| Risk | Signal that reveals it |
|---|---|
| A builder still omits `card`/`details` (the rollout hazard survives) | scenario 10/13 tests assert exact PUT bodies via `onBody`; TypeScript rejects a `SubtaskInput` without `card` |
| Old fixtures/tests break on the required `card` key | compile errors in `test/msw/handlers.ts` and tests — fixed at the type task, not later |
| `<p></p>` ghost stored on save | `toDetails` unit test + scenario 17 integration (`details: null` in the body) |
| Card panel collapses/stretches (`af-panelBox` flex trap) | `.nb-taskCardBox { flex: none; min-width }` like `.nb-taskMetricsBox`; live pass on the real API at audit |
| Violation routing lands `card.code` on `name` (regex order) | routing unit tests with `create.input.card.code` paths |
| `Intl` month names differ across environments (CI vs local) | Node full-ICU is standard ≥ 13; `formatDate.test` pins en `Aug` and pt `ago` |
| Tab switching loses SubtasksPanel editor state | tabs render both panels and toggle visibility (`af-tabPanel.active`), no unmount |
| Details edit sends a stale echo (task changed by a subtask tick meanwhile) | the tab reads `task` from the page's single state; save builds from the current prop |
| `next lint`/prettier on new files | `npm run verify` runs lint first — red is loud |

## Test plan — 22 scenarios → 10 test files
- `lib/tasks/viewModel.test.ts` — `toCardInput` matrix (both blank → null; code only → url null;
  whitespace code + url → sent; url only → sent with code `''`), `toDetails`/`isEmptyRichText`
  (`''`, `<p></p>`, real HTML), `taskToDraft` ↔ `toTaskInput` round-trip echoes card+details,
  `toSubtaskInput` echoes card, `draftToSubtaskInput` card, `subtaskToDraft`.
- `lib/tasks/formatDate.test.ts` — `2026-08-04` → `04-Aug-2026` (en) / `04-ago-2026` (pt).
- `components/tasks/CardLink.test.tsx` — null → `—`; code-only → text; url → anchor with
  `target`/`rel`; `label="open"` text.
- `components/tasks/CardFields.test.tsx` — inputs, labels, error rendering, `aria-invalid`.
- `components/tasks/TaskForm.test.tsx` — card fields present; POST body card/`url: null`; routing
  of `task.card.code.required`/`task.card.url.invalid` to their fields; edit echoes details.
- `components/tasks/SubtasksPanel.test.tsx` — Card column (link/`—`); editor card inputs and
  bodies; tick echoes card; span footer; formatted dates; `routeSubtaskProblem` card routing.
- `components/tasks/TaskDetailsTab.test.tsx` — hostile served HTML rendered without script; image
  requested by id; edit → PUT `details` with `<strong>`; emptied → `details: null`; too_long → form
  error; empty state.
- `components/tasks/TasksTable.test.tsx` — Start/End/Card columns, `—` for null.
- `app/(app)/tasks/tasks.integration.test.tsx` — the page-level scenarios (metrics panel + hints,
  span, no date input / no date keys on PUT, card panel + Open link, edit echoes card+details,
  clear card, Details tab flow, pt locale, guard/not-found regression).
- `lib/i18n/keysetCoverage.test.ts` — unchanged, pins the new keys' en/pt parity.
