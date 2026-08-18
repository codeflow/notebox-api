# Audit — Task dates, card link and rich-text details (feat-012, US-4.2)

**Date:** 2026-08-17 · **Auditor:** session (opus·xhigh discipline) + independent adversarial
sub-agent with fresh context for the author-bias-exposed checks (traceability, scenario honesty,
attack pass), following the feat-010 precedent. **Method:** all six mandated checks ran
explicitly. Constitution/compliance/scope/OQ checks ran in the session by grep over
`git diff develop..HEAD` (28 files, +1470/−80; five per-task commits `e501250..9cb33ab`), plus a
concrete `java.net.URI` probe of the new constraint. Checks 1–2 and the sub-agent's independent
test re-run are recorded below.

## Check 1 — Traceability: PASS
The sub-agent mapped all **25 scenarios** to named test methods and the production path each
drives — no broken link. Every FR-12 date rule is pinned at the pure-domain level (`TaskTest`,
exact spec fixtures incl. the A/B/C one-sided set and the inverted pair) *and* at the service
level through the real CDI observer (`TaskServiceTest`, `@TestTransaction`), with the
insertion-order, empty-task, reschedule and listing-row cases additionally proven over the wire
(`TaskResourceTest`, real JWTs, real MySQL). FR-13 card scenarios are asserted on **reloaded**
entities (`em.flush(); em.clear()`) at the service level and on POST/PUT/GET shapes at the wire;
FR-14 details on the **stored** value (write side), the wire response, and a hostile row planted
by native SQL and read back through the API (read side). Standing C-01/C-09 scenarios asserted at
the wire. Full 25-row table in the sub-agent run; two rows rated *weak* rather than *covered* —
scenario 7 (its "And" clause, finding 2 below) and, cosmetically, scenario 6's service test is
named `updateSubtask_…` but exercises `addSubtask` (finding 5).

## Check 2 — Scenario honesty: PASS with findings
The sub-agent verified, regression by regression, that the tests fail when the behaviour breaks:
observer deleted → reschedule/removal date asserts fail; `SubtaskRescheduled` not fired →
`SubtaskChangeRecorder` count asserts fail; read-side sanitization removed →
`legacyHostileDetailsRow_isSanitizedOnTheWayOut` fails on `<script`; write-side removed → the
DB-reload assertion in `create_hostileDetails_storedValueIsSanitized` fails; bound applied before
sanitization → `create_detailsOversizeOnlyBeforeSanitization_…` fails; all-null `Card` not
materializing as null → `update_omittingCard_clearsIt` (reload) and the wire `card nullValue()`
fail; details leaking onto rows → `not(hasKey("details"))` fails; `AFTER_SUCCESS` misphase →
the `@TestTransaction` reads fail. Strength gaps that survived, none blocking:
- **V6 backfill has no automated test** — Flyway runs on an empty schema, so a wrong `UPDATE`
  would pass the suite. *Mitigated in the audit itself:* the sub-agent executed the exact V6 SQL
  against `mysql:8.4` with five fixtures (two-dated, one-sided, dateless subtasks, no subtasks,
  inverted) — all correct (dateless/no-subtask tasks stay `NULL/NULL`; the alias-qualified
  `UPDATE task t SET t.col` form is accepted). A seeded-before-migration test is not expressible
  in the standard `@QuarkusTest` setup; recorded as backlog.
- **No exact byte-boundary test** for the details guard (`TaskService.java` `> 65535`): the tests
  use 65 607 (rejected) and 65 007 (accepted); an off-by-one `>=` would stay green while rejecting
  a legal 65 535-byte value. → hardening item.
- Scenario 7's "And" clause unasserted (finding 2); scenarios 10/22 wire tests assert the
  POST/PUT response rather than a follow-up GET (the service tests reload from DB, so the
  behaviour is covered overall — noted, not actioned).

## Check 3 — Constitution: PASS
Greps clean on every boundary the plan bound:
- **BR-06 frozen:** the only diff lines touching `recomputeStatus`/`percentOf` are Javadoc; the
  status math is byte-identical to feat-010.
- **BR-07 held structurally:** `Task` exposes **no** date setter; the only assignments to
  `startDate`/`endDate` are inside `Task.recomputeDates()` (`Task.java:236-245`); `TaskInput`
  poisons both components with `@Null`, so no input can reach a date field. The only
  `setStartDate/setEndDate` calls in `src/main` are on `Subtask`.
