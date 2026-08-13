-- V5 — tasks & subtasks (feature 010, US-4.1: FR-10/FR-11, BR-05/BR-06, OQ-21/OQ-22)
-- Task is the tenant-owned aggregate root (AD-03); Subtask is aggregate-internal (no tenant_id).
-- task.status is the STORED derived completion % (BR-06), recomputed in-transaction (AD-10).
-- US-4.2 will ALTER (all nullable, additive): task.details, task.card_code/card_url,
-- task.start_date/end_date (FR-12 derived); subtask.card_code/card_url. Nothing here blocks them.
-- No UNIQUE (tenant_id, name): the spec requires no task-name uniqueness.
-- No subtask position column: dates derive by date, not position (OQ-05).

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
) ENGINE=InnoDB;

-- One composite index serves BOTH the AD-03 tenant predicate (leftmost prefix — no separate
-- ix_task_tenant needed) and the OQ-21 newest-first page (tenant = const, created_at/id scan).
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
) ENGINE=InnoDB;

CREATE INDEX ix_subtask_task ON subtask (task_id);
