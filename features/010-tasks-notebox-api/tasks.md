# Tasks — Tasks with subtasks and derived progress

**ID:** features/010-tasks-notebox-api · **US:** US-4.1 · **Date:** 2026-08-13
**Source:** plan.md + contracts/tasks.md + data-model.md (approved 2026-08-13)
**Status:** Approved (human approval 2026-08-13)

> Strictly sequential — each task builds the layer the next one consumes (AD-01 inward
> dependencies). No `parallel: yes` anywhere → no worktree isolation. Every task ships its tests;
> `mvn -B verify` (the harness command) must be green at the end of each task.

- [x] **T-01 · Task aggregate persists — entities, BR-06 math, V5 migration** ✔ 2026-08-13, verify green (170 tests)
      - files: `domain/Task.java`, `domain/Subtask.java`, `domain/Priority.java`, `db/migration/V5__tasks.sql`, `test …/domain/TaskTest.java`
      - covers: FR-10 (shape), BR-06 + OQ-22 · scenario: "A non-integer proportion rounds to an integer percent" (pure `percentOf` table: 0/0→0, 1/3→33, 2/3→67, 1/8→13, 1/2→50, 3/3→100)
      - depends: — · parallel: no
      - verify: `mvn -B verify`

- [x] **T-02 · Tenant-scoped repository, `SubtaskChange` events, recalculator** ✔ 2026-08-13, verify green (176 tests)
      - files: `infrastructure/persistence/TaskRepository.java`, `domain/event/SubtaskChange.java` (+ 4 records), `application/task/TaskProgressRecalculator.java`, `test …/infrastructure/persistence/TaskRepositoryTest.java`
      - covers: AD-03, AD-10 (inaugural), OQ-21 · scenarios: "The listing is ordered newest first" (id tiebreak), "Listings never leak across tenants" (repo seam); first `@QuarkusTest` also proves V5 ↔ entity `hibernate validate`
      - depends: T-01 · parallel: no
      - verify: `mvn -B verify`

- [ ] **T-03 · `TaskService` use cases — audited deletes + recompute wiring**
      - files: `application/task/TaskService.java`, `domain/error/TaskNotFoundException.java`, `domain/error/SubtaskNotFoundException.java`, `test …/application/task/TaskServiceTest.java`
      - covers: FR-10/FR-11, BR-05, C-10 · scenarios: "Completing a subtask updates progress" (2nd of 4 → 50%), "Adding a subtask recomputes downward" (100→50), "Un-completing…" (100→50), "Deleting a subtask recomputes" (25→0, audited), "Deleting a task removes it and its subtasks, audited", "Update name and priority without touching progress", status-guard invariant
      - depends: T-02 · parallel: no
      - verify: `mvn -B verify`

- [ ] **T-04 · Wire contract — resource, DTOs, validators, i18n, OpenAPI coverage**
      - files: `api/TaskResource.java`, `api/dto/{TaskInput, SubtaskInput, TaskDto, TaskListItemDto, SubtaskDto}.java`, `api/validation/{ValidPriority, PriorityValidator, DateRangeValid, DateRangeValidator}.java`, `messages.properties`, `messages_pt.properties` (+11 line-parallel keys), `test …/api/TaskResourceTest.java`, `test …/infrastructure/i18n/TaskMessageCoverageTest.java`, `test …/api/OpenApiCoverageTest.java` (extend, +4 path keys)
      - covers: FR-10/FR-11 at the wire, NFR-08, C-01/C-02/C-09, NFR-06 · scenarios: all 21 wire shapes — create at 0%, blank/long names, priority "Urgent", status poison field, read-one with subtasks, pagination trio (default 50 / 201 rejected / T3-T2-T1), unauthenticated 401, foreign-tenant 404s (task and subtask), pt-locale message, date-range rejection, duplicate-name-allowed pin
      - depends: T-03 · parallel: no
      - verify: `mvn -B verify` (full suite green = the `implement` step's `verify_green` gate)

## Coverage check

Every one of the spec's 21 scenarios is cited by exactly one primary task (T-03 service-level, T-04
wire-level assertions of the same behaviors are complementary, not gaps). No task cites zero
scenarios; no scenario is uncovered.
