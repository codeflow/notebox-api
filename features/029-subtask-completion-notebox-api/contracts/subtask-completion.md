# Contract — subtask completion moment

**Feature:** features/029-subtask-completion-notebox-api · **Date:** 2026-09-03

The artifacts `implement` codes against and `audit` checks against. Signatures only — no bodies beyond
the one method whose body *is* the design.

## Domain

```java
// domain/Subtask.java

/** When this subtask was marked done (FR-20); null when it is not done, and null for a subtask
 *  completed before this was recorded. */
@Column(name = "completed_at")
private Instant completedAt;

/**
 * Marks this subtask done or not done, keeping the completion moment consistent with the flag
 * (FR-20): a false→true transition records the moment, a true→false transition erases it, and a
 * call that does not change the flag leaves the moment exactly as it was — which is also why a
 * subtask completed before FR-20 existed is never back-filled by a later update.
 *
 * <p>Replaces {@code setDone}. The plain setter is gone on purpose: it is the only guard this
 * codebase can offer, since there is no ArchUnit rule and no build gate that would catch a second
 * writer of the flag.
 *
 * @param done the intended done state
 */
public void markDone(boolean done) {
    if (done == this.done) {
        return;
    }
    this.done = done;
    this.completedAt = done ? Instant.now() : null;
}

/**
 * The moment this subtask was marked done.
 *
 * @return the completion moment, or null when it is not done or was completed before FR-20
 */
public Instant getCompletedAt();
```

**The constructor delegates** so birth-as-done travels the same transition, keeping one writer:

```java
public Subtask(String name, LocalDate startDate, LocalDate endDate, boolean done) {
    this.name = name;
    this.startDate = startDate;
    this.endDate = endDate;
    markDone(done);   // NOT `this.done = done` — one expression assigns completedAt, repo-wide
}
```

> **Constraint on any later refactor:** `markDone` must not become overridable while it is called from a
> constructor, and no constructor may take `completedAt`. Both would reopen the door this design closes.

## Application

```java
// application/task/TaskService.java — updateSubtask, ONE identifier changes
- subtask.setDone(input.doneOrFalse());
+ subtask.markDone(input.doneOrFalse());
```

`addSubtask` is **not modified**. Nothing else in the file moves: the `wasDone` capture, both
`events.fire` branches and the `rescheduled` computation stay byte-identical.

## Wire — input

```java
// api/dto/SubtaskInput.java
@DateRangeValid
public record SubtaskInput(
        @NotBlank(message = "task.subtask.name.required")
                @Size(max = 120, message = "task.subtask.name.too_long")
                String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean done,
        @Null(message = "task.subtask.completed_at.not_writable") Instant completedAt,
        @Valid CardInput card) { }
```

Rejection shape, matching the existing poison fields:

```
HTTP 400
{ "code": "validation.failed",
  "violations": [ { "code": "task.subtask.completed_at.not_writable", "message": "<localized>" } ] }
```

## Wire — output

```java
// api/dto/SubtaskDto.java
public record SubtaskDto(
        UUID id, String name, LocalDate startDate, LocalDate endDate,
        boolean done, CardDto card,
        Instant completedAt,          // NEW — null when not done, or completed before FR-20
        Instant createdAt, Instant updatedAt) { }
```

Additive and optional. A client that ignores it is unaffected; `notebox-web` begins reading it in
feat-030.

## Messages

| Key | en | pt |
|---|---|---|
| `task.subtask.completed_at.not_writable` | `The subtask completion time is recorded by the server and cannot be set directly.` | `O momento de conclusão da subtarefa é registrado pelo servidor e não pode ser definido diretamente.` |

Both catalogs, **and** the entry on the message-coverage test's hand-maintained key list — it enumerates
keys rather than scanning code, so an omission there is silently uncovered.

## Not in this contract

- No repository signature changes — `Subtask` has no repository; it is reached through `Task`.
- No event payload changes — `SubtaskCompleted` / `SubtaskUncompleted` keep carrying only `taskId`.
- No derived "late" flag anywhere. The client compares `completedAt` with `endDate`, both already in this
  payload (spec, Out of scope).
