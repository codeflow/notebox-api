# Audit — Aggregate counts for the Navigator and group listings

**ID:** features/016-group-counts · **US:** US-3.1 · **Round:** 1
**Date:** 2026-08-24 · **Model:** opus/xhigh
**Scope:** `develop..feature/group-counts` — 5 commits, 12 source files
**Verify at audit time:** `mvn -B verify` → BUILD SUCCESS, **386 tests, 0 failures, 0 errors**
**Hardening:** 2026-08-24 — F-01 and F-02 closed, F-03 to backlog. **389 tests** green.

## Verdict — **PASS WITH FINDINGS** (F-01, F-02 closed 2026-08-24; F-03 backlogged)

Three findings, **0 blockers**. One of them — **F-01** — is a wrong value on a published field and
should be fixed before this merges, even though no approved scenario covers it.

---

## 1. Traceability — FR → scenario → test → code

| FR | Scenarios | Tests | Code |
|---|---|---|---|
| **FR-09** node counts | 5 | `NavigationResourceTest` +5 | `typeGroupCountsInTenant`, `groupCountsInTenant`, `NavigationService`, `NavigationGroupNodeDto` |
| **FR-08** annotation groups | 3 | `GroupAggregatesTest`, `GroupResourceTest` | `aggregatesByGroupInTenant`, `GroupAggregates`, `GroupDto` |
| **FR-08** task groups | 5 | `GroupAggregatesTest`, `GroupResourceTest` | same |
| additive by construction | 3 | see **F-03** | — |

**15 of 16 scenarios have an executable test.** The sixteenth is F-03.

## 2. Scenario honesty — would these fail on a regression?

- **`noTasksMeansNoAverage_whichIsNotAnAverageOfZero`** — asserts the two values against *each
  other* with `assertNotEquals`, not just individually. Collapse null into 0 and the build fails.
  **Honest, and the strongest test in the feature.**
- **`theAverageRoundsHalfUp`** — avg(33,34) → 34. `Math.round` would also pass this, which the test
  says out loud; it exists to pin the *rule*, and the implementation states HALF_UP explicitly
  rather than inheriting it. **Honest about its own limits.**
