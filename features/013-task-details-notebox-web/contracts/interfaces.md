# Contract — client interfaces (feat-013, US-4.2 web)

**Consumes:** `features/012-task-details-notebox-api/contracts/task-details.md` (wire truth).
**Extends:** `features/011-tasks-notebox-web/contracts/interfaces.md` — everything there stands;
this document is the **delta**. Signatures below are what `tasks`/`implement` code against and the
audit checks.

## `lib/api/tasksClient.ts` — unchanged
Same eight calls; the richer `TaskInput`/`SubtaskInput`/`TaskDto`/`TaskListItemDto`/`SubtaskDto`
flow through the existing signatures. Errors: `ApiError` (`problem.code`, `problem.violations[]`)
as before.

## `lib/tasks/viewModel.ts` (pure, DOM-free) — delta

```ts
export interface CardDraft { code: string; url: string; }
export const EMPTY_CARD_DRAFT: CardDraft;                       // { code: '', url: '' }
export interface TaskDraft { name: string; priority: TaskPriority | ''; card: CardDraft; details: string; }
export const EMPTY_TASK_DRAFT: TaskDraft;
export interface SubtaskDraft { name: string; startDate: string; endDate: string; card: CardDraft; }
export interface TaskFormErrors { name: string | null; priority: string | null; cardCode: string | null; cardUrl: string | null; form: string | null; }
export interface SubtaskFormErrors { name: string | null; dates: string | null; cardCode: string | null; cardUrl: string | null; form: string | null; }

/** null iff both fields are blank; empty url → null (never ''); whitespace-only code with a url is SENT (server answers). */
export function toCardInput(draft: CardDraft): CardInput | null;
export function cardToDraft(card: CardDto | null): CardDraft;
/** '' or '<p></p>' (feat-006 EMPTY_RICH) → true. */
export function isEmptyRichText(html: string): boolean;
/** empty → null (only null clears server-side); otherwise the dialect HTML as-is. */
export function toDetails(html: string): string | null;
/** The edit-page initial and the echo source for every task update. */
export function taskToDraft(task: TaskDto): TaskDraft;
export function toTaskInput(draft: TaskDraft): TaskInput;       // now emits card + details
export function toSubtaskInput(source: SubtaskDto, overrides?: Partial<SubtaskInput>): SubtaskInput; // echoes card
export function draftToSubtaskInput(draft: SubtaskDraft, done: boolean): SubtaskInput;             // card via toCardInput
export function subtaskToDraft(subtask: SubtaskDto): SubtaskDraft;                                  // card via cardToDraft
```

## `lib/tasks/formatDate.ts` (new, pure)

```ts
/** ISO yyyy-MM-dd → dd-MMM-yyyy with the locale's short month (trailing period stripped): 04-Aug-2026 / 04-ago-2026. */
export function formatIsoDate(iso: string, locale: 'en' | 'pt'): string;
```

## Violation routing — delta (in the components, as shipped)

```ts
export function routeTaskProblem(problem: Problem): TaskFormErrors;      // TaskForm.tsx
export function routeSubtaskProblem(problem: Problem): SubtaskFormErrors; // SubtasksPanel.tsx
```
| source | match | → |
|---|---|---|
| violation field | `/(?:^|\.)card\.code$/` (checked before `name`) | `cardCode` |
| violation field | `/(?:^|\.)card\.url$/` | `cardUrl` |
| bare code | `task.card.code.required`, `task.card.code.too_long` | `cardCode` |
| bare code | `task.card.url.invalid`, `task.card.url.too_long` | `cardUrl` |
| bare code | `task.details.too_long`, `task.dates.not_writable` | `form` |
| (rest) | as feat-011 | unchanged |

## Component APIs (`components/tasks/`, all `'use client'`)

```ts
// new
export function CardLink({ card, label }: { card: CardDto | null; label?: 'code' | 'open' }): JSX.Element;
//   null → <span>—</span>; no url → <span>{code}</span>;
//   url → <a href={url} target="_blank" rel="noopener noreferrer">{label === 'open' ? t('tasks.card.open', { code }) : code}</a>
export function CardFields({ draft, onChange, errors, idPrefix }: {
  draft: CardDraft; onChange(next: CardDraft): void;
  errors: { cardCode: string | null; cardUrl: string | null }; idPrefix: string;
}): JSX.Element;   // two labelled af-inputText inputs + nb-fieldError spans, aria-invalid
export function TaskDetailsTab({ task, onTaskUpdated }: { task: TaskDto; onTaskUpdated(task: TaskDto): void }): JSX.Element;
//   view: <RichTextValue html={task.details}/> | empty state; "Edit details" → edit mode
//   edit: <RichTextEditor value onChange uploadImage={useRichImageUpload()}/> + Save/Cancel;
//   Save → tasksClient.update(task.id, toTaskInput({ ...taskToDraft(task), details: draftHtml })) → onTaskUpdated
//   ApiError → routeTaskProblem(problem).form → nb-msg-error

// extended (props unchanged)
export function TasksTable({ onOpenTask, onNewTask }): JSX.Element;      // + Start · End · Card columns
export function TaskForm({ mode, taskId, initial, onSaved, onCancel }): JSX.Element; // + CardFields; initial: TaskDraft (taskToDraft on edit)
export function SubtasksPanel({ task, onTaskUpdated }): JSX.Element;    // + Card column, editor card inputs, span footer, formatted dates
```