- **AD-01 layering:** `domain/` imports nothing from `api`, `infrastructure` or `jakarta.ws.rs`;
  `application/task` imports only `api.dto` records (feat-010 precedent); `api/dto/TaskDto` depends
  inward on `application.content.RichTextSanitizer` (allowed direction, `AnnotationValueDto`
  precedent).
- **AD-02:** no `EntityManager`/query API outside `infrastructure/` (tests exempt, feat-007
  precedent for the legacy-row native write).
- **AD-03:** `TaskDatesRecalculator` re-fetches through `TaskRepository.findByIdInTenant`
  (`TaskDatesRecalculator.java:33`) — the choke point, same as `TaskProgressRecalculator`.
- **AD-09/AD-10/AD-12:** declarative `@Transactional` only (no `UserTransaction` in main); the
  new observer is `@ApplicationScoped`, constructor-injected, default IN_PROGRESS phase; the sealed
  `SubtaskChange` hierarchy grew by exactly one member (`SubtaskRescheduled`), fired after the
  aggregate mutation, as the plan's ordering rule requires.
- **C-09 hygiene:** all constraint messages are dot-namespaced keys; no user-facing literals.

## Check 4 — Compliance: PASS
Every item marked `applies` in the spec's pre-flight has its evidence where the spec said:
- **C-01 (applies):** `TaskResourceTest.foreignTenant_taskWithCardAndDetails_stillBehavesLikeMissing`
  — 404 on read, update **and** delete of a foreign task carrying card + details; the observer
  reads through the tenant choke point; feat-010's per-endpoint cross-tenant tests still run green
  over the extended DTOs.
- **C-02 (applies):** no new endpoint; `anyTaskEndpoint_unauthenticated_rejected` still asserts
  401 across the surface.
