# Plan — Aggregate counts for the Navigator and group listings

**ID:** features/016-group-counts
**User Story:** US-3.1 · **Version:** v1
**Status:** Draft — awaiting human approval
**Date:** 2026-08-24
**Spec:** [spec.md](spec.md) — approved 2026-08-24, 16 scenarios

## Origin
- **Spec:** `features/016-group-counts/spec.md` (approved 2026-08-24).
- **FRs served:** FR-09 (tree data), FR-08 (group listing). **Originating decision:** OQ-27.
- **Refinements this plan implements:** average status rounds integer HALF_UP (mirroring OQ-22); a
  group with no tasks has **no** average, distinct from an average of 0.
- **Architecture bound by:** AD-01 (layering), AD-02 (`EntityManager` only in `infrastructure`),
  AD-03 (tenant choke point), AD-06 (JSON only), AD-12 (explicit CDI scopes).

## Approach

**The tree's counts are free.** Both navigation projections already aggregate — `select distinct
annotationTypeId, groupId` and `select distinct groupId` are grouping operations that happen to
discard the group size. Replacing `DISTINCT` with `GROUP BY … COUNT(e)` returns the same rows plus
the number, **in the same single statement**. No extra query, no per-node follow-up, and the
covering indexes feat-014 added (`ix_record_tenant_type_group`, `ix_task_tenant_group`) still serve
them as index-only scans, because a count is derivable from the index entries themselves. This is
what makes the spec's *"the number of database statements is the same as before this feature"*
scenario satisfiable rather than aspirational.

Both methods get honest names while their meaning changes: `distinctTypeGroupPairsInTenant` →
**`typeGroupCountsInTenant`**, `distinctGroupIdsInTenant` → **`groupCountsInTenant`**. Leaving
"distinct" on a method that now returns counts is how a reader later assumes the count is a
deduplicated one.

**The listing's aggregates cost exactly one statement per page — never one per row.** The group
listing fetches its page of groups as today, then issues **one** aggregate query keyed by that
page's group ids:

- annotation domain — `select groupId, count(e), count(distinct annotationTypeId) … where tenantId
  = :t and groupId in :pageIds group by groupId`
- task domain — `select groupId, count(e), avg(status) … where tenantId = :t and groupId in
  :pageIds group by groupId`

Bounded by the page size (≤ 200, NFR-08), and it rides the same tenant predicate as the page. A
group with no members simply does not come back from the aggregate query — which is precisely the
**null-vs-zero** distinction the spec demands: absent from the result means `itemCount = 0` and
`averageStatus = null`, while a group whose tasks are all at 0% *is* present and averages 0. The
distinction falls out of the query rather than needing a special case.

**Rounding is stated, not inherited.** `AVG` returns a `Double`; the service converts it with
`BigDecimal.setScale(0, HALF_UP)` rather than `Math.round`, so the rule is the one OQ-22 wrote down
rather than whatever the JDK happens to do at `.5`.

**Wire shape — flat fields with nulls, exactly as OQ-27 previewed.** `GroupDto` gains `itemCount`
(always present), `typesUsed` (annotation groups; null on task groups) and `averageStatus` (task
groups; null on annotation groups, and null on a task group with no tasks). A nested `aggregates`
object was considered and rejected below. `NavigationGroupNodeDto` gains `count`.

**No schema change.** No migration, no column, no index — everything is computed from what feat-014
already stores and already indexed. See `data-model.md`.

## Alternatives rejected

- **Stored counters on `item_group`** (a `member_count` column maintained on write). Rejected: it
  buys a marginally cheaper read for an invalidation obligation on every record, task and group
  mutation — including the FK's `ON DELETE SET NULL`, which bypasses the application entirely and
  would silently desynchronise the counter. The spec scoped it out and this plan agrees: a wrong
  count that looks right is worse than a computed one.
- **A correlated subquery per row in the listing's main query** (`select g, (select count(*) …)`).
  Rejected: it is an N+1 wearing one statement's clothing, and it forces the repository to return
  `Object[]` from what is otherwise a clean entity fetch.
- **`LEFT JOIN … GROUP BY` folded into the listing query itself.** Rejected: one statement instead
  of two, but it mixes entity hydration with aggregation, makes the paging (`setMaxResults` over a
  joined, grouped result) subtly wrong, and the second statement is O(1) per page anyway.
- **A nested `aggregates: { … }` object on `GroupDto`.** Rejected: an extra level of nesting for
  three scalars, and OQ-27's decision preview showed them flat — feat-015's spec was approved
  against that shape.
- **Computing the aggregates client-side** — already rejected at OQ-27 (one round trip per node).
- **A separate `/groups/{id}/stats` endpoint.** Rejected: it re-creates the N+1 at the HTTP layer,
  which is the whole thing OQ-27 chose the API to avoid.

## Reversibility

**One-way — where the effort went:**
- **The wire shape of the four new fields.** feat-015's approved spec consumes them; renaming later
  breaks a client whose spec is already signed off.
- **`averageStatus` null-vs-0 semantics.** A consumer that renders `null` as `0%` reports an empty
  group as stalled; once a client depends on the distinction it cannot be collapsed.
- **The count's *meaning*** — "the size of the listing clicking this node opens". It is the property
  the cross-check scenario asserts, and changing it silently invalidates that test's value.

**Reversible — decided, not deliberated:** the two-statement decomposition of the listing; the
repository method names; whether the aggregate query takes a `Collection` or an array; the
`BigDecimal` vs `Math.round` mechanic (the *rule* is one-way, the mechanic is not).

## Blast radius

**New files (2):** `application/group/GroupAggregates.java` (a small value: `itemCount`,
`typesUsed`, `averageStatus`), `test …/application/group/GroupAggregatesTest.java`.

**Existing files changed (10):**

| File | Change |
|---|---|
| `infrastructure/persistence/AnnotationRecordRepository.java` | `distinctTypeGroupPairsInTenant` → `typeGroupCountsInTenant` (+ count); new `aggregatesByGroupInTenant(ids)` |
| `infrastructure/persistence/TaskRepository.java` | `distinctGroupIdsInTenant` → `groupCountsInTenant` (+ count); new `aggregatesByGroupInTenant(ids)` |
| `application/navigation/NavigationService.java` | carry the count through the assembly |
| `application/group/GroupService.java` | expose `aggregatesFor(domain, groups)` |
| `api/dto/NavigationGroupNodeDto.java` | `+ count` |
| `api/dto/GroupDto.java` | `+ itemCount`, `+ typesUsed`, `+ averageStatus` |
| `api/GroupResource.java` | fetch the page's aggregates, pass them to the DTO factory |
| `test …/api/NavigationResourceTest.java` | count assertions |
| `test …/api/GroupResourceTest.java` | aggregate assertions |
| `test …/api/OpenApiCoverageTest.java` | the extended schemas (NFR-06) |

**Consumers:** feat-015 (not yet implemented — it is the reason this exists). **No existing
consumer breaks:** every published field keeps its name, type and meaning, so a client that ignores
the new ones is unaffected — asserted by its own scenario.

**Not touched:** the `item_group` schema, the membership FKs, BR-06/BR-07 derivation, the `group`
listing filter, `GroupService.delete`'s audit `countMembers` (see Risk 4).

## Risk

1. **`DISTINCT` → `GROUP BY` changing the tree's rows.** It should not — grouping by the same
   columns yields one row per pair, exactly what `DISTINCT` returned. *Signal:* feat-014's
   `NavigationResourceTest` shape and ordering tests fail. They run unmodified against the new
   queries, which is the evidence the substitution is behaviour-preserving.
2. **Rounding at `.5`.** `Math.round` and HALF_UP agree on positives, so a wrong mechanic would
   pass by luck at these values and diverge if statuses ever went negative or the rule changed.
   *Signal:* the 33/34 → 34 scenario. *Mitigation:* `BigDecimal`, stating the rule.
3. **An empty `IN` list.** A page with zero groups must not issue `… in ()` — invalid SQL in some
   dialects and a full scan in others. *Signal:* the empty-listing test. *Mitigation:* skip the
   aggregate query entirely when the page is empty and return no aggregates.
4. **`countMembers` and the new aggregate drifting apart.** Both count a group's members;
   `GroupService.delete` uses the former for its audit detail. They agree today. *Signal:* none
   automatic — noted so the auditor does not assume one was derived from the other. Deliberately
   **not** merged: delete needs one group's count and the aggregate path is page-shaped.
5. **`avg` over an empty set.** MySQL returns `NULL`, which is the semantics we want — but the group
   simply will not appear in the result at all, so the null arrives by absence. *Signal:* the
   no-tasks scenario. Documented so it is not "fixed" into a `0`.

## Test plan (16 scenarios → 4 classes)

| Class | Covers | Scenarios |
|---|---|---|
| `NavigationResourceTest` (extend) | node counts, per-(type,group) independence, cross-tenant | 5 |
| `GroupResourceTest` (extend) | annotation + task aggregates, empty group, types-used vs records | 7 |
| `GroupAggregatesTest` (new) | rounding HALF_UP, null-vs-zero average, absent-group mapping | 3 |
| `OpenApiCoverageTest` (extend) | the extended schemas are published (NFR-06) | 1 |

The additive-compatibility scenario is covered by feat-014's existing assertions continuing to pass
unmodified — the only honest evidence that nothing published changed shape.