```ts
// components/annotationRecords/useRichImageUpload.ts (extracted from ValueField.tsx, behaviour identical)
export function useRichImageUpload(): (file: File) => Promise<string>;   // validateIconFile → t(rejection) | imagesClient.upload → ref.id
```

Pages: `[taskId]/page.tsx` — metrics rows (Start/End + hints), Card panel (`.nb-taskCardBox`),
`af-panelTabbed` (Subtasks / Details); `[taskId]/edit/page.tsx` — `initial={taskToDraft(task)}`;
`new/page.tsx` — `initial={EMPTY_TASK_DRAFT}`.

## i18n — 24 new client keys (en text final; pt line-parallel at implement)

```
tasks.list.column.start=Start · tasks.list.column.end=End · tasks.list.column.card=Card
tasks.detail.start=Start date · tasks.detail.end=End date
tasks.detail.startHint=min(subtask start) · tasks.detail.endHint=max(subtask end)
tasks.detail.tabs.subtasks=Subtasks · tasks.detail.tabs.details=Details
tasks.card.title=Card · tasks.card.code=Code · tasks.card.url=URL · tasks.card.open=Open {code}
tasks.card.none=No card · tasks.card.note=Not a shared entity: the card lives on this task (and can also live on a subtask).
tasks.form.card.code=Card code · tasks.form.card.url=Card URL
tasks.form.card.codePlaceholder=e.g. OPS-2481 · tasks.form.card.urlPlaceholder=https://…
tasks.subtasks.column.card=Card · tasks.subtasks.span=Span
tasks.details.empty=No details yet. · tasks.details.edit=Edit details · tasks.details.save=Save
tasks.details.cancel=Cancel · tasks.details.saveFailed=Something went wrong. Please try again.
```

## MSW additions (`test/msw/handlers.ts`, append-only)
- Fixtures: `TASK_MIGRATE` gains `startDate: '2026-09-01'`, `endDate: '2026-09-10'`, `card: null`,
  `details: null`, subtasks `card: null`; new `TASK_DETAILED` (dates 2026-08-04/2026-08-29, card
  `OPS-2481`/`https://tracker.example/OPS-2481`, details `<p>plan</p>`, one subtask with card
  `OPS-2482`); `taskRow(overrides)` accepts the new fields.
- Handlers: `taskUpdateSuccess(saved, onBody?)` gains the `onBody` capture (the echo assertions);
  `taskValidationFailed(violations)` reused with `card.code`/`card.url` field paths;
  `taskProblem(code, status)` for the bare `task.details.too_long` 400; images
  `imageFetchSuccess(id, bytes)` for the `data-image-id` resolution assertion (reuse feat-006's if
  present).

## Documented edges (client)
- **No client-side trimming of card values** (plan alternative 6): `{ code: 'X', url: ' ' }` or a
  trailing space is sent verbatim and the server answers `task.card.url.invalid` at the URL field;
  only the exact `''` url maps to `null`.
- **`isEmptyRichText` normalises empty documents** — `''`, `<p></p>`, stacked empty paragraphs and
  break-only paragraphs (`<p><br></p>`) all count as no details (audit finding 6); the Details tab's
  empty state is keyed on that, so served `""`/`<p></p>` (storable via the API's own edge) render
  "No details yet." rather than a blank renderer (audit finding 4), and the next save collapses them
  to `null`.
- **Details-tab save errors** surface as the form-level alert; a field violation on the echoed
  card/name (only a hypothetical legacy row could trigger one) collapses to the server's top-level
  message (audit finding 5).
- **Tabs** use `role=tab`/`aria-selected` and `hidden` panels; no `aria-controls` wiring (a11y nit,
  backlog — the satellite owns accessibility per the compliance note).
- `formatIsoDate` returns a non-`yyyy-MM-dd` value verbatim instead of throwing (unreachable from the
  API's `LocalDate`; guarded anyway).
- The **Card panel is display-only** on the detail page; card editing is on the task form (approved
  spec) even though design 16 draws inputs in the panel.
- **Details are edited in the Details tab only** (inline, response repaint); the create form has no
  editor and sends `details: null`; the edit form echoes details silently.
- `formatIsoDate` is applied to subtask date cells too — feat-011's one ISO assertion changes.
- The `tasks.card.note` copy is design text (screen 16), shown as a hint under the panel.
