# Contract — the completion helpers

```ts
/** What ticking Done on this subtask requires. `today` is passed in; the helpers never read a clock. */
export type CompletionPlan =
  | { kind: 'ask' }                        // no start date — the member must be asked first
  | { kind: 'fill'; endDate: string }      // start present, end missing — fill with today, no question
  | { kind: 'send' };                      // both present — only the flag changes

export function completionPlan(subtask: SubtaskDto, today: string): CompletionPlan;

/**
 * True only when done, a moment was recorded, an end date was planned, and the moment's LOCAL
 * calendar date is after that end date. Every other combination is false — including a done subtask
 * with no recorded moment, which is every subtask that existed before FR-20.
 */
export function isLateCompletion(subtask: SubtaskDto): boolean;

/** Today as yyyy-MM-dd in the MEMBER's timezone. Never via toISOString(). */
export function todayIso(now?: Date): string;
```

**Invariants**

1. `completionPlan` is consulted only when ticking Done ON. Un-ticking is always a plain send.
2. `isLateCompletion` requires all four conditions; no default, no fallback, no invented moment.
3. On `{ kind: 'ask' }` + confirm, **both** start and end become `today` — the product owner's rule.
4. On cancel, nothing is sent. The controlled checkbox re-renders unticked on its own.
