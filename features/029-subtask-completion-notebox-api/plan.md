# Plan — A subtask records when it was completed

**ID:** features/029-subtask-completion-notebox-api
**Spec:** `spec.md` (Approved, human approval 2026-09-03)
**Version:** v1
**Status:** Approved (human approval 2026-09-03)
**Date:** 2026-09-03

## Origin

- **Spec:** `features/029-subtask-completion-notebox-api/spec.md` — 12 scenarios, gate passed 2026-09-03.
- **User Story:** US-4.1 · **FR:** FR-20 (new, from OQ-38) · **BRs:** BR-06 and BR-07 frozen, BR-05
  considered and out of scope, BR-01/BR-02 standing.

## Approach

**The completion moment is an invariant of the subtask, so the subtask owns it.** `Subtask` gains one
field and `setDone(boolean)` is replaced by `markDone(boolean)`, which is the only expression in the
repository that assigns it:

```java
public void markDone(boolean done) {
    if (done == this.done) {
        return;          // no transition — the moment is left exactly as it was
    }
    this.done = done;
    this.completedAt = done ? Instant.now() : null;
}
```

The four-argument constructor **delegates to it** rather than assigning `done` directly, so a subtask
born done travels the same transition as any other completion. `done` is a primitive defaulting to
`false`, so `markDone(true)` at construction is a genuine false→true transition and `markDone(false)` is
a no-op.

Three properties fall out of that shape rather than needing to be enforced:

- **Idempotence is the guard clause.** An update that restates the same `done` returns before touching
  anything, which is simultaneously the *"done unchanged leaves the moment unchanged"* scenario and the
  *"a pre-FR-20 row is never back-filled"* one. They are the same code path, so they cannot diverge.
- **Staleness is impossible.** Un-completing writes `null`; re-completing writes a fresh instant. A stale
  value cannot survive an intervening null, which is why the spec's re-completion scenario asserts
  presence and needs no ordering comparison.
- **The event-payload problem disappears.** The write happens where the subtask identity is already in
  hand, so no observer ever needs to know which subtask completed. `SubtaskCompleted`,
  `SubtaskUncompleted`, `SubtaskAdded`, `SubtaskChange` and both recalculators are **untouched**.

The application layer's diff is **one identifier**: `subtask.setDone(...)` → `subtask.markDone(...)` in
`TaskService.updateSubtask`. The surrounding `wasDone` capture and both `events.fire` branches stay
byte-identical, and `addSubtask` is not touched at all — the constructor now records the moment.

**Why the rename and not just a new body on `setDone`.** It is the only guard available. There is no
ArchUnit dependency and no architecture test anywhere in `src/test` — verified, not assumed — and the
constitution's forbidden-list is a human checklist read at `audit`, not a build gate. A lint rule would
also be unreliable here: this project has a recorded trap where `grep --include=*.ext` silently matches
nothing in this shell, so a grep-based guard can false-green. Deleting the plain setter makes the guard a
**compile error**: after the rename there is no way to write the flag without settling the moment, so a
future bulk-complete endpoint cannot quietly reintroduce the defect. The cost is one line — `setDone(`
has exactly two occurrences in the whole repository, the declaration and that single call site, and **no
test calls it**.

**Input rejection.** `SubtaskInput` gains a poison field in the established shape — Jakarta `@Null` with
a message key, exactly as `TaskInput` rejects `status` and the task dates. New key
`task.subtask.completed_at.not_writable`, with en and pt values in the bundled catalogs.

**Read model.** `SubtaskDto` gains `completedAt` beside the existing `createdAt`/`updatedAt`. The
comparison against the planned end date stays with the client, per the spec's Out of scope.

## Alternatives rejected

