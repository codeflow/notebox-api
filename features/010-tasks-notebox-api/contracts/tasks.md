# Contract — Tasks API (feat-010, US-4.1)

**Consumers:** feat-011-tasks-notebox-web (primary), OpenAPI at `/q/openapi` (derived — NFR-06).
**Global prefix:** `quarkus.http.root-path=/api` applies to every path below.
**Auth:** every endpoint `@Authenticated`; tenant comes from the verified JWT (`TenantContext`).
**Errors:** the uniform envelopes — `Problem(code, message, correlationId)` for domain errors,
`ValidationProblem(code="validation.failed", …, violations[])` for Bean Validation — both localized
(en/pt) via the message catalog. Foreign-tenant access is indistinguishable from missing (404,
never 403).

## Endpoints

| Method | Path | Purpose | Success | Notes |
|---|---|---|---|---|
| POST | `/tasks` | create task | `201` + `TaskDto` | status always `0` (no subtasks yet) |
| GET | `/tasks?page={0+}&size={1..200}` | paginated listing, newest first | `200` + `PageDto<TaskListItemDto>` | defaults `page=0`, `size=50`; order `createdAt DESC, id DESC` (OQ-21); "more exist" signaled by `total` |
| GET | `/tasks/{id}` | read one, with subtasks | `200` + `TaskDto` | |
| PUT | `/tasks/{id}` | update name/priority | `200` + `TaskDto` | never touches status |
| DELETE | `/tasks/{id}` | delete task **and its subtasks** | `204` | audited `TASK_DELETED` (BR-05, C-10) |
| POST | `/tasks/{id}/subtasks` | add subtask | `201` + **parent `TaskDto`** | fires `SubtaskAdded` → recompute |
| PUT | `/tasks/{id}/subtasks/{subtaskId}` | update subtask (name/dates/done) | `200` + **parent `TaskDto`** | done flip fires `SubtaskCompleted` / `SubtaskUncompleted` |
| DELETE | `/tasks/{id}/subtasks/{subtaskId}` | delete subtask | `200` + **parent `TaskDto`** | audited `SUBTASK_DELETED` + `SubtaskRemoved` → recompute |

Subtask mutations return the **parent task** because every FR-11 flow needs the recomputed status
immediately; task `DELETE` returns `204` (nothing meaningful remains).

## DTOs (`api/dto/`, Java records)

```java
// inputs
record TaskInput(
    @NotBlank(message = "task.name.required")
        @Size(max = 120, message = "task.name.too_long") String name,
    @NotNull(message = "task.priority.required")
        @ValidPriority(message = "task.priority.invalid") String priority,   // wire = enum name "LOW".."CRITICAL"
    @Null(message = "task.status.not_writable") Integer status)              // poison field — any supplied value is rejected

@DateRangeValid(message = "task.subtask.date.invalid")                       // class-level: start ≤ end when both present
record SubtaskInput(
    @NotBlank(message = "task.subtask.name.required")
        @Size(max = 120, message = "task.subtask.name.too_long") String name,
    LocalDate startDate, LocalDate endDate,
    Boolean done) { boolean doneOrFalse(); }                                 // null → false (PUT-replace semantics)

// outputs (static from(...) factories)
record TaskDto(UUID id, String name, String priority, int status,
               Instant createdAt, Instant updatedAt, List<SubtaskDto> subtasks)
record TaskListItemDto(UUID id, String name, String priority, int status,
               Instant createdAt, Instant updatedAt)                         // listing rows: scalars only, no subtasks
record SubtaskDto(UUID id, String name, LocalDate startDate, LocalDate endDate, boolean done,
               Instant createdAt, Instant updatedAt)
// PageDto<T>(items, page, size, total) — reused unchanged from feat-008
```

`SubtaskInput` deliberately carries **no** status poison field: a subtask has no derived value —
`done` *is* its writable completion control. Unknown JSON properties on any input remain
Jackson-ignored (tolerant reader), exactly as on every shipped endpoint; only `TaskInput.status` is
explicitly poisoned because the spec demands rejection.

