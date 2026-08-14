# Audit — Tasks with subtasks and derived progress (feat-010, US-4.1)

**Date:** 2026-08-13 · **Auditor:** session (opus·xhigh discipline) + independent adversarial
sub-agent for the author-bias-exposed checks (traceability, scenario honesty) with fresh context.
**Method:** all six mandated checks ran explicitly; the sub-agent read every spec scenario, every
production file and every test body, then re-executed the seven test classes against the real
MySQL Dev Service — all green (TaskResourceTest 22/22, TaskServiceTest 9/9, TaskTest 9/9,
TaskRepositoryTest 5/5, TaskProgressRecalculatorTest 1/1, TaskMessageCoverageTest 1/1,
OpenApiCoverageTest 3/3; suite total 209). Constitution/scope/compliance/OQ checks ran in the
session via greps and `git diff db42c43..HEAD`.

## Check 1 — Traceability: PASS
All 21 scenarios map to at least one test whose body genuinely exercises the Given/When/Then
against real production code — real JWTs (`TestTokens`), real MySQL, real CDI events; the only
mock anywhere is `TenantContext` as *input* in non-HTTP tests. Highlights: the status-poison
scenario asserts both the 400 **and** the unchanged stored status via a fresh GET; the PRD §4
canonical example (2-of-4 → 50%) runs at both service and wire level; pt-locale asserts the exact
Portuguese catalog string. No broken chain. (Full 21-row table in the sub-agent run; row-level
notes folded into the findings below.)

## Check 2 — Scenario honesty: PASS with findings
The observer-never-fired trap is well covered: `TaskProgressRecalculatorTest` persists a task
whose stored status disagrees with its subtasks, fires the event, `em.flush(); em.clear()`, and
asserts the recomputed value on a **fresh fetch** — it fails if the observer is unwired or acts on
a detached instance. Wire reads prove stored status across separate requests. Logic inversion is
caught by the 1/3→33 / 0-done→0 / all-done→100 triangulation; 12.5→13 pins HALF_UP at the exact
boundary. Two strength gaps survived the hunt — findings 1 and 2.

## Check 3 — Constitution: PASS
Greps clean on every forbidden pattern: no `EntityManager`/query API outside `infrastructure/`;
no `domain/` import of api/infrastructure/`jakarta.ws.rs`; no manual transaction control; no
cache client anywhere in the slice; no user-facing string literals (all catalog keys). Explicit
scopes on all three new beans; the resource carries none, matching shipped-resource precedent
(AD-12 names `@ApplicationScoped` as the default for JAX-RS resources). BR-06 held structurally:
no code path assigns status from input — `TaskInput.status` is `@Null`-poisoned and only
`Task.recomputeStatus()` writes the field. BR-05: both delete paths are explicit endpoints,
audited in-transaction. AD-10 inaugurated exactly as the AD describes (sealed facts, synchronous
IN_PROGRESS observer).

## Check 4 — Compliance: PASS
- **C-01/C-02 (applies):** cross-tenant scenarios prove 404-indistinguishability at wire, service
  and repository seams; unauthenticated 401 test present.
- **C-06 (applies, standing):** no new transport surface; existing TLS ingress config unchanged.
- **C-09 (applies):** 11 keys line-parallel in both catalogs; `TaskMessageCoverageTest` pins
  presence; the pt wire test pins one exact text.
- **C-10 (applies):** `TASK_DELETED` and `SUBTASK_DELETED` audit rows asserted in the same
  transaction as the mutation.
- **C-08 and the rest (n/a):** grep-proven — no sanitizer/rich-text reference anywhere in the
  slice; no secrets, images, admin surface or retention path introduced.

