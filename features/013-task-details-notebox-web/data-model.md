# Data model — Task dates, card link and rich-text details UI (notebox-web)

**ID:** features/013-task-details-notebox-web · **Companion of:** `plan.md` · **Date:** 2026-08-18
**Wire truth:** `features/012-task-details-notebox-api/contracts/task-details.md` (mirrored, never
re-derived). No persistence lives in the satellite; this is the client's data model.

## Wire mirrors — append to `lib/api/types.ts` (feat-013 block)

```ts
/** Inline card (FR-13, OQ-06): code required, url an absolute http/https URL with an authority or null. */
export interface CardInput { code: string; url: string | null; }
export interface CardDto   { code: string; url: string | null; }

/**
 * Create/update body. `status` AND the derived dates are @Null-poisoned on the wire; `never` makes
 * sending any of them a COMPILE error (BR-06, BR-07). PUT-replace: an omitted/null card or details
 * CLEARS server-side — so both keys are required, and the edit path echoes what it doesn't change.
 */
export interface TaskInput {
  name: string;
  priority: TaskPriority | null;
  card: CardInput | null;
  details: string | null;
  status?: never;
  startDate?: never;
  endDate?: never;
}

/** PUT-replace body: every field required — `card` included, so omission stays unconstructable. */
export interface SubtaskInput {
  name: string;
  startDate: string | null;
  endDate: string | null;
  done: boolean;
  card: CardInput | null;
}

export interface SubtaskDto {
  id: string; name: string;
  startDate: string | null; endDate: string | null;
  done: boolean;
  card: CardDto | null;
  createdAt: string; updatedAt: string;
}

/** Read-one / mutation-response shape. Derived dates are the server's (null = underivable); details already sanitized on read (C-08). */
export interface TaskDto {
  id: string; name: string; priority: TaskPriority; status: number;
  startDate: string | null; endDate: string | null;
  card: CardDto | null;
  details: string | null;
  createdAt: string; updatedAt: string;
  subtasks: SubtaskDto[];
}

/** Listing row: scalars only — dates + the two card scalars ride along; NO details (contract). */
export interface TaskListItemDto {
  id: string; name: string; priority: TaskPriority; status: number;
  startDate: string | null; endDate: string | null;
  card: CardDto | null;
  createdAt: string; updatedAt: string;
}
```

## View-model state (`lib/tasks/viewModel.ts` + component-local)

```ts
export interface CardDraft   { code: string; url: string; }              // '' = empty field
export interface TaskDraft   { name: string; priority: TaskPriority | ''; card: CardDraft; details: string; } // details: dialect HTML, '' = none
export interface SubtaskDraft{ name: string; startDate: string; endDate: string; card: CardDraft; }
export interface TaskFormErrors    { name; priority; cardCode; cardUrl; form: string | null; }
export interface SubtaskFormErrors { name; dates; cardCode; cardUrl; form: string | null; }
export const EMPTY_CARD_DRAFT: CardDraft; export const EMPTY_TASK_DRAFT: TaskDraft;
```

Component-local additions: `TaskDetailsTab` — `mode: 'view' | 'edit'`, `draftHtml: string`,
`error: string | null`, `saving: boolean`. Detail page — `activeTab: 'subtasks' | 'details'`
(both panels stay mounted; the inactive one is hidden, so the subtask inline editor survives a
tab switch). Everything else is feat-011's unchanged `LoadState` / `EditorState`.

## Invariants (client)

- **INV-W1 (BR-07):** no request body ever carries `startDate`/`endDate` for a task — enforced by
  the type (`never`); no date input exists for a task. Dates render only from `TaskDto`/
  `TaskListItemDto`; the client computes none.
- **INV-W2 (rollout fix, PUT-replace):** every task update body is built from `taskToDraft(task)`
  (+ overrides) and every subtask update body from `toSubtaskInput(source, overrides)` — so `card`
  and `details` are echoed verbatim unless the user changed them. A body that drops them is a
  compile error (`SubtaskInput.card` required) or a failing `onBody` test (`TaskInput`).
- **INV-W3 (contract edge — url):** `toCardInput` never emits `url: ''`; an empty URL field is
  `null`; a card is `null` iff both fields are blank; whitespace-only code with a URL is *sent* so the
  server answers `task.card.code.required` at the field.
- **INV-W4 (contract edge — details):** `toDetails` never emits `''` or `<p></p>`; an emptied editor
  is `null` (clears); create sends `details: null`.
- **INV-W5 (C-08 client share):** every rendered details value passes `sanitizeRichText` (via
  `RichTextValue`) and every edited value originates from `RichTextEditor` (dialect-only output);
  embedded images are `data-image-id` references resolved through the authenticated images
  endpoint — never a `src`.
- **INV-W6 (repaint rule):** dates, card, subtasks, progress and details repaint only from a
  returned `TaskDto` (`onTaskUpdated`) — the detail page has one task state.
- **INV-W7 (i18n):** every new string is a `MessageKey` in en and pt (keyset-parity test); the
  "Open <code>" label uses `t('tasks.card.open', { code })`.

## Display conventions

- Dates: `formatIsoDate(iso, locale)` → `dd-MMM-yyyy` (`04-Aug-2026` / `04-ago-2026`), `—` for
  null; applied to task dates and subtask cells alike.
- Card: code as link text when a URL exists (`target="_blank" rel="noopener noreferrer"`), plain
  code otherwise, `—` when none; the detail panel's link reads "Open <code>".
- Span footer: shown only when both task dates exist — `Span dd-MMM-yyyy → dd-MMM-yyyy`.