- **`anEmptyPageYieldsNoAggregatesAndIssuesNoQuery`** — asserts the returned map is empty. It does
  **not** observe that no SQL was issued; the guard is a plain `isEmpty()` early return, verified by
  reading. Adequate, and the name overstates slightly (see F-03's neighbourhood).
- **feat-014's 8 `NavigationResourceTest` cases and 18 `GroupResourceTest` cases pass unmodified** —
  the diff to `GroupResourceTest` is **93 insertions, 0 deletions**. That is the additive-
  compatibility claim as evidence rather than assertion, and it is the single most valuable signal
  in this audit: the `DISTINCT` → `GROUP BY` swap did not change a row, an order, or a field.
- **`aNodesCountEqualsTheTotalOfTheListingItOpens`** — see **F-02**. It cross-checks two independent
  queries, which is real, but it never anchors either to an expected value.

**Two adversarial probes run during this audit, not before it:**

1. **`GroupDto.from(group)` on a populated group.** Injected a temporary test: create a group, put
   **18 records** in it, read `GET /groups/{id}`. Result — `"itemCount":0,"typesUsed":0`. → **F-01**.
   Probe removed; verify re-run green at 386.
2. **Row multiplication from the `@OneToMany` associations.** `AnnotationRecord` owns `values` and
   `Task` owns `subtasks`; a join would inflate `count(e)`. Read the generated JPQL: neither
   aggregate query joins a collection — both project straight off the root entity. **No inflation.**
   Worth checking, because this is the classic way an aggregate silently doubles.

## 3. Constitution — greps, not assumptions

> Quoted `--include='*.java'` with a positive control first (155 files). An unquoted glob matches
> nothing in this shell and would have produced a false all-clear on every line here.

| Boundary | Result |
|---|---|
| **AD-01** `domain/` imports no infra/api/`ws.rs` | **CLEAN** |
| **AD-02** `EntityManager`/`createQuery` only in `infrastructure/` | **CLEAN** — 0 hits in `api/`, `application/` |
| **AD-03** tenant predicate on every query | **CLEAN** — 17 queries across the 4 touched repositories, **0** without `:tenant` |
| **AD-12** explicit CDI scope | **CLEAN** — no new bean; `GroupService`'s widened constructor stays constructor-injected |

**BR-06 is respected, not redefined:** the average is taken over the *already-derived* `status`; no
code here recomputes or writes it. **BR-01/BR-02:** a count is a disclosure, and the cross-tenant
scenario proves another tenant's 40 records never surface as a size.

## 4. Compliance pre-flight — evidence where claimed

| Item | Claimed | Found |
|---|---|---|
| **C-01** tenant isolation | cross-tenant assertions | ✔ `countsNeverCrossTenants`; every aggregate carries `:tenant` |
| **C-02** authenticated by default | 401 per endpoint | ✔ feat-014's existing assertions, unmodified |
| **C-06** encryption in transit | standing | ✔ no new transport surface |

The 9 **n/a** items were re-checked against the diff and remain n/a. **C-09 n/a is correct and
verified:** `git diff` shows **no change to `messages.properties` or `messages_pt.properties`** —
this feature genuinely emits no user-facing string.

## 5. Scope — diff vs the plan's blast radius

**Exactly the plan's 12 files** — 2 new, 10 changed, nothing more. **No migration file in the
diff**, which is `data-model.md`'s central claim checked rather than trusted.

One addition beyond the plan's file list, disclosed in the T-03 tick and commit at the time: the
second `GroupDto.from(group)` overload. It is *also* the subject of F-01 — the disclosure was
honest, the decision was wrong.

## 6. Open Questions

**OQ-27** is this feature's originating decision, resolved by rafaelsantos on 2026-08-24 before the
spec was written. The **OQ-23 clarification** likewise. The two OQ-09 refinements — HALF_UP
rounding, and null-is-not-zero — were **disclosed in the spec's Open Questions section and approved
at the gate**, not closed by the implementer. Nothing here was decided by assumption.

---

## Findings

### F-01 · A single-group read reports `itemCount: 0` for a group that holds 18 records
**Severity:** medium-high — a wrong value on a published field. **Fix before merge.**
**Where:** `GroupDto.from(Group)` → `GroupResource.get/create/replace`
**Concrete failure:** create the annotation group "Infrastructure", assign 18 records to it, then
`GET /groups/{id}`. The listing says `itemCount: 18`; the single read says **`itemCount: 0`,
`typesUsed: 0`**. Verified by probe, not inferred.
**Why it happened:** T-03 added a `from(group)` overload that fills the aggregates with
`GroupAggregates.empty(domain)` so single reads "do not pay for a count nobody asked for". Java
records serialise every component, so *abstaining* is not available — the overload does not omit
the fields, it **publishes zeros**. The optimisation bought a lie for a saved query on a
single-row read.
**Consequence for the consumer:** feat-015 renders whatever `GroupDto` carries. A group detail or
a post-create response would show "0 annotations" for a populated group.
**Fix:** compute the aggregates on single reads too — `aggregatesFor(domain, List.of(group))` is
one query for one group. The saving was never worth a wrong number.

### F-02 · The cross-check test has no anchor, so a both-zero regression survives it
**Severity:** low (test strength)
**Where:** `NavigationResourceTest.aNodesCountEqualsTheTotalOfTheListingItOpens`
**Concrete failure:** the test reads `count` from the tree, then asserts the listing's `total`
equals it — but never asserts either is **12**. If a shared regression returned `0` from both (a
broken tenant predicate, an empty result), `0 == 0` passes and the test reports success.
**Why it still has value:** the two numbers come from independent queries, so a bug in one alone is
caught, and `aGroupNodeUnderATypeCountsThatTypesRecordsInThatGroup` pins the absolute 12 separately.
The pair is sound; this test alone is not.
**Fix:** assert `nodeCount == 12` before the comparison. One line, and it turns a relative
assertion into an anchored one.

### F-03 · One scenario has no executable test — it was verified by code inspection
**Severity:** low (traceability)
**Where:** spec scenario *"The aggregates do not turn a bounded read into an unbounded one"*
**Concrete gap:** the scenario says *"the number of database statements is the same as before this
feature / and no statement is issued per node"*. Nothing asserts this at runtime. It was verified
structurally — `tree()` makes 4 repository calls and each method issues exactly one `createQuery`,
counted by grep — and reported as such in the T-01 commit. That is a real argument, but it is not a
test, and it will not fail if someone later adds a per-node lookup.
**Fix:** a Hibernate `StatementInspector` counting statements around the two reads, asserting 4 for
the tree and 3 for a listing page. Real work — a test-scope inspector bean — which is why it is
recorded here rather than improvised now.
**Related, smaller:** `anEmptyPageYieldsNoAggregatesAndIssuesNoQuery` asserts the *result*, not the
absence of SQL; the same inspector would let its name become true.

---

## Hardening — F-01 and F-02 closed 2026-08-24

Verify green at **389 tests** (386 → 389).

| Item | Resolution |
|---|---|
| **F-01** | **Fixed.** The `GroupDto.from(Group)` overload that published zeros is **deleted**, not patched — leaving it would have kept a way to emit a wrong count. `GroupService.aggregatesOf(group)` computes a single group's aggregates, and `get`/`create`/`replace` all route through it. Three regression tests: the single read now agrees with the listing at 18/1; a rename still reports its aggregates; and a freshly created group reports 0 — *truthfully*, which is the case that distinguishes a real zero from the placeholder. |
| **F-02** | **Fixed.** `assertEquals(12, nodeCount)` anchors the cross-check before the comparison, so a regression returning 0 from both surfaces can no longer pass as `0 == 0`. |
| **F-03** | **Backlogged deliberately.** A Hibernate `StatementInspector` is a test-scope bean plus wiring — real work, and improvising it inside a hardening pass is how a half-built harness lands unreviewed. It remains the only scenario without an executable test. |

### Note on the hardening run

The first `mvn -B verify` after the fixes reported `Tests run: 373, Errors: 1, Skipped: 310` — which
reads as a catastrophic regression and is not one. The Docker daemon had stopped, so Testcontainers
could not start MySQL and the Quarkus test app failed to boot. Recorded because the failure mode
mimics a code defect precisely: restarting Docker and re-running gave 389/0/0 with **zero** skips.

## Remaining backlog

- **F-03** — a statement-counting inspector, which would cover both the budget scenario and make
  `anEmptyPageYieldsNoAggregatesAndIssuesNoQuery`'s name true.

## Not found

No cross-tenant leak. No boundary crossing. No row inflation from the owned collections. No
migration where the data model claimed none. No weakened or deleted test — 0 deletions across both
extended test files. No OQ closed by assumption. No scope creep.