**1 · Write the moment in the application service, where the transition is already detected.**
`TaskService.updateSubtask` already computes `wasDone`, so the branch that fires `SubtaskCompleted` could
also stamp the field. Rejected because the rule would then live in **two call sites** — that branch and
`addSubtask` — with nothing preventing a third. The panel's own advocate for this angle could name no
guard available in this codebase and admitted it rests on discipline. A stronger variant (delete
`setDone` *and* drop the boolean from the constructor, forcing every caller through an explicit
`recordCompletion`) closes two doors instead of one, but costs **30 `new Subtask(...)` call sites** —
verified by grep — and 22 of them sit in the very tests that constitute the evidence that BR-06/BR-07 are
frozen. Churning the freeze tests to add a fact the spec says is *not* an input to derivation is the
wrong place to spend a diff.

**2 · Observe the existing CDI events (AD-10).** Superficially the best fit: `SubtaskCompleted` and
`SubtaskUncompleted` already fire at exactly the two transitions. Rejected on the constitution's own
terms and on a hard fact. The fact: both events carry **only `taskId`**, so an observer cannot identify
which subtask completed without widening a payload that two shipped recalculators consume. The terms:
AD-10 scopes events to *decoupling otherwise-unrelated components* — a same-aggregate observer re-fetching
the entity the producer is already holding is not that — and its two existing occupants recompute
**derived, idempotent** state, whereas a completion moment is non-derivable and write-once. It also does
not reach the add path, which fires `SubtaskAdded` and no completion event at all.

**3 · A `CHECK (completed_at IS NULL OR done = 1)` constraint.** Rejected on precedent, which the source
hierarchy ranks above a better new pattern: no migration V1–V8 declares a CHECK, and V6 explicitly
declines one in a comment — *"No CHECK: the API guarantees url never appears without code."* It would
also constrain the wrong direction. The failure mode worth guarding is `done = 1` with **no** moment; that
one is unconstrainable **forever**, because pre-FR-20 rows legitimately have exactly that shape. A
constraint that cannot catch the failure it is being bought for is not worth the schema deviation or the
opaque 500 a breach would produce.

**4 · A `Clock` seam.** Rejected as out of proportion, and recorded as the honest cost. It would make one
domain assertion sharper — a pinned instant instead of object identity — but `Instant.now()` would still
be chosen at the application layer, so the wire and service seams stay exactly as unpinnable. Nothing in
this codebase has a clock: ten entities call `Instant.now()` directly. Introducing one for this field
would either be inconsistent or become a cross-cutting refactor this feature has no mandate for. **If it
is ever needed the migration is two call sites** — `markDone(boolean, Instant)` with the caller supplying
the moment — with no schema change and no obligation to touch the other entities.

## Reversibility

| Decision | | Why |
|---|---|---|
| `markDone` replaces `setDone` | **reversible** | One production call site; restoring the setter is a rename back. |
| The moment is a stored field on the subtask | **one-way** | It is a schema addition with a migration checksum. Dropping it later loses every recorded moment; nothing else holds them. |
| The wire field on the read model | **one-way** | A shipped contract. `notebox-web` starts depending on it in feat-030; removing it breaks that client. |
| Not back-filling pre-existing done rows | **one-way, and the window closes at deploy, not at merge** | Today a `done=1, completed_at NULL` row is unambiguously "completed before FR-20". The moment real values start accumulating, a NULL becomes indistinguishable from a bug that failed to write one. If backfill is ever wanted it must be decided *before* this ships. |
| The rejection message key | **reversible** | A catalog key with no consumer but this validation. |
| No CHECK constraint | **reversible** | Additive later in its own migration; no row would violate it. |
| No clock seam | **reversible** | Two call sites, no schema change (see rejected alternative 4). |

## Blast radius

**Production, 6 files.**

| File | Change |
|---|---|
| `domain/Subtask.java` | one field, `setDone` → `markDone` with the transition guard, constructor delegates, `getCompletedAt()` |
| `application/task/TaskService.java` | **one identifier** — `setDone` → `markDone`. No other line moves. |
| `api/dto/SubtaskInput.java` | poison field `@Null Instant completedAt` |
| `api/dto/SubtaskDto.java` | `completedAt` in the record and in `from(...)` |
| `resources/messages.properties` · `messages_pt.properties` | one key each |
| `resources/db/migration/V9__subtask_completed_at.sql` | new file |

