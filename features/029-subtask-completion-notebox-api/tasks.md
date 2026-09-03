# Tasks — A subtask records when it was completed

**ID:** features/029-subtask-completion-notebox-api
**Plan:** `plan.md` (Approved, human approval 2026-09-03)
**Status:** Approved (human approval 2026-09-03) · **Date:** 2026-09-03

Seven tasks. Ordered by dependency, then by risk: **T-01 and T-02 carry every one-way decision and
every way the plan could be wrong**, so they go first while changing the plan is still cheap.

---

- [x] **T-01 · Add the completion column**
      - files: `src/main/resources/db/migration/V9__subtask_completed_at.sql`
      - covers: FR-20 · enables every scenario; asserts none on its own
      - depends: —
      - parallel: no — everything else waits on the schema
      - verify: `mvn -B verify`
      - **Why first.** `%test` runs Hibernate with `database.generation=validate`, so the mapping and
        the migration must agree or the whole suite refuses to boot. Landing the column alone proves
        the migration is well-formed against a real MySQL before any Java depends on it.
      - The comment block is the one in `data-model.md`, verbatim: why before what, and the two
        explicit refusals (no CHECK, following V6; no backfill).

- [x] **T-02 · The transition records and erases the moment**
      - files: `domain/Subtask.java`, `application/task/TaskService.java`,
        `src/test/java/com/notebox/api/domain/SubtaskTest.java` *(new)*
      - covers: FR-20, BR-06 (frozen) · scenarios: *"Completing a subtask records the moment"*,
        *"A subtask created already done records the moment"*, *"Un-completing a subtask erases the
        moment"*, *"Completing again records a moment, never a stale one"*, *"An update that leaves done
        unchanged leaves the moment unchanged"*, *"Completing does not move the planned dates"*
      - depends: T-01
      - parallel: no
      - verify: `mvn -B verify`
      - **Three files, one outcome, and they are inseparable**: deleting `setDone` breaks
        `TaskService`'s compilation, so the identifier change rides along by necessity, not by choice.
        `TaskService`'s diff is that one identifier — if anything else in that file moves, the plan was
        wrong and this is where it shows.
      - `SubtaskTest` is plain JUnit — no container, no Docker. Six of the twelve scenarios land here.
      - **Binding on the author:** idempotence asserts **`assertSame`**, never `assertEquals`.
        `Instant.now()` allocates a fresh object per call, so object identity catches a re-stamp even
        when two instants are value-equal at the stored resolution; `assertEquals` there can pass
        vacuously.

- [x] **T-03 · Expose the moment in the read model**
      - files: `api/dto/SubtaskDto.java`, `src/test/java/com/notebox/api/api/TaskResourceTest.java`
      - covers: FR-20 · scenario: *"The read model carries what a lateness comparison needs"*
      - depends: T-02
      - parallel: no — three tasks touch `TaskResourceTest`; they run in sequence
      - verify: `mvn -B verify -Dtest=TaskResourceTest`
      - **Binding on the author:** any assertion of the form *"reports the same moment it reported
        before"* compares **a read against a read**, never a mutation response against a later read.
        `Instant.now()` carries nanoseconds; the stored column holds microseconds, and the response
        inside the write transaction is built from the managed entity — so the two differ in the last
        digits and the test flakes.

- [x] **T-04 · Reject a client-supplied moment, in both locales**
      - files: `api/dto/SubtaskInput.java`, `resources/messages.properties`,
        `resources/messages_pt.properties`,
        `src/test/java/com/notebox/api/infrastructure/i18n/TaskMessageCoverageTest.java`,
        `src/test/java/com/notebox/api/api/TaskResourceTest.java`
      - covers: FR-20, C-09 · scenarios: *"The completion moment is not client-writable"*,
        *"The rejection resolves in the caller's locale"*
      - depends: T-03
      - parallel: no — same `TaskResourceTest`
      - verify: `mvn -B verify -Dtest=TaskResourceTest+TaskMessageCoverageTest`
      - The key goes on `TaskMessageCoverageTest`'s hand-maintained list as well as in both catalogs.
        That list **enumerates keys rather than scanning code**, so a key added to the catalogs but not
        to the list is silently uncovered — the coverage test would pass while proving nothing about it.

