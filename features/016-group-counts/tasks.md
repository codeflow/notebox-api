# Tasks — Aggregate counts for the Navigator and group listings

**ID:** features/016-group-counts · **US:** US-3.1 · **Date:** 2026-08-24
**Source:** plan.md + contracts/group-counts.md + data-model.md (approved 2026-08-24)
**Status:** Approved (human approval 2026-08-24)

> Three tasks, strictly sequential. T-01 and T-02 both edit `AnnotationRecordRepository` and
> `TaskRepository`, and T-03 edits DTOs that T-01/T-02 populate, so no `parallel: yes` anywhere →
> no worktree isolation. Every task ships its tests; `mvn -B verify` must be green at the end of
> each. Ordered by risk, not just dependency: **T-01 carries the one change that could break
> something already shipped** — swapping `DISTINCT` for `GROUP BY` under feat-014's live tree — and
> the cheapest proof it is behaviour-preserving is feat-014's own tests passing unmodified. That
> proof is worth having before anything is built on top.

- [x] **T-01 · Tree counts — `DISTINCT` → `GROUP BY … COUNT` in the same statements** ✔ 2026-08-24, verify green (374 tests, 369 → 374; `8021dd2`). feat-014's 8 existing `NavigationResourceTest` cases pass **unmodified** — the evidence the swap is behaviour-preserving. Statement budget verified structurally (4 repository calls, one `createQuery` each) rather than by reading a SQL log.
      - files: `infrastructure/persistence/AnnotationRecordRepository.java` (`distinctTypeGroupPairsInTenant` → `typeGroupCountsInTenant`, + count), `infrastructure/persistence/TaskRepository.java` (`distinctGroupIdsInTenant` → `groupCountsInTenant`, + count), `application/navigation/NavigationService.java` (carry the count through), `api/dto/NavigationGroupNodeDto.java` (+ `count`, and `ungrouped(long)`), `test …/api/NavigationResourceTest.java` (extend)
      - covers: FR-09 node counts, data-model **A-1/A-2/A-7** · scenarios: "A group node under a type counts that type's records in that group", "The count equals the listing that clicking the node opens", "A task group node counts the tasks in that group", "The same group under two types counts each independently", "Counts never cross tenants"
      - depends: — · parallel: no
      - verify: `mvn -B verify` — the decisive evidence is **feat-014's existing `NavigationResourceTest` shape and ordering tests passing unmodified**: same rows, same order, only a field added. The cross-check scenario asserts a node's `count` against the `total` of the filtered listing it opens, so the two can never drift apart silently. Confirm in the SQL log that the statement count is unchanged (4) and that no statement is issued per node.

- [x] **T-02 · Listing aggregates — one grouped query per page, keyed by the page's group ids** ✔ 2026-08-24, verify green (382 tests, 374 → 382; `56256a2`). The null-vs-zero pair is asserted **against each other** (`assertNotEquals`), so collapsing them fails the build rather than passing quietly.
      - files: `application/group/GroupAggregates.java` (new value + `empty(domain)`), `infrastructure/persistence/AnnotationRecordRepository.java` (+ `aggregatesByGroupInTenant`), `infrastructure/persistence/TaskRepository.java` (+ `aggregatesByGroupInTenant`), `application/group/GroupService.java` (+ `aggregatesFor(domain, page)`), `test …/application/group/GroupAggregatesTest.java` (new)
      - covers: FR-08 aggregates, data-model **A-3/A-4/A-5/A-6** · scenarios: "A group reports its records and the types they span", "An empty group reports zero on both", "Types used counts distinct types, not records", "A group reports its tasks and their mean derived status", "The average rounds half up, like the status itself (OQ-22)", "A group with no tasks has NO average, which is not zero", "A group whose tasks are all at zero averages zero, not nothing"
      - depends: T-01 · parallel: no
      - verify: `mvn -B verify` — three things must be asserted, not assumed: the **null-vs-zero** pair (an empty group and an all-zero group are two different tests and must disagree); **HALF_UP** via the 33/34 → 34 case, which `Math.round` would also pass, so the assertion is on the rule not the value; and the **empty-page guard** — a page with no groups issues **no** aggregate query at all, never `in ()`.

- [x] **T-03 · Wire the aggregates out — `GroupDto`, resource, OpenAPI** ✔ 2026-08-24, verify green (386 tests, 382 → 386; `27994e0`). Additive compatibility evidenced by the diff itself: **93 insertions, 0 deletions** in `GroupResourceTest` — feat-014's 18 cases assert the same published fields unmodified. Added beyond the file list: a second `GroupDto.from(group)` overload for single-group reads, so create/get/replace do not pay for aggregates.
      - files: `api/dto/GroupDto.java` (+ `itemCount`, `typesUsed`, `averageStatus`; `from(group, aggregates)`), `api/GroupResource.java` (fetch the page's aggregates, pass them to the factory), `test …/api/GroupResourceTest.java` (extend), `test …/api/OpenApiCoverageTest.java` (extend)
      - covers: FR-08 at the wire, NFR-06, **the additive-compatibility guarantee** · scenarios: "A client ignoring the new fields is unaffected", "The aggregates do not turn a bounded read into an unbounded one", "An unauthenticated caller still gets nothing", plus the wire half of the annotation/task aggregate scenarios
      - depends: T-02 · parallel: no
      - verify: `mvn -B verify` — additive compatibility is evidenced by **feat-014's `GroupResourceTest` assertions passing unmodified** (every published field keeps its name, type and meaning); assert `typesUsed` is null on a task group and `averageStatus` null on an annotation group, so the domain-shaped nulls are contract, not accident; `OpenApiCoverageTest` covers the extended schemas.

## Coverage check

All **16** spec scenarios are claimed by exactly one task:

| Spec `Feature:` block | Scenarios | Task |
|---|---|---|
| FR-09 node sizes | 5 | T-01 |
| FR-08 annotation groups | 3 | T-02 (service) + T-03 (wire) |
| FR-08 task groups | 5 | T-02 (service) + T-03 (wire) |
| Additive by construction | 3 | T-01 (statement budget) + T-03 (compatibility, auth) |

**No scenario is uncovered**, and no task cites zero scenarios.

Two obligations belong to the plan rather than to a spec scenario, and are called out so the audit
does not read them as invention: the **method renames** (`typeGroupCountsInTenant`,
`groupCountsInTenant` — plan Approach) and the **empty-`IN` guard** (plan Risk 3).

**No migration in any task** — data-model.md states that as a checkable claim; if a task ever needs
one, the plan was wrong and the task should be blocked rather than a `V8` improvised.