**Documented edges:** an explicit `"status": null` passes `@Null` (indistinguishable from absent —
harmless); a non-numeric `status` or malformed date fails at Jackson binding with a non-catalog 400
(outside the spec's scenarios). The generated OpenAPI schema shows `status` on `TaskInput` — it
exists only to reject writes; clients must never send it.

## Query params (feat-008 idiom)

```java
@QueryParam("page") @DefaultValue("0")
    @Min(value = 0,   message = "task.list.size.out_of_bounds") int page
@QueryParam("size") @DefaultValue("50")
    @Min(value = 1,   message = "task.list.size.out_of_bounds")
    @Max(value = 200, message = "task.list.size.out_of_bounds") int size
```

## Validation / error matrix

| Condition | Mechanism | HTTP | Key |
|---|---|---|---|
| blank/missing task name | `@NotBlank` | 400 | `task.name.required` |
| task name > 120 | `@Size` | 400 | `task.name.too_long` |
| priority missing | `@NotNull` | 400 | `task.priority.required` |
| priority ∉ {LOW, MEDIUM, HIGH, CRITICAL} | `@ValidPriority` | 400 | `task.priority.invalid` |
| any `status` supplied on create/update | `@Null` | 400 | `task.status.not_writable` |
| blank subtask name | `@NotBlank` | 400 | `task.subtask.name.required` |
| subtask name > 120 | `@Size` | 400 | `task.subtask.name.too_long` |
| start > end (both present) | `@DateRangeValid` | 400 | `task.subtask.date.invalid` |
| page < 0, size < 1 or size > 200 | `@Min` / `@Max` | 400 | `task.list.size.out_of_bounds` |
| task missing or foreign-tenant | `TaskNotFoundException` (NOT_FOUND) | 404 | `task.not_found` |
| subtask missing, foreign, or of another task | `SubtaskNotFoundException` (NOT_FOUND) | 404 | `task.subtask.not_found` |
| no/invalid token | existing auth filters | 401 | existing keys |

New constraint pairs in `api/validation/`: `@ValidPriority` + `PriorityValidator`
(`ConstraintValidator<ValidPriority, String>` — passes `null`, rejects non-members) and
`@DateRangeValid` + `DateRangeValidator` (`ConstraintValidator<DateRangeValid, SubtaskInput>` —
valid when either date is null or `!start.isAfter(end)`).

## Message keys (11 — en shown; pt line-parallel, translated at implement)

```
task.name.required=The task name is required.
task.name.too_long=The task name must be at most 120 characters.
task.priority.required=The task priority is required.
task.priority.invalid=The priority must be one of Low, Medium, High or Critical.
task.status.not_writable=The task status is derived from its subtasks and cannot be set directly.
task.not_found=The task was not found.
task.subtask.name.required=The subtask name is required.
task.subtask.name.too_long=The subtask name must be at most 120 characters.
task.subtask.date.invalid=The start date must not be after the end date.
task.subtask.not_found=The subtask was not found.
task.list.size.out_of_bounds=The page size must be between 1 and 200
```

## Application-layer signatures

```java
@ApplicationScoped
class TaskService {          // ctor: (TaskRepository, AuditLogRepository, Event<SubtaskChange>, TenantContext)
  // constants: TARGET_TASK="TASK", TARGET_SUBTASK="SUBTASK",
  //            ACTION_TASK_DELETED="TASK_DELETED", ACTION_SUBTASK_DELETED="SUBTASK_DELETED"
  Task create(TaskInput input);                                   // @Transactional
  Task get(UUID id);
  List<Task> list(int page, int size);
  long count();
  Task update(UUID id, TaskInput input);                          // @Transactional
  void delete(UUID id);                                           // @Transactional; audit detail "subtasks=" + n
  Task addSubtask(UUID taskId, SubtaskInput input);               // @Transactional; fires SubtaskAdded
  Task updateSubtask(UUID taskId, UUID subtaskId, SubtaskInput input); // @Transactional; done flip fires events
  Task removeSubtask(UUID taskId, UUID subtaskId);                // @Transactional; audit target=subtaskId, detail "taskId="+taskId
}

@ApplicationScoped
class TaskProgressRecalculator {  // ctor: (TaskRepository)
  void onSubtaskChange(@Observes SubtaskChange change);           // IN_PROGRESS phase — same TX, deliberately NOT AFTER_SUCCESS
}

@ApplicationScoped
class TaskRepository extends TenantScopedRepository<Task> {
  void remove(Task task);
  List<Task> listNewestFirstInTenant(int page, int size);         // JPQL: where e.tenantId = :tenant order by e.createdAt desc, e.id desc
  long countInTenant();
}
```

**Event contract (AD-10, inaugurated here):** producers fire after mutating the aggregate, in the
same transaction; the observer re-fetches via the choke point and calls `Task.recomputeStatus()`.
US-4.2's date derivation (BR-07) will observe the same `SubtaskChange` seam.
