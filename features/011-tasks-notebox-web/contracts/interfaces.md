# Contract — feat-011 client interfaces (tasks UI)

**Consumes:** `features/010-tasks-notebox-api/contracts/tasks.md` (wire truth).
**Consumers:** the feat-011 routes/components and their tests. Types live in `lib/api/types.ts`
(see `data-model.md`).

## `lib/api/tasksClient.ts`
Idiom-clone of `annotationRecordsClient.ts`: `BASE = '/tasks'`, per-verb module-private functions
with a one-line JSDoc citing the FR/feature, single `export const tasksClient` at the bottom; all
calls via `authFetch` (Bearer + `ApiError`/`parseProblem`; no Accept-Language — locale is server-side
via the JWT).

```ts
export interface TasksClient {
  list(page: number, size: number): Promise<PageDto<TaskListItemDto>>;  // GET /tasks?page&size — newest first as served (NFR-08, OQ-21)
  create(input: TaskInput): Promise<TaskDto>;                            // POST /tasks — 201; status always 0
  get(id: string): Promise<TaskDto>;                                     // GET /tasks/{id} — with subtasks
  update(id: string, input: TaskInput): Promise<TaskDto>;                // PUT /tasks/{id} — name/priority only
  remove(id: string): Promise<void>;                                     // DELETE /tasks/{id} — 204, cascades subtasks
  addSubtask(taskId: string, input: SubtaskInput): Promise<TaskDto>;     // POST …/subtasks — 201 + PARENT TaskDto
  updateSubtask(taskId: string, subtaskId: string, input: SubtaskInput): Promise<TaskDto>; // PUT — 200 + PARENT
  removeSubtask(taskId: string, subtaskId: string): Promise<TaskDto>;    // DELETE — 200 + PARENT (NOT 204 — the repaint source)
}
export const tasksClient: TasksClient;
```

## `lib/tasks/viewModel.ts` (pure, DOM-free)

```ts
export const TASK_PRIORITIES: readonly TaskPriority[];   // ['LOW','MEDIUM','HIGH','CRITICAL'] — select order
export function toTaskInput(draft: TaskDraft): TaskInput;                 // '' → null (server answers task.priority.required); never a status key
export function toSubtaskInput(source: SubtaskDto, overrides?: Partial<SubtaskInput>): SubtaskInput;
                                                          // FULL echo of name/startDate/endDate/done, then overrides
                                                          // checkbox tick = toSubtaskInput(s, { done: !s.done })
export function draftToSubtaskInput(draft: SubtaskDraft, done: boolean): SubtaskInput;
                                                          // '' → null; `done` EXPLICIT at every call site (add: false; edit: existing.done)
export function subtaskToDraft(subtask: SubtaskDto): SubtaskDraft;        // null → ''
```

## Component APIs

```ts
// components/AppNav.tsx — 'use client'; nav.af-navTabs > button.af-navTab(.selected, aria-current="page")
export function AppNav(): JSX.Element;   // no props; usePathname()+useRouter(); tabs Overview(/) · Annotations(/annotation-types) · Tasks(/tasks)

// components/tasks/TasksTable.tsx — owns fetch/page/size/pager/empty (RecordsGrid donor)
interface TasksTableProps { onOpenTask(taskId: string): void; onNewTask(): void }

// components/tasks/TaskForm.tsx — noValidate; owns POST/PUT; exports the router for tests
interface TaskFormProps {
  mode: 'create' | 'edit'; taskId?: string;               // taskId required when mode='edit'
  initial: TaskDraft; onSaved(task: TaskDto): void; onCancel(): void;
}
export function routeTaskProblem(problem: Problem): TaskFormErrors;       // tails .name/.priority; residue → form

// components/tasks/PriorityLabel.tsx — HIGH → span.st.warn, CRITICAL → span.st.err, LOW/MEDIUM plain; text t('tasks.priority.'+p)
interface PriorityLabelProps { priority: TaskPriority }

// components/tasks/TaskProgress.tsx — span.af-progress > span.bar (width:`${percent}%` — the one sanctioned inline style) + `${percent}%` label
interface TaskProgressProps { percent: number; narrow?: boolean }         // narrow → nb-progressNarrow (130px list); default 210px (detail)

// components/tasks/SubtasksPanel.tsx — fetchless; every mutation lifts the returned PARENT up
interface SubtasksPanelProps { task: TaskDto; onTaskUpdated(task: TaskDto): void }
export function routeSubtaskProblem(problem: Problem): SubtaskFormErrors; // tail .name → name; ANY other violation → dates (closed matrix); residue → form

// components/tasks/DeleteTaskDialog.tsx — af-dialogOverlay.open > af-dialog (DeleteRecordDialog shape); names the task + subtasks-removed-permanently warning
interface DeleteTaskDialogProps { taskName: string; open: boolean; onConfirm(): void; onCancel(): void }

// components/tasks/DeleteSubtaskDialog.tsx — same shape; cannot-be-undone copy
interface DeleteSubtaskDialogProps { subtaskName: string; open: boolean; onConfirm(): void; onCancel(): void }
```

