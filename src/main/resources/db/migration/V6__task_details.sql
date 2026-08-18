-- V6 — task details (feature 012, US-4.2: FR-12/FR-13/FR-14, BR-07, OQ-05/OQ-06, C-08)
-- The ALTERs V5's header earmarked. All nullable and additive; no existing row is invalidated.
-- task.start_date/end_date are the STORED derived dates (BR-07): min subtask start / max subtask
-- end, recomputed in-transaction (AD-10) — never written from input. Backfilled below.
-- card_code/card_url are the inline Card value object (OQ-06) — owned by the row, never shared;
-- card_code IS NULL means "no card". No CHECK: the API guarantees url never appears without code.
-- details is sanitized rich text (C-08) — TEXT like annotation_value.text_value (feat-005), bounded
-- by the service to 65 535 UTF-8 bytes AFTER sanitization, so strict-mode 1406 is unreachable.
-- No new index: none of these columns is a query predicate; the listing keeps ix_task_tenant_created.
-- VARCHAR(2048) under utf8mb4 may push long rows to off-page storage — harmless, expected.

ALTER TABLE task
  ADD COLUMN start_date DATE          NULL,
  ADD COLUMN end_date   DATE          NULL,
  ADD COLUMN card_code  VARCHAR(60)   NULL,
  ADD COLUMN card_url   VARCHAR(2048) NULL,
  ADD COLUMN details    TEXT          NULL;

ALTER TABLE subtask
  ADD COLUMN card_code  VARCHAR(60)   NULL,
  ADD COLUMN card_url   VARCHAR(2048) NULL;

-- Backfill the derived dates for tasks that predate V6 (FR-12 over already-stored subtask dates).
-- A correlated MIN/MAX over an empty or all-NULL set yields NULL, so tasks with no dated subtasks
-- stay dateless — exactly what Task.recomputeDates() computes at runtime.
UPDATE task t
   SET t.start_date = (SELECT MIN(s.start_date) FROM subtask s WHERE s.task_id = t.id),
       t.end_date   = (SELECT MAX(s.end_date)   FROM subtask s WHERE s.task_id = t.id);