- [x] **T-05 · The transition through the real aggregate**
      - files: `src/test/java/com/notebox/api/application/task/TaskServiceTest.java`
      - covers: FR-20, BR-06, BR-07 · scenarios: *"Completing a subtask records the moment"* (50%),
        *"A subtask created already done records the moment"* (100%), *"Rescheduling a completed subtask
        keeps its moment"*
      - depends: T-02
      - parallel: **yes** — its own file, touched by no other task
      - verify: `mvn -B verify -Dtest=TaskServiceTest`
      - Tests only; T-02 already shipped the behaviour. This is where the **BR-06 freeze** is evidenced
        end to end: the percentage still counts `done`, never the moment.

- [ ] **T-06 · A pre-FR-20 row keeps reporting nothing**
      - files: `src/test/java/com/notebox/api/infrastructure/persistence/TaskRepositoryTest.java`
      - covers: FR-20 · scenario: *"A subtask completed before this feature existed reports no moment"*
      - depends: T-03
      - parallel: **yes** — its own file
      - verify: `mvn -B verify -Dtest=TaskRepositoryTest`
      - The row must be **manufactured with a direct `EntityManager` statement** — `done = 1,
        completed_at = NULL` — the way `GroupSchemaTest` already reaches around the API. That this is
        necessary is itself the proof the invariant holds: after T-02 the public API cannot produce that
        state. The test then updates the subtask leaving it done, and asserts the moment is **still**
        null — the no-backfill rule, which is the one-way decision whose window closes at deploy.

- [ ] **T-07 · Security evidence on the path this feature touches**
      - files: `src/test/java/com/notebox/api/api/TaskResourceTest.java`
      - covers: C-01, C-02 · scenario: *"A subtask of another tenant cannot be completed"*
      - depends: T-04
      - parallel: no — same `TaskResourceTest`
      - verify: `mvn -B verify -Dtest=TaskResourceTest`
      - Extends the existing foreign-tenant sweep with the completion assertion, and closes the C-02 gap
        the spec named rather than glossed: the existing unauthenticated assertion issues only a task
        listing, so the 401 half is currently unevidenced on the subtask update path.

---

## Dependency chain

```
T-01 ──> T-02 ──┬──> T-03 ──┬──> T-04 ──> T-07
                │           └──> T-06  (parallel)
                └──> T-05              (parallel)
```

**Parallelisable:** T-05 and T-06 only. Each owns a test file no other task opens.
**Strictly sequential:** T-03 → T-04 → T-07 all edit `TaskResourceTest`; running them in parallel
worktrees would conflict on merge.

## Scenario coverage

All twelve scenarios are claimed by at least one task:

| Scenario | Task |
|---|---|
| Completing a subtask records the moment | T-02, T-05 |
| A subtask created already done records the moment | T-02, T-05 |
| Un-completing a subtask erases the moment | T-02 |
| Completing again records a moment, never a stale one | T-02 |
| An update that leaves done unchanged leaves the moment unchanged | T-02 |
| Rescheduling a completed subtask keeps its moment | T-05 |
| Completing does not move the planned dates | T-02 |
| The read model carries what a lateness comparison needs | T-03 |
| A subtask completed before this feature existed reports no moment | T-06 |
| The completion moment is not client-writable | T-04 |
| The rejection resolves in the caller's locale | T-04 |
| A subtask of another tenant cannot be completed | T-07 |

**Nothing is left uncovered — and one thing is deliberately uncoverable.** No task can assert that the
recorded moment is the *right* one: every scenario asserts presence, and an implementation storing
`getCreatedAt()` or `Instant.EPOCH` would pass all seven tasks. That is **OQ-39**, open and non-blocking;
the live pass in feat-030 is the only check available.
