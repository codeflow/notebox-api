# Audit — Item groups and navigation-tree data

**ID:** features/014-groups-navigation-notebox-api · **US:** US-3.1 · **Round:** 1
**Date:** 2026-08-23 · **Model:** opus/xhigh
**Scope:** `develop..feature/groups-navigation` — 10 commits, 54 source files, +3156/−115
**Verify at audit time:** `mvn -B verify` → BUILD SUCCESS, **368 tests, 0 failures, 0 errors**
**Hardening closed:** 2026-08-24 — all four items resolved, **369 tests** green

## Verdict — **PASS WITH FINDINGS** (all findings closed 2026-08-24)

Three findings, **0 blockers**. Every FR traces to a scenario, a test and code; every architecture
boundary is uncrossed by grep, not by assumption; every `applies` compliance item has its evidence
where the spec said it would be. The findings are one spec-vs-code wording mismatch that needs a
human decision, one dishonest test fixture, and one latent NPE — none of which changes shipped
behaviour.

---

## 1. Traceability — FR → scenario → test → code

| FR | Scenarios | Tests | Code |
|---|---|---|---|
| **FR-08** groups CRUD + at-most-one assignment | 15 (blocks 1–3) | `GroupTest` 5, `GroupSchemaTest` 6, `GroupServiceTest` 15, `GroupResourceTest` 18, `GroupAssignmentTest` 10 | `Group`, `GroupDomain`, `GroupRepository`, `GroupService`, `GroupResource`, `V7` |
| **FR-09** navigation-tree data | 8 (blocks 4–5) | `NavigationResourceTest` 7, `GroupFilteredListingTest` 8, `GroupFilterTest` 5 | `NavigationService`, `NavigationResource`, 3 DTOs, 4 projections |
| standing | 3 (block 6) | spread across the above | — |

**All 26 spec scenarios have a test that exercises them.** No orphaned scenario, no test citing a
scenario that does not exist. Chain followed in the real files, not inferred from names.

## 2. Scenario honesty — would these fail on a regression?

Spot-checked the tests most likely to be theatre. Results:

- **`GroupSchemaTest.deletingAGroupUngroupsItsRecords_andDeletesNone`** — reads back after
  `em.clear()`, so it asserts stored rows rather than the persistence context. Drop the FK's
  `ON DELETE SET NULL` and it fails on the first assertion. **Honest.**
- **`GroupServiceTest.deletingAGroupIsAudited`** asserts `members=3`. Move the count to *after*
  `groups.remove(...)` and it reads `members=0` and fails. That ordering is the subtle part of
  OQ-24 and it is genuinely pinned. **Honest** — but see **F-02** on its fixture.
- **`NavigationResourceTest.siblingOrderingIsCaseInsensitiveWithUngroupedLast`** uses
  `alpha`/`Beta`/`gamma`; a case-*sensitive* comparator orders `Beta` first and fails. **Honest.**
- **`NavigationResourceTest.theTreeStopsAtGroupNodes`** greps the raw response body for distinctive
  record and task names. Enumerate leaves and it fails. **Honest** — OQ-23 asserted, not assumed.
- **`GroupMessageCoverageTest.portugueseIsActuallyTranslated_notAnEnglishCopy`** — a presence-only
  check passes while shipping English to a `pt` caller; this one does not. **Honest.**
- **`GroupFilteredListingTest.anUnknownOrForeignGroupYieldsAnEmptyPageNotA404`** — turn the empty
  page into a 404 and it fails, which is what keeps the filter from being an existence oracle.
  **Honest.**

**One adversarial probe run during this audit, not before it:** `@ValidGroupDomain` sits on a
**query parameter** (`GET /groups?domain=`), and nothing proved Jakarta Validation actually fires
there rather than letting a bogus value reach `GroupDomain.valueOf` and surface as a 500. Probed
directly with `?domain=SOMETHING_ELSE` → **400**. Validation does run. The probe was kept as a
permanent test (`listingRejectsADomainOutsideTheClosedSet`), since the contract's validation matrix
claims that row and nothing covered it. Test count 367 → 368.

## 3. Constitution — greps, not assumptions

> Every grep below was run with a **quoted** `--include='*.java'` and a positive control first
> (154 files matched). An unquoted glob silently matches nothing in this shell and would have
> produced a false all-clear on every line of this section.