**Untouched, and that is load-bearing evidence.** `SubtaskCompleted`, `SubtaskUncompleted`,
`SubtaskAdded`, `SubtaskChange`, `TaskProgressRecalculator`, `TaskDatesRecalculator`, `TaskRepository`,
`TaskResource`, `Task`, `TaskInput`, `TaskDto`. **`TaskService` has no logic diff at all**, so the entire
existing service suite becomes an unmodified regression guard on the frozen BR-06/BR-07 — an unchanged
file cannot have acquired a new input to the percentage.

**Consumers.** `notebox-web` reads `SubtaskDto`. The new field is additive and optional; the current
client ignores unknown fields, so nothing breaks before feat-030 lands. **No client can write the field**,
so the feat-012/feat-014 PUT-replace clearing hazard does not repeat here.

**Tests.** No existing test is rewritten. `setDone` is called by no test; the 4-argument constructor is
preserved, so all 30 `new Subtask(...)` sites compile unchanged. Everything added is new.

## Risk

| Risk | Signal that reveals it |
|---|---|
| **A wrong instant passes every scenario.** No test at any seam can distinguish `Instant.now()` from `getCreatedAt()` or `Instant.EPOCH` — all twelve scenarios assert presence, and a wrong-but-present value satisfies them. This is the feature's entire product value and it is not test-covered. | Nothing automated. Recorded as **OQ-39**; the live pass in feat-030 is the only check, where a moment equal to creation time would show as a late verdict on work just completed. |
| **Precision flake.** `Instant.now()` carries nanoseconds on modern JDKs; the stored column is microsecond-resolution. A mutation response is built from the managed entity and carries the in-JVM value; a later read returns the truncated one. | The three *"reports the same moment it reported before"* scenarios would flake. **Mitigation, binding on `tasks`:** those scenarios compare a read against a read, never a mutation response against a later read. |
| **The suite refuses to boot** if the entity mapping and the migration disagree — `%test` runs Hibernate with `database.generation=validate`. | Total suite failure, not a subtle one. It fails loudly and early, which is why it is a low risk. |
| **A future third writer of the done flag** bypasses the invariant via JPQL or a native bulk update. | Not catchable in Java, and deliberately not constrained in SQL (rejected alternative 3). The compile-time guard covers every path that goes through the domain API, which is every path that exists. |
| **Time zone.** Nothing in the codebase sets a JDBC time zone, and the stored type carries none, so the `Instant` round-trip rides the JVM default. Pre-existing and equally true of every `created_at`, but this is the first feature whose whole value is the correctness of a moment. | Recorded as **OQ-40**; a deployment whose JVM zone differs from the DB's would show a systematic offset. |

## Test seams

Four existing layers, no new kind of test:

- **domain unit** — new `SubtaskTest`, plain JUnit, no container, no Docker. Seven of the twelve
  scenarios land here. Idempotence asserts **`assertSame`**, not `assertEquals`: `Instant.now()`
  allocates a fresh object each call, so object identity catches a re-stamp even when two instants are
  value-equal at the stored resolution. `assertEquals` there can pass vacuously.
- **service** — `TaskServiceTest`, for the transition through the real aggregate and the frozen BR-06.
- **wire** — `TaskResourceTest`, for the read model, the rejection, its Portuguese resolution, and the
  cross-tenant not-found.
- **persistence** — for the one state the public API can no longer produce: a legacy `done=1,
  completed_at NULL` row. It must be **manufactured with a direct `EntityManager` statement**, the way
  `GroupSchemaTest` already reaches around the API. That this is necessary is itself the proof the
  invariant holds.