**Route pages** (all `'use client'` default exports):
- `app/(app)/tasks/page.tsx` — `af-panelHeader` h1 + `TasksTable` (`onOpenTask` → push `/tasks/${id}`; `onNewTask` → push `/tasks/new`).
- `app/(app)/tasks/new/page.tsx` — `TaskForm` create (`onSaved` → push `/tasks/${saved.id}`; cancel → `/tasks`).
- `app/(app)/tasks/[taskId]/page.tsx` — `LoadState` fetch via `tasksClient.get`; `nb-detailHeader` + metrics box + `SubtasksPanel` + `DeleteTaskDialog` (confirm → `remove` → push `/tasks`).
- `app/(app)/tasks/[taskId]/edit/page.tsx` — `LoadState` fetch; `TaskForm` edit seeded from the DTO (`onSaved`/cancel → push `/tasks/${taskId}`).

## i18n — 52 new client keys (en text final; pt line-parallel at implement)

- **Nav (3):** `nav.overview` Overview · `nav.annotations` Annotations · `nav.tasks` Tasks
- **List (10):** `tasks.list.title` Tasks · `tasks.list.new` New task · `tasks.list.empty` No tasks yet · `tasks.list.column.task` Task · `tasks.list.column.priority` Priority · `tasks.list.column.status` Status · `tasks.grid.pagerOf` Page · `tasks.grid.pageSize` Page size · `tasks.grid.previous` Previous · `tasks.grid.next` Next
- **Priorities (4):** `tasks.priority.LOW` Low · `tasks.priority.MEDIUM` Medium · `tasks.priority.HIGH` High · `tasks.priority.CRITICAL` Critical
- **Form (9):** `tasks.form.title.new` New task · `tasks.form.title.edit` Edit task · `tasks.form.name` Name · `tasks.form.priority` Priority · `tasks.form.priority.placeholder` Select a priority · `tasks.form.save` Save · `tasks.form.saving` Saving… · `tasks.form.cancel` Cancel · `tasks.form.saveFailed` Something went wrong. Please try again.
- **Detail (6):** `tasks.detail.metrics` Derived metrics · `tasks.detail.status` Status · `tasks.detail.priority` Priority · `tasks.detail.edit` Edit · `tasks.detail.delete` Delete · `tasks.notFound` Task not found
- **Subtasks (14):** `tasks.subtasks.title` Subtasks · `tasks.subtasks.add` Add subtask · `tasks.subtasks.empty` No subtasks yet · `tasks.subtasks.column.done` Done · `tasks.subtasks.column.name` Subtask · `tasks.subtasks.column.start` Start date · `tasks.subtasks.column.end` End date · `tasks.subtasks.column.actions` Actions · `tasks.subtasks.count` subtasks · `tasks.subtasks.edit` Edit · `tasks.subtasks.remove` Remove · `tasks.subtasks.form.save` Save · `tasks.subtasks.form.cancel` Cancel · `tasks.subtasks.form.saveFailed` Something went wrong. Please try again.
- **Deletes (6):** `tasks.delete.title` Delete task · `tasks.delete.warning` This task and all of its subtasks will be removed permanently. · `tasks.delete.confirm` Delete · `tasks.delete.cancel` Cancel · `tasks.subtasks.delete.title` Delete subtask · `tasks.subtasks.delete.warning` This action cannot be undone.

Server `task.*` messages are shown verbatim, never cataloged client-side. Null date cells render the
literal glyph `—` (not a catalog entry). Column labels double as the inline form's input
`aria-label`s; the checkbox accessible name composes the Done label + subtask name.

## MSW additions (`test/msw/handlers.ts`, append-only)

Task fixtures + factories with parent-returning semantics in their names: `tasksListSuccess`,
`taskGetSuccess`, `taskCreateSuccess`, `taskUpdateSuccess`, `taskDeleteSuccess`,
`subtaskAddReturningParent`, `subtaskPutReturningParent`, `subtaskDeleteReturningParent`,
`taskNotFound`, `taskValidationFailed(violations)` — wildcard-prefixed paths, request-recording
callbacks, per the existing idiom.