| Boundary | Result |
|---|---|
| **AD-01** `domain/` imports no `infrastructure`/`api`/`jakarta.ws.rs` | **CLEAN** — 0 hits |
| **AD-02** `EntityManager`/`createQuery` only in `infrastructure/` | **CLEAN** — 0 hits in `api/` or `application/` |
| **AD-03** every query carries the tenant predicate | **CLEAN** — 16 queries across the 4 touched repositories, **0** without `:tenant` |
| **AD-05** no user-facing literals; catalog keys only | **CLEAN** — all 5 new exceptions carry dot-namespaced keys |
| **AD-06/AD-11** no error shaping inlined in resources | **CLEAN** — 0 `Response.status(4xx/5xx)` / `new Problem` in the new resources |
| **AD-12** explicit CDI scope on every new bean | **CLEAN** — all 5 new beans annotated |

**BR-01/BR-02** (isolation): held — the tenant predicate result above plus four cross-tenant tests.
**BR-05** (explicit irreversible delete): held, and refined by OQ-24 — the group is destroyed, its
members are not. **BR-06/BR-07** (derived status and dates): untouched; `TaskService`'s recompute
paths are not in the diff.

## 4. Compliance pre-flight — evidence where it was claimed

| Item | Claimed evidence | Found |
|---|---|---|
| **C-01** tenant isolation | cross-tenant test per endpoint | ✔ `GroupResourceTest`, `NavigationResourceTest`, `GroupFilteredListingTest`, `GroupAssignmentTest` |
| **C-02** authenticated by default | 401 per new endpoint | ✔ 5 group endpoints + `/navigation` |
| **C-06** encryption in transit | ingress unchanged | ✔ no new transport surface in the diff |
| **C-09** localization completeness | en+pt coverage + pt assertions | ✔ `GroupMessageCoverageTest` (10 keys, both files, translation-not-copy) + 2 wire assertions |
| **C-10** audit trail | audit entry + test | ✔ `GROUP_DELETED`/`GROUP`/`members=n`, asserted incl. `members=0` |

The 7 items marked **n/a** were re-checked against the diff and remain n/a: no image binaries
(C-07), no rich text (C-08), no admin surface (C-03), no new identity data (C-04), no credentials
(C-05), no account management (C-11), no Secret-flagged values (C-12).

## 5. Scope — diff vs the plan's blast radius

The plan named 11 new + 13 changed production files. The diff carries **6 production files the
plan never mentioned**:

| File | Verdict |
|---|---|
| `domain/error/Group{NotFound,NameTaken,DomainNotModifiable,DomainMismatch}Exception` | **plan was incomplete, not scope creep** — the constitution's §Errors mechanism makes these mandatory; a localized domain rejection cannot be thrown without one. Same omission feat-012's plan made with `TaskDetailsTooLongException`. |
| `domain/error/GroupFilterInvalidException` | same |
| `infrastructure/persistence/AnnotationTypeRepository` (+1 method) | **plan was incomplete** — the tree's structural axis (C28) needs the type list name-ordered; the plan described the query but omitted the file |

None adds behaviour beyond the spec. All six were recorded in the `tasks.md` tick and the commit
body at the time, so they arrive at this audit disclosed rather than discovered.

**6 existing test files** were touched purely to widen 62 constructor call sites — mechanical,
predicted by the plan, no assertion changed.

**One unplanned refactor:** T-05 made the pre-existing unfiltered repository methods delegate to
the filtered ones with `GroupFilter.none()`. This is *inside* the blast radius (both repositories
were listed) and reduces duplication rather than adding surface — it leaves one source of truth for
the OQ-20/OQ-21 ordering. feat-008/feat-010's repository tests still pass unmodified, which is the
evidence that the delegation is behaviour-preserving.

## 6. Open Questions

**OQ-23, OQ-24, OQ-25** were opened *and* resolved on 2026-08-22, each recorded in
`catalogs/open-questions.md` with `decided by rafaelsantos` and the date, **before any scenario was
written**. None was closed by the implementer's own assumption — the decisive check for this
section. OQ-24 and OQ-25 each record *why* they diverge from an earlier decision (OQ-14, OQ-20/21),
so the audit does not read them as inconsistency.

The spec's own refinements (name ≤ 120, unique per tenant per domain, immutable domain, the
structural-vs-projection presence rule) sit under the standing **OQ-09** delegation — *"feature
specs may refine"* — and were listed in the spec's Open Questions section and surfaced at the
approval gate. Legitimate.

---

## Findings