- **C-06 (applies, standing):** no new transport surface; ingress config unchanged.
- **C-08 (applies — this feature is the constitution item's named case):** write side asserted on
  the **stored** value (`TaskServiceTest.create_hostileDetails_storedValueIsSanitized`,
  `update_hostileDetails_sanitizedExactlyLikeCreate`) and on the wire (script/on*/`javascript:`
  href/`src`-stripped image); read side asserted by planting a hostile row via native SQL and
  reading it through the API (`legacyHostileDetailsRow_isSanitizedOnTheWayOut`); dialect-clean
  value byte-identical over the wire; **one dialect project-wide** — `TaskService` and
  `TaskDto.from` use the same injected `RichTextSanitizer` singleton, `JsoupRichTextSanitizer`
  untouched.
- **C-09 (applies):** 6 new keys line-parallel in both catalogs; `TaskMessageCoverageTest` pins
  all 17 task keys; `cardValidationMessage_resolvesInPortugueseLocale` pins one exact pt text.
- **C-07 (n/a, standing):** no image upload/retrieval surface added (`ImageResource`/`ImageService`
  untouched); the `src`-stripping test proves no binary or foreign URL enters through details.
- **C-03/C-04/C-05/C-10/C-11/C-12 (n/a):** grep-proven — no admin surface, no identity data, no
  credential, no new irreversible action (clearing card/details is the client's stated replace
  state, BR-05 untouched), no retention path, no secret field.

## Check 5 — Scope: PASS (two deviations, both recorded at the moment they happened)
`git diff --name-only develop..HEAD` matches the plan's enumerated blast radius on the main side
(the plan's prose counts "7 new / 10 modified" undercount its own enumeration — 8 new incl. V6,
12 modified incl. the five DTOs and two catalogs — the enumeration is the authority, as with
feat-010's "19"). Every file the plan marked **explicitly untouched** is untouched:
`TaskRepository`, `TaskProgressRecalculator`, `RichTextSanitizer`, `JsoupRichTextSanitizer`,
`ImageResource`, `ImageService`, `PageDto`, both exception mappers, `OpenApiCoverageTest`,
`application.properties`, migrations V1–V5. Beyond the enumeration, exactly two files, both
recorded on their task in `tasks.md`:
- `domain/error/TaskDetailsTooLongException` (T-04) — the plan mandated the guard, the HTTP 400 and
  the key but not the class; the constitution §Errors mechanism (`DomainException` → mapper) and the
  `AnnotationRecordValueTooLongException` precedent make it the only correct vehicle. Not scope
  creep — a plan omission, now closed.
- `testsupport/SubtaskChangeRecorder` (T-02, test-only) — lets the service tests assert *which*
  facts fire (reschedule = exactly one; name-only = none), a proof the plan's risk table asked for
  and no existing test-support bean could give.

## Check 6 — Open Questions: PASS
No OQ was opened or closed during implementation. OQ-05 (min/max by date) and OQ-06 (inline value
object) were resolved 2026-07-22 and are folded into spec, plan and code (`recomputeDates()`
min/max; `@Embeddable Card`). The card bounds and the BR-07 write-rejection are OQ-09 refinements
declared in the approved spec, not implementer assumptions. No `[TBD]` marker exists in any
feat-012 artifact.

## Attack pass (session) — findings, ranked

1. **`@AbsoluteHttpUrl` accepts authority-less http(s) forms — non-blocking (data quality, not
   security).** Concrete: `POST /tasks` with `"card": {"code": "PAY-1", "url": "https:foo"}` (or
   `"https:///path"`) → **201, stored**; the web renders `<a href="https:foo">`, which the browser
   resolves against the app origin — a dead/misleading link. `java.net.URI.isAbsolute()` only
   checks scheme presence. Probe results: `https:foo` ✗-accepted, `https:///path` ✗-accepted;
   `https://`, `http:`, `javascript:`, `data:`, `/rel`, `ftp://`, whitespace variants all correctly
   rejected. The contract (`contracts/task-details.md`) says "absolute URI with scheme http/https"
   and the code implements exactly that — so this is a **contract-strength gap**, not a code/contract
   divergence. Hardening: additionally require `uri.getHost() != null`
   (`AbsoluteHttpUrlValidator.java:24-27`) and note it in the contract; feat-013 consumes the same
   rule.
2. **Scenario 7's "And" clause is not asserted — non-blocking (test strength).** The spec says
   *"And the stored task's dates remain exactly the computed values"*; `updateTask_supplyingEndDate_rejectedAsDerived`
   asserts the 400 only. It is trivially true today (rejection happens at the edge, before the
   resource method runs), but feat-010's status-poison test asserted the follow-up GET and the
   audit praised it; mirror that so a future refactor that moves the check inward cannot silently
   half-apply the payload.
3. **Observation for feat-013 (not a feat-012 defect):** under the established PUT-replace
   semantics, feat-011's shipped web client sends `TaskInput`/`SubtaskInput` **without** `card` or
   `details`, so a card or details set through the API is **cleared** by any edit made from the
   old UI until feat-013 ships. This is exactly the contract feat-010 defined and this spec
   inherited; record it in feat-013's spec as a motivating constraint (the pair should reach
   `main` together — consistent with the promotion posture already in HANDOFF).

## Attack pass (sub-agent, fresh context) — merged findings, ranked
The sub-agent's independent probe confirmed finding 1 with more forms (`https:/foo`, `https:?q`,
`http:foo@bar`, `https:javascript:alert(1)` — all http(s)-scheme, none XSS-capable, all stored)
and finding 3, and added:
4. **`"url": ""` is rejected as invalid — non-blocking, contract edge.** `new URI("")` is
   relative → 400 `task.card.url.invalid`; only `null`/omitted means "no URL". Undocumented in
   `contracts/task-details.md`; feat-013 must send `null`, not `""`. → contract note.
5. **Cosmetic:** `TaskServiceTest.updateSubtask_oneSidedDates_derivedBoundsAreIndependent`
   exercises `addSubtask`. → rename in hardening.
6. **`"details": ""` is stored and served as `""` — non-blocking, contract edge.** The sanitizer
   passes empty through; the contract says `null` = none, so `null`, `""` and `<p></p>` are three
   distinguishable "empty" states on the wire. Faithful to input, not a defect; → contract note
   (feat-013 decides how an emptied editor maps — `null` clears).
7. **Suspicion — poison dates and Jackson coercion:** `"startDate": ""` / `"startDate": []` are
   coerced to `null` by JSR-310 defaults and pass `@Null` — silently accepted rather than rejected
   with `task.dates.not_writable`. Harmless (nothing is stored; dates stay computed) and the same
   class as the shipped `"status": null` edge; → contract note, no code change.
8. **Suspicion — inherited, not introduced:** no `@Version` on `Task`; two concurrent subtask
   mutations can leave stored derived dates stale (last writer wins) — the identical race class
   feat-010 accepted for stored status. Backlog note against the aggregate, not this feature.
