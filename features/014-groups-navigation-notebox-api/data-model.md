# Data model — Item groups (feat-014, US-3.1)

**Migration:** `src/main/resources/db/migration/V7__groups_navigation.sql`
**Bound by:** AD-03 (tenant choke point), BR-01/BR-02 (isolation), BR-05 + OQ-24 (delete), OQ-04
(single-membership, flat, per-domain).

## New entity — `Group` → table `item_group`

> **Table name.** `GROUP` is a reserved word in MySQL. The entity is `Group`; the table is
> `item_group`. An unquoted `group` table breaks every statement that touches it.

| Field | Java | Column | Type | Null | Notes |
|---|---|---|---|---|---|
| `id` | `UUID` | `id` | `CHAR(36)` | no | `@JdbcTypeCode(SqlTypes.CHAR)`, assigned in `@PrePersist` — house pattern |
| `tenantId` | `UUID` | `tenant_id` | `CHAR(36)` | no | `TenantOwned`; immutable (`updatable = false`) |
| `name` | `String` | `name` | `VARCHAR(120)` | no | required, non-blank; 120 = the `annotation_type.name` / `task.name` bound |
| `domain` | `GroupDomain` | `domain` | `VARCHAR(10)` | no | `@Enumerated(STRING)`; `ANNOTATION` \| `TASK`; **immutable after creation** |
| `createdAt` | `Instant` | `created_at` | `DATETIME(6)` | no | `updatable = false` |
| `updatedAt` | `Instant` | `updated_at` | `DATETIME(6)` | no | `@PreUpdate` |

`Group implements TenantOwned` and is reached **only** through `GroupRepository extends
TenantScopedRepository<Group>` (AD-03). It is a leaf aggregate: it owns no children and holds no
reference to its members — membership is expressed on the item side, so a group never has to be
loaded to read or write an item.

### New enum — `GroupDomain`
`ANNOTATION`, `TASK`. Closed set, in the `Priority` / `FieldType` / `BadgeColour` mould; validated
at the edge by `@ValidGroupDomain` (AD-07), persisted by name.

### Constraints and indexes

| Name | Kind | Columns | Why |
|---|---|---|---|
| `pk_item_group` | PK | `id` | — |
| `fk_item_group_tenant` | FK → `tenant(id)` | `tenant_id` | ownership (D4) |
| `uq_item_group_tenant_domain_name` | **UNIQUE** | `tenant_id, domain, name` | OQ-04's separate namespaces + the spec's per-tenant-per-domain uniqueness, in one constraint. Mirrors `uq_annotation_type_tenant_name` |

**No separate index.** The unique key's B-tree on `(tenant_id, domain, name)` already serves the
AD-03 tenant predicate (leftmost prefix), the per-domain group listing, and the OQ-25 name ordering.
Same reasoning as V5's single `ix_task_tenant_created`.

## Changed entities — the membership column

Two nullable FKs. `NULL` means **ungrouped**, and is the only representation of it.

### `annotation_record`
| Column | Type | Null | Notes |
|---|---|---|---|
| `group_id` | `CHAR(36)` | **yes** | FK → `item_group(id)` **`ON DELETE SET NULL`** |

### `task`
| Column | Type | Null | Notes |
|---|---|---|---|
| `group_id` | `CHAR(36)` | **yes** | FK → `item_group(id)` **`ON DELETE SET NULL`** |

Both map as a plain `UUID` column (`@JdbcTypeCode(SqlTypes.CHAR)`), **not** a `@ManyToOne`. The item
never needs the group object — assignment validates a resolved group in the service, and the tree
resolves ids to names in one batch — so an association would buy a lazy proxy and an N+1 risk for
nothing.

### New indexes

| Name | Table | Columns | Why |
|---|---|---|---|
| `ix_record_tenant_type_group` | `annotation_record` | `tenant_id, annotation_type_id, group_id` | makes the tree's `select distinct annotationTypeId, groupId` an index-only scan (Risk 4), and covers the group-filtered listing predicate |
| `ix_task_tenant_group` | `task` | `tenant_id, group_id` | same, for `select distinct groupId` |

InnoDB requires an index on each FK column and would auto-create one; both indexes above have
`group_id` covered, so no implicit single-column index is generated.

## Invariants

| # | Invariant | Enforced by | Cites |
|---|---|---|---|
| I-1 | A group belongs to exactly one tenant and is never read or written outside it | `TenantScopedRepository` (AD-03) | BR-01, BR-02, C-01 |
| I-2 | A group's name is non-blank and ≤ 120 chars | Bean Validation at the edge (AD-07) | spec / OQ-09 |
| I-3 | Within one tenant and one domain, a group name is unique | `uq_item_group_tenant_domain_name` + a localized duplicate mapping | spec / OQ-09 |
| I-4 | The same name may exist once per domain | the domain column being part of the unique key | OQ-04 |
| I-5 | A group's domain never changes after creation | service rejects a mismatching domain on update; column has no update path | spec |
| I-6 | Groups are flat — no group references another | no parent column exists; unrepresentable | OQ-04 |
| I-7 | An item references **at most one** group | single nullable FK column; unrepresentable otherwise | OQ-04, FR-08 |
| I-8 | An item's group is of the item's own domain | `GroupService` domain check before assignment (an FK cannot express it) | spec, FR-08 |
| I-9 | An item's group is of the item's own tenant | the group is resolved through the tenant-scoped repository, so a foreign id is simply not found | BR-01, NFR-01 |
| I-10 | Deleting a group deletes **no** record and **no** task; every member becomes ungrouped | `ON DELETE SET NULL` | **OQ-24**, BR-05 |
| I-11 | Deleting a group is audited (who / what / when) | `AuditLogRepository` write in `GroupService.delete`, `subtasks=`-style detail | C-10, BR-05 |

## Migration — `V7__groups_navigation.sql`

```sql
-- V7 — item groups & navigation tree (feature 014, US-3.1: FR-08/FR-09,
-- OQ-04/OQ-23/OQ-24/OQ-25, BR-05, C-10)
-- Table is item_group, NOT group: GROUP is a reserved word in MySQL.
-- Membership is a nullable FK on the ITEM (OQ-04 single-membership made structural);
-- NULL is the one and only representation of "ungrouped".
-- ON DELETE SET NULL *is* the OQ-24 rule: deleting the label never deletes a member.
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
  -- Serves the AD-03 tenant predicate (leftmost prefix), the per-domain listing,
  -- and the OQ-25 name ordering — so no separate index is created.
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

-- Makes the navigation tree's DISTINCT projections index-only scans and covers the
-- group-filtered listing predicate. Both also satisfy InnoDB's FK-column index requirement,
-- so no implicit single-column index is generated.
CREATE INDEX ix_record_tenant_type_group
  ON annotation_record (tenant_id, annotation_type_id, group_id);
CREATE INDEX ix_task_tenant_group
  ON task (tenant_id, group_id);
```

**Rollback.** Additive and reversible: drop the two FKs and columns, drop the two indexes, drop
`item_group`. No data outside `item_group` is destroyed, because membership is the only thing the
ALTERs added.