### F-01 · Spec says the request "is rejected"; the code accepts and ignores — **needs a decision**
**Severity:** medium (artifact-vs-code mismatch, no behavioural risk)
**Where:** spec.md scenario *"A group cannot be nested"* vs `GroupResourceTest.aGroupCannotBeNested`
**Concrete failure:** `POST /groups {"name":"Child","domain":"ANNOTATION","parentId":"<uuid>"}`
returns **201**, not the rejection the scenario states. The property is silently dropped because the
project does not enable `FAIL_ON_UNKNOWN_PROPERTIES`.
**Why it is not a blocker:** OQ-04's substance holds completely — no parent column exists, none is
stored, none is returned, and `GroupTest` fails if a parent accessor is ever added. Nesting is
unrepresentable. Only the scenario's wording is wrong.
**Options:** (a) amend the scenario to *"the parent is not stored"* — a one-line spec edit matching
reality and every other endpoint in the codebase; (b) enable `FAIL_ON_UNKNOWN_PROPERTIES` globally
— changes deserialization for **every** endpoint and is a constitution-level decision, not a
feature one. **Recommend (a).** Raised by the implementer at T-03, not discovered here.

### F-02 · The audit-count test uses a fixture the API cannot produce
**Severity:** low (test honesty)
**Where:** `GroupServiceTest.deletingAGroupIsAudited`
**Concrete failure:** the test builds an **ANNOTATION** group holding 2 records **and 1 task**, then
asserts `members=3`. A task in an annotation group is exactly what `resolveForAssignment` rejects
(`group.domain.mismatch`); the fixture reaches it only by bypassing the service with
`em.persist(task)`. The assertion is arithmetically right but describes an unreachable state, so it
would keep passing even if cross-domain membership became possible — the opposite of what it should
guard.
**Fix:** make the fixture 3 records (one namespace, as the API enforces), and let
`GroupAssignmentTest` remain the sole owner of the cross-domain rule. Backlog, not blocking:
`members=n` is independently pinned by the `members=0` case.

### F-03 · `countMembers` runs two queries where the namespace guarantees one is always empty
**Severity:** low (efficiency + latent inconsistency)
**Where:** `GroupRepository.countMembers`
**Concrete failure:** deleting an ANNOTATION group issues a `count(Task)` that structurally cannot
match, since a task can only reference a TASK-domain group (I-8). Every group delete therefore pays
one wasted round trip. Harmless today; it also quietly makes F-02's impossible fixture *look*
supported.
**Fix:** branch on `group.getDomain()` and count the one table that can hold members. Backlog.

### N-01 · Latent NPE in the ordering comparator — **noted, not a finding**
`NavigationService.BY_NAME` dereferences `n.name()` and `n.groupId()`, both null on the Ungrouped
node. It is safe **only** because Ungrouped is appended *after* `nodes.sort(...)`. Correct today; a
future edit that sorts the assembled list would NPE. Labelled a suspicion rather than a finding
because no current input reaches it. Cheap hardening: give the comparator null-last semantics and
drop the ordering dependency.

---

## Hardening — all four closed 2026-08-24

Nothing was carried to backlog. Verify green at 369 tests (368 → 369).

| Item | Resolution |
|---|---|
| **F-01** | **Decided by rafaelsantos:** amend the scenario. `spec.md` → **v2** — *"the request is rejected"* became *"the parent is not stored and the created group has none"*, with the reason recorded inline and in the spec's Open Questions. Deserialization is untouched, so no other endpoint's behaviour moved. |
| **F-02** | Fixture rebuilt inside one namespace. **A second instance the audit had missed** was found while fixing the first — `deletingAGroupUngroupsItsMembersAndDeletesNone` had the same defect. It now uses two groups, one per namespace, which is both API-reachable *and* still proves the FK un-groups both tables. |
| **F-03** | `countMembers` branches on `group.getDomain()` and issues **one** query. The dead cross-domain count is gone, and with it the implication that cross-domain membership is a supported state. |
| **N-01** | Comparator is null-safe on both components and pins Ungrouped last **itself**. `nodes()` now adds Ungrouped *before* the sort, so the guarantee is exercised rather than defensive: without the comparator's null handling the list would NPE. Regression test `ungroupedSortsLastEvenBehindANameThatOrdersAfterEverything` uses a group named `zzz` — one that sorts after everything yet must still precede Ungrouped. |

### Audit self-correction

F-02 was reported as a single test. It was two. The audit's spot-check read
`deletingAGroupIsAudited` closely and took the neighbouring
`deletingAGroupUngroupsItsMembersAndDeletesNone` at face value because its *name* described the
behaviour correctly — exactly the failure mode section 2 exists to catch. Recorded here rather than
quietly fixed, since an audit that under-reports is worth knowing about.

## Not found

No cross-tenant leak. No boundary crossing. No weakened or deleted test — feat-008/feat-010's
repository tests still assert the unfiltered contract unmodified. No OQ closed by assumption. No
scenario without a test. No unplanned behaviour in the diff.