9. **Verified NOT defects:** byte bound is exactly the TEXT capacity (65 535 accepted / 65 536
   rejected by `>`), measured post-sanitization on UTF-8 bytes; `Objects.equals` on `LocalDate`
   handles null↔date and same-value and is computed **before** mutation; observer phase is
   IN_PROGRESS in the firing TX; **every** task-returning resource path (6 sites) goes through
   `TaskDto.from(…, sanitizer)` and the listing DTO has no details field; all-null embeddable →
   null proven against MySQL; V6 backfill correct against `mysql:8.4`; Jackson `startDate:[2026,9,1]`
   / numeric → non-null → correctly rejected.

**Independent test re-run (sub-agent):** `TaskResourceTest` 51/0/0 · `TaskServiceTest` 28/0/0 ·
`TaskTest` 20/0/0 · `TaskMessageCoverageTest` 1/0/0 → 100 run, 0 failures — BUILD SUCCESS. Session
gate at implement close: full `mvn -B verify` **268/0/0**.

## Verdict: **PASS with findings**

Nothing blocking survived either the session's or the sub-agent's attack: no spec/plan/contract
divergence, no constitution or compliance breach, no scope creep, no OQ closed by assumption. The
findings are all non-blocking and split three ways:

**Hardening (same-day, before publish — the feat-010/011 precedent):**
- H1 · `@AbsoluteHttpUrl` additionally requires a non-null authority (`uri.getRawAuthority()`),
  with validator unit cases (`https:foo`, `https:///p`, `https:?q` rejected; `https://[::1]/`,
  IDN authority, `HTTPS://X` accepted) and one wire case; contract wording updated (finding 1).
- H2 · scenario 7: follow-up GET asserting the dates stayed as computed after the 400 (finding 2).
- H3 · exact byte-boundary service tests: 65 535 accepted, 65 536 rejected (check-2 gap).
- H4 · rename the misnamed one-sided-dates test (finding 5).
- H5 · `contracts/task-details.md` "Documented edges" gains: `""` url invalid (4), `""` details
  stored as-is (6), Jackson-coerced empty date poison passes `@Null` (7), and the rollout note (3).

**Backlog (recorded, not actioned here):** V6 backfill has no automated test (manually verified
against MySQL in this audit); `@Version`/optimistic locking on the task aggregate is a
feat-010-inherited gap covering stored status *and* dates.

**Handed to feat-013's spec:** the PUT-replace × legacy-web rollout hazard (finding 3 / sub-agent
4): until feat-013 ships, an old-UI task edit or a subtask checkbox tick clears any card/details
set via the API — spec-conformant, so the pair should reach `main` together and feat-013's input
builders must echo `card`/`details`.

Gate `audit_pass` opens. `catalogs/epics.md`: US-4.2 → `building` (API half audited; the story is
`delivered` only when feat-013 ships — the US-2.1/US-4.1 rule). Issue #13 gets the verdict comment;
nothing merges or closes here (publish → review → promotion).

## Hardening close-out (same day, 2026-08-17)

All five hardening items landed on `feature/task-details` before publish, verify green at
**274 tests** (268 → 274):
- **H1 ✔** `AbsoluteHttpUrlValidator` requires a non-empty raw authority (`getRawAuthority()` — so
  IDN hosts, which `getHost()` reports as null, stay valid); new `AbsoluteHttpUrlValidatorTest`
  (4 cases: null passes; six accepted forms incl. `https://[::1]/`, IDN, upper-case scheme; nine
  authority-less/opaque forms rejected; ten other-scheme/relative/unparseable forms rejected) and
  a wire case (`createTask_cardUrlWithoutAuthority_rejected`: `https:foo`, `https:///path`,
  `https:?q` → 400 `task.card.url.invalid`). Contract wording updated (validation matrix + constraint
  note).
- **H2 ✔** `updateTask_supplyingEndDate_rejectedAndDatesStayComputed` now seeds a dated subtask,
  sends the poisoned PUT, and re-reads: dates exactly the computed values **and** name/priority not
  half-applied (feat-010 status-poison precedent).
- **H3 ✔** `create_detailsExactlyAtTheByteBound_accepted_oneOver_rejected`: 65 535 bytes stored
  byte-identical, 65 536 → `TaskDetailsTooLongException`.
- **H4 ✔** `addSubtask_oneSidedDates_derivedBoundsAreIndependent` (renamed).
- **H5 ✔** `contracts/task-details.md` documented edges: `""` url invalid, `""` details stored as-is
  (three distinguishable empties), Jackson-coerced empty date poison passes `@Null`, and the
  PUT-replace × legacy-web rollout note.

Backlog items (V6 backfill automated test; `@Version` on the task aggregate) and the feat-013
hand-off stand as recorded above.
