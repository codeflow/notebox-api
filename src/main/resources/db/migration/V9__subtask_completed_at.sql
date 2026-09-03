-- FR-20 · When a subtask was completed.
--
-- A subtask carries done as a boolean and nothing about WHEN. That moment cannot be recovered
-- afterwards, and it cannot be derived: "finished after the planned date" computed as
-- done AND end_date < today turns TRUE for work delivered ON TIME as soon as today walks past
-- end_date. So it is written at the transition (OQ-38, human decision 2026-09-02).
--
-- NULLable, and NULL carries meaning rather than absence of data: every row that exists when this
-- runs was completed before the moment was ever recorded, and none is back-filled — inventing a
-- value would manufacture a lateness verdict out of nothing.
--
-- No CHECK, following V6: the API guarantees the invariant. The only constraint expressible here
-- (completed_at IS NULL OR done = 1) guards a direction nothing threatens, while the shape a bug
-- would produce (done = 1 with no moment) is exactly the legacy shape and can never be forbidden.
ALTER TABLE subtask
  ADD COLUMN completed_at DATETIME(6) NULL AFTER done;