## Check 5 — Scope: PASS (deviations recorded, all additive)
`git diff db42c43..HEAD` matches the plan's enumerated blast radius exactly on the main side
(24 new main files + 2 catalogs; the plan prose said "19" but its own enumeration — the authority —
lists precisely these). Deviations, both recorded at the moment they happened: `TaskInput`/
`SubtaskInput` + both validator pairs shipped with T-03 instead of T-04 (service signature
required them to compile); one extra test class (`TaskProgressRecalculatorTest`) beyond the plan's
five, justified as the dedicated AD-10 wiring proof. No unplanned production surface; no shipped
file modified beyond the two message catalogs and `OpenApiCoverageTest`.

## Check 6 — Open Questions: PASS
OQ-21 (listing order) and OQ-22 (status representation) were opened by the spec, answered by
**rafaelsantos on 2026-08-13**, recorded in `catalogs/open-questions.md` with decisions, and
folded into spec/PRD before implementation. No OQ was closed by implementer assumption; no new
OQs arose during implementation.

## Findings (ranked)

1. **Important — wire `page`/`size` plumbing untested.** Hardcoding `service.list(0, 50)` in
   `TaskResource.list` keeps the entire suite green: no test drives `?page=`/`?size=` through HTTP
   with a non-default value and asserts the returned rows (the `PageDto.size` field echoes the
   request, not the query). Repository-level pagination IS tested, so the exposure is exactly the
   resource→service wiring — and feat-008's own resource test covers this for records, a precedent
   this contract cites. *Concrete failure:* a client's `GET /tasks?size=10` returns 50 rows;
   `?page=1` repeats page 0; CI stays green. Also untested: `page=-1`/`size=0` rejection rows of
   the contract matrix.
2. **Important — subtask date accept-path and round-trip untested.** The only date in the test
   tree is the rejection case. *Concrete failures that stay green:* `DateRangeValidator`
   regressing to reject every date pair (no client can set dates at all); `SubtaskDto.from` or the
   entity mappings transposing start/end (dates come back swapped). These dates exist precisely so
   US-4.2/FR-12 can derive task dates — a silent swap poisons FR-12 later.
3. **Observation** — audit assertions pin tenant+target+action but not actor/detail; wrong-"who"
   or dropped detail would pass. Mitigated by NOT NULL columns; idiom inherited from shipped
   features.
4. **Observation** — `listTasks_newestFirst` has a theoretical microsecond-collision flake window
   (random-UUID tiebreak vs creation order); negligible across three HTTP round trips, and the
   repository test handles collisions with a comparator-invariant assertion.
5. **Observation** — the unauthenticated test exercises one route (matching the scenario's
   literal text); the other seven rely on class-level `@Authenticated` (verified present). A
   refactor to per-method annotations that missed one route would go undetected.
6. **Observation** — foreign-tenant PUT/DELETE assert the 404 status only (the envelope code is
   asserted for GET); cross-tenant subtask-create/delete unexercised. All routes funnel through
   the same `service.get` choke point, so the risk is structural drift only.
7. **Observation** — `TaskMessageCoverageTest` asserts key presence, not non-blank values;
   verified by inspection that all 22 entries carry real text.
8. **Observation** — the documented `done: null → false` PUT-replace semantics has no direct
   test; feat-011 must send the full subtask object on update (contract already says so).

## Verdict: **PASS with findings**

No blocker: intent, artifacts and code say the same thing, every scenario is honestly covered,
and all boundaries held. Findings 1–2 are test-strength gaps (2–3 additive test methods, no
production change) logged as backlog for hardening before or alongside feat-011's consumption of
this contract; 3–8 are observations, no action required. Gate `audit_pass`: **open**.

**Post-verdict hardening (same day, human-approved):** findings 1 and 2 closed by three additive
test methods in `TaskResourceTest` — `listTasks_pageAndSizeParams_driveTheQuery` (page 1 of size 2
returns exactly t2,t1 of 5, so a hardcoded `list(0, 50)` fails), `listTasks_negativePageOrZeroSize_rejected`
(the remaining contract-matrix rows), and `subtaskDates_acceptedAndRoundTripUnswapped` (accept path,
exact unswapped round-trip, PUT-replace clearing the omitted date). Verify green: **212 tests**.
Production code needed no change — the tests confirmed the audited behavior and now pin it.
