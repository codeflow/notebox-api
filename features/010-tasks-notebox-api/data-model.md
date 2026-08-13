# Data model — Tasks with subtasks and derived progress

**ID:** features/010-tasks-notebox-api · **US:** US-4.1 · **Date:** 2026-08-13
**Invariants trace:** BR-05 (explicit, irreversible deletes), BR-06 (status derived, 0% empty, integer percent HALF_UP — OQ-22), AD-03 (tenant scoping on the root).

## Entities

### `Task` — aggregate root, tenant-owned (`implements TenantOwned`) — `domain/Task.java`

| Field | Java | Column | Notes |
|---|---|---|---|
| `id` | `UUID` | `CHAR(36)` PK | `@JdbcTypeCode(SqlTypes.CHAR)`, assigned in `@PrePersist` |
| `tenantId` | `UUID` | `CHAR(36) NOT NULL`, `updatable=false` | AD-03 scoping key |
| `name` | `String` | `VARCHAR(120) NOT NULL` | required; 120 = house baseline (OQ-09 refinement) |
| `priority` | `Priority` | `VARCHAR(10) NOT NULL` | `@Enumerated(EnumType.STRING)` |
| `status` | `int` | `INT NOT NULL` | **derived** (BR-06): constructor starts 0; only `recomputeStatus()` writes it |
| `createdAt` / `updatedAt` | `Instant` | `DATETIME(6) NOT NULL` | `@PrePersist` / `@PreUpdate`; `createdAt` `updatable=false`, backs OQ-21 ordering |
| `subtasks` | `List<Subtask>` | — | `@OneToMany(cascade = ALL, orphanRemoval = true) @JoinColumn(name = "task_id", nullable = false) @OrderBy("createdAt asc, id asc")` |

Domain surface: `Task(UUID tenantId, String name, Priority priority)` · `setName(String)` ·
`setPriority(Priority)` · `addSubtask(Subtask)` · `Optional<Subtask> subtask(UUID)` ·
`boolean removeSubtask(UUID)` · `void recomputeStatus()` ·
`static int percentOf(int doneCount, int totalCount)` — 0 when `totalCount == 0` (BR-06), else the
integer percent of `doneCount/totalCount` rounded **HALF_UP**: 1/3 → 33, 2/3 → 67, 1/8 → 13 (OQ-22).

### `Subtask` — aggregate-internal child, **no `tenantId`** (reached only via the root) — `domain/Subtask.java`

| Field | Java | Column | Notes |
|---|---|---|---|
| `id` | `UUID` | `CHAR(36)` PK | `@PrePersist` |
| `name` | `String` | `VARCHAR(120) NOT NULL` | required |
| `startDate` / `endDate` | `LocalDate` | `DATE NULL` | each independently optional; start ≤ end enforced at the API edge |
| `done` | `boolean` | `BIT(1) NOT NULL` | the writable completion flag (FR-11) |
| `createdAt` / `updatedAt` | `Instant` | `DATETIME(6) NOT NULL` | backs the deterministic `@OrderBy`; per-subtask lifecycle endpoints justify child timestamps |

### `Priority` — `domain/Priority.java`
`LOW, MEDIUM, HIGH, CRITICAL` — the closed set of FR-10 (D7). No default: absence is a validation error.

### `domain/event/` — the AD-10 seam (new)
`sealed interface SubtaskChange { UUID taskId(); }` permitting the records
`SubtaskAdded(UUID taskId)`, `SubtaskCompleted(UUID taskId)`, `SubtaskUncompleted(UUID taskId)`,
`SubtaskRemoved(UUID taskId)` — plain records, no CDI imports (AD-01 clean). Events model facts,
carry ids only, and are observed synchronously in-transaction (`IN_PROGRESS` phase) by
`TaskProgressRecalculator`.

## Invariants

1. **BR-06:** `task.status` equals `percentOf(count(done subtasks), count(subtasks))` after every
   committed transaction that touched the aggregate. No code path assigns it from input; `TaskInput`
   poisons the field with `@Null`.
2. **BR-05 / C-10:** a task or subtask row disappears only through its explicit DELETE endpoint, and
   the same transaction writes the corresponding `AuditLog` row (`TASK_DELETED` / `SUBTASK_DELETED`).
3. **AD-03:** every `Task` read/write goes through `TaskRepository extends TenantScopedRepository<Task>`;
   `Subtask` is never queried directly — it has no repository.
4. **OQ-21:** the listing order is total and stable: `created_at DESC, id DESC`.

## Migration — `V5__tasks.sql` (latest on disk is V4)

```sql
-- V5 — tasks & subtasks (feature 010, US-4.1: FR-10/FR-11, BR-05/BR-06, OQ-21/OQ-22)
-- Task is the tenant-owned aggregate root (AD-03); Subtask is aggregate-internal (no tenant_id).
-- task.status is the STORED derived completion % (BR-06), recomputed in-transaction (AD-10).
-- US-4.2 will ALTER (all nullable, additive): task.details, task.card_code/card_url,
-- task.start_date/end_date (FR-12 derived); subtask.card_code/card_url. Nothing here blocks them.

CREATE TABLE task (
    id         CHAR(36)     NOT NULL,
    tenant_id  CHAR(36)     NOT NULL,
    name       VARCHAR(120) NOT NULL,
    priority   VARCHAR(10)  NOT NULL,
    status     INT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_task PRIMARY KEY (id),
    CONSTRAINT fk_task_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB;

-- One composite index serves BOTH the AD-03 tenant predicate (leftmost prefix) and the
-- OQ-21 newest-first page (tenant = const, then created_at/id via backward scan).
CREATE INDEX ix_task_tenant_created ON task (tenant_id, created_at, id);

CREATE TABLE subtask (
    id         CHAR(36)     NOT NULL,
    task_id    CHAR(36)     NOT NULL,
    name       VARCHAR(120) NOT NULL,
    start_date DATE         NULL,
    end_date   DATE         NULL,
    done       BIT(1)       NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_subtask PRIMARY KEY (id),
    CONSTRAINT fk_subtask_task FOREIGN KEY (task_id) REFERENCES task (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE INDEX ix_subtask_task ON subtask (task_id);
```

**Deliberately absent:** any `UNIQUE (tenant_id, name)` — the spec requires no task-name uniqueness
(two "Migrate broker" tasks are legal); any subtask `position` column (OQ-05: dates derive by date,
not position); any US-4.2 column (`details`, `card_*`, task-level dates).
