-- V7 — item groups & navigation tree (feature 014, US-3.1: FR-08/FR-09,
-- OQ-04/OQ-23/OQ-24/OQ-25, BR-05, C-10)
-- Table is item_group, NOT group: GROUP is a reserved word in MySQL and an unquoted `group`
-- table breaks every statement that touches it. The entity is named Group; only the table differs.
-- Membership is a nullable FK on the ITEM — OQ-04's "at most one group" made structural, since a
-- second membership is then unrepresentable. NULL is the one and only representation of ungrouped.
-- ON DELETE SET NULL *is* the OQ-24 rule: deleting the label never deletes a member. Deliberately
-- unlike OQ-14's block rule for annotation types, whose justification is orphaned records.
-- Both ALTERs are additive and nullable; no existing row is invalidated and no backfill is needed
-- (every pre-V7 record and task is ungrouped, which is exactly NULL).

CREATE TABLE item_group (
  id         CHAR(36)     NOT NULL,
  tenant_id  CHAR(36)     NOT NULL,
  name       VARCHAR(120) NOT NULL,
  domain     VARCHAR(10)  NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  CONSTRAINT pk_item_group PRIMARY KEY (id),
  CONSTRAINT fk_item_group_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
  -- Serves the AD-03 tenant predicate (leftmost prefix), the per-domain group listing and the
  -- OQ-25 name ordering, so no separate index is created.
  CONSTRAINT uq_item_group_tenant_domain_name UNIQUE (tenant_id, domain, name)
) ENGINE=InnoDB;

ALTER TABLE annotation_record
  ADD COLUMN group_id CHAR(36) NULL,
  ADD CONSTRAINT fk_record_group FOREIGN KEY (group_id)
      REFERENCES item_group (id) ON DELETE SET NULL;

ALTER TABLE task
  ADD COLUMN group_id CHAR(36) NULL,
  ADD CONSTRAINT fk_task_group FOREIGN KEY (group_id)
      REFERENCES item_group (id) ON DELETE SET NULL;

-- Makes the navigation tree's DISTINCT projections index-only scans and covers the group-filtered
-- listing predicate. Both also satisfy InnoDB's FK-column index requirement, so no implicit
-- single-column index is generated.
CREATE INDEX ix_record_tenant_type_group
  ON annotation_record (tenant_id, annotation_type_id, group_id);
CREATE INDEX ix_task_tenant_group
  ON task (tenant_id, group_id);
