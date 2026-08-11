-- Audit detail (audit F6): which value a reveal/erase touched — "who saw the DB password"
-- must be answerable per field, not only per record (BR-10, C-10).
ALTER TABLE audit_log ADD COLUMN detail VARCHAR(255) NULL;
