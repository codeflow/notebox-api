# Data model — Aggregate counts (feat-016, US-3.1)

## No schema change

**No migration. No new entity. No new column. No new index.**

Every value this feature adds is *derived on read* from what feat-014 already persists, over the
indexes it already created. `V7__groups_navigation.sql` remains the latest migration.

This section exists because "there is no data model change" is a claim the audit should be able to
check, not an omission it has to interpret. The three things that would make it false — a stored
counter, a materialised view, a new index — are each rejected below with the reason.

| Considered | Rejected because |
|---|---|
| `item_group.member_count` column | Needs invalidation on every record/task/group mutation **including the FK's `ON DELETE SET NULL`**, which bypasses the application entirely (feat-014, OQ-24). A counter that silently desynchronises is worse than a computed one. |
| Materialised aggregate table | Same invalidation obligation, plus a second source of truth for a number MySQL computes in the same statement. |
| New index for the aggregates | Unnecessary — see *Indexes already sufficient* below. |

## The derived values

| Value | Surface | Definition | Type |
|---|---|---|---|
| `count` | `/navigation` group node | number of items the node stands for — records of *that type* in *that group* under Annotations; tasks in that group under Tasks; the Ungrouped node under the same rule | `long` ≥ 0 |
| `itemCount` | `/groups` row | records in the group (annotation domain) or tasks in the group (task domain) | `long` ≥ 0 |
| `typesUsed` | `/groups` row, **annotation domain only** | distinct annotation types the group's records span | `long` ≥ 0, `null` on task groups |
| `averageStatus` | `/groups` row, **task domain only** | mean of the group's tasks' derived statuses, integer percent HALF_UP | `Integer` 0–100, `null` on annotation groups **and** on a task group with no tasks |

## Invariants

| # | Invariant | Enforced by | Cites |
|---|---|---|---|
| A-1 | Every aggregate is computed inside the caller's tenant | the same `:tenant` predicate as the row it decorates (AD-03) | BR-01/BR-02, C-01, NFR-01 |
| A-2 | A tree node's `count` equals the `total` of the listing that node opens | both derive from the same predicate over the same table | spec's cross-check scenario |
| A-3 | `itemCount` counts **items**, `typesUsed` counts **distinct types** — 9 records of one type is `9 / 1` | `count(e)` vs `count(distinct e.annotationTypeId)` | design 14 |
| A-4 | `averageStatus` is **absent** for a task group with no tasks, and **0** for a group whose tasks are all at 0% | the group is missing from the aggregate result vs present with `avg = 0.0` | spec, OQ-22 refinement |
| A-5 | `averageStatus` rounds half up to an integer percent | `BigDecimal.setScale(0, HALF_UP)`, not `Math.round` | OQ-22 |
| A-6 | Aggregates never consult the other namespace | the query targets the one entity the group's domain permits | feat-014 I-8 |
| A-7 | No aggregate is issued per row or per node | one grouped statement for the tree; one per listing **page** | NFR-08 |

## Indexes already sufficient

| Query | Index | Why it holds |
|---|---|---|
| tree — `group by annotationTypeId, groupId` | `ix_record_tenant_type_group (tenant_id, annotation_type_id, group_id)` | the grouping columns are the index's own columns in order; the count comes from the index entries, so it stays an index-only scan |
| tree — `group by groupId` | `ix_task_tenant_group (tenant_id, group_id)` | same |
| listing — `where groupId in (…) group by groupId` | the same two | `tenant_id` is the leftmost prefix and `group_id` is covered; the `IN` list is bounded by the page size |

`count(distinct annotationTypeId)` for the annotation aggregate is served by the same composite —
the type column sits between the tenant and the group in the index, so the distinct count is
computed from the index rather than the row data.

**Nothing new is needed.** feat-014 added those two indexes for the tree's `DISTINCT` projections
(plan Risk 4); the aggregates are the same access pattern with the count retained.

## Rollback

Nothing to roll back — no schema object is created. Reverting the feature is reverting code.
