# Data model — Tasks & subtasks UI (feat-011, client-side)

**ID:** features/011-tasks-notebox-web · **US:** US-4.1 · **Date:** 2026-08-14
No persistence here — the client's "data model" is the wire mirrors, the two structural guards, and
the view state. Wire truth: `features/010-tasks-notebox-api/contracts/tasks.md`.

## Wire mirrors — append to `lib/api/types.ts`

```ts
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/** Create/update body. `status` is @Null-poisoned on the wire; `never` makes sending it a COMPILE error (BR-06). */
export interface TaskInput { name: string; priority: TaskPriority | null; status?: never }

/** PUT-replace body: every field required — an omitted field would CLEAR server-side, so omission is unconstructable. */
export interface SubtaskInput { name: string; startDate: string | null; endDate: string | null; done: boolean }

export interface SubtaskDto {
  id: string; name: string; startDate: string | null; endDate: string | null;
  done: boolean; createdAt: string; updatedAt: string;
}
export interface TaskDto {
  id: string; name: string; priority: TaskPriority; status: number;   // server's integer 0..100, display-only
  createdAt: string; updatedAt: string; subtasks: SubtaskDto[];
}
export interface TaskListItemDto {
  id: string; name: string; priority: TaskPriority; status: number;
  createdAt: string; updatedAt: string;                               // scalars only — no subtasks/count on rows
}
// PageDto<T>, Problem, Violation — reused as-is.
```

## View-model state (`lib/tasks/viewModel.ts` + component-local)

```ts
// Form drafts ('' = deliberately unset; only the converters touch the wire)
export interface TaskDraft { name: string; priority: TaskPriority | '' }
export interface SubtaskDraft { name: string; startDate: string; endDate: string } // type=date values; '' = unset

// Error-routing targets (RecordErrors idiom; values are the server's verbatim localized messages)
export interface TaskFormErrors { name: string | null; priority: string | null; form: string | null }
export interface SubtaskFormErrors { name: string | null; dates: string | null; form: string | null }
```

- Detail/edit pages: `type LoadState = { status: 'loading' } | { status: 'ready'; task: TaskDto } | { status: 'notFound' }` (local per page, repo idiom).
- `TasksTable`: `page`, `size` (`PAGE_SIZES = [50, 100, 200]`), `result: PageDto<TaskListItemDto> | null`; pager text ONLY from `result` facts; rows never re-sorted.
- `SubtasksPanel`: `editor: { kind: 'add'; draft } | { kind: 'edit'; subtaskId; draft } | null` (one at a time), `errors: SubtaskFormErrors`, `busy: boolean` (disables checkboxes/actions during any in-flight mutation), `confirmingDelete: SubtaskDto | null`.
- Detail page: `LoadState` + `confirmingDelete: boolean`; every `onTaskUpdated(dto)` replaces the `ready` state wholesale.

## Invariants

1. **BR-06 (client obligation):** no client code computes, edits or serializes a status — `status?: never` at compile time, the no-status-key serialization tests at run time, and every rendered % comes verbatim from a server DTO.
2. **PUT-replace safety:** a subtask body is only ever built by `toSubtaskInput` (full echo + overrides) or `draftToSubtaskInput` (explicit `done` parameter) — the required-field `SubtaskInput` makes a partial body a compile error.
3. **Repaint source:** after any subtask mutation, the detail's `task` state is exactly the API's returned parent `TaskDto` — never a locally patched copy.
4. **Order:** the list renders rows in served order; nothing on the client sorts.
