# Audit — Annotation types (feat-003, US-1.1)

**Step:** `feat-003-annotation-types.audit` · **Model/effort:** opus/xhigh · **Date:** 2026-07-23
**Auditor mandate:** try to prove the feature is *not* done. Six checks below, each reported explicitly.
**Diff audited:** `develop..feature/annotation-types` @ `268fbe2` (includes remediation commit `910f064`).
`mvn -B verify` is green (66 tests). Verify-green is not the question here — the question is whether the
chain spec→scenario→test→code is intact and whether behaviour would actually fail on regression.

**History:** First pass (@ `8db6094`) verdicted **FAIL** on one blocking regression (F1) plus five lower
findings. The coordinator remediated; this is the re-verification.

## Verdict: **PASS WITH FINDINGS**

The blocking regression (F1) is fixed and now regression-guarded; I re-ran the offending endpoint and
confirmed it. F3/F4/F5 are addressed; F2 is now recorded in the plan as a tracked deviation. Two
non-blocking residuals remain, logged below against OQ-16 and as a backlog test (F6, F2-residual) — neither
is a defect in feat-003's delivered behaviour. The gate opens; `catalogs/epics.md` US-1.1 → `delivered`.

---

## Re-verification of the previously-blocking finding

### F1 — RESOLVED (was HIGH/blocking) · feat-001 validation no longer 500s

**Fix.** `ConstraintViolationMapper` (`api/error/ConstraintViolationMapper.java`) now resolves every message
through a new private `localize(key, locale)` that catches `MissingResourceException` and returns the raw
message as-is (lines 62–68). A constraint carrying a non-catalog default message (feat-001's `LoginRequest`
/`UserPatch`, e.g. `"must not be blank"`) therefore degrades to a plain field message instead of throwing,
and the top-level `"validation.failed"` (a real catalog key) still resolves localized. feat-001's resolver
and DTOs were left untouched — a minimal, well-scoped fix.

**Re-verified empirically (this audit).** Ran `mvn -B test -Dtest=ValidationErrorContractTest` — BUILD
SUCCESS, 2 passed. The endpoint that returned **500 + a leaked stack trace** in the first pass now returns:
`POST /api/auth/login {"email":"not-an-email","password":""}` → **`400 {"code":"validation.failed",…}`**.
The regression test asserts `statusCode(400)`, so a reintroduced 500 would fail CI. feat-003's own path is
also re-asserted: a blank field name returns `violations[].code = annotation.field.name.required`.

**Residual (non-blocking, tracked under OQ-16).** For feat-001's default-message constraints, the per-field
`violations[].code`/`message` is now the raw English text `"must not be blank"` rather than a dot-namespaced
key localized to pt. This is not a BR-08 breach — BR-08 forbids a *raw key or blank* reaching the client;
the top-level message is localized and the field text is human-readable English. Full per-field localization
of feat-001's DTOs is exactly the realignment deferred in **OQ-16**, now explicitly noted in `plan.md`. It is
pre-existing feat-001 drift surfaced through the unified mapper, not a regression introduced by feat-003.

## Status of the other findings

- **F2 — ADDRESSED (recorded).** The first pass flagged that deleting feat-001's `ValidationExceptionMapper`
  actioned the *deferred* OQ-16 by implementer assumption. `plan.md` §Blast radius now records this as a
  deliberate deviation — "the forced subset of the OQ-16 realignment" — with the defensive-mapper mitigation
  and the note that full feat-001 DTO migration stays deferred under OQ-16. The action is now
  plan-documented rather than silent. Acceptable.
- **F3 — RESOLVED.** `plan.md` blast radius rewritten: it no longer claims "no behavioural change to
  feat-001," it lists the `ValidationExceptionMapper` deletion, names the one behavioural change to an
  existing surface (feat-001's validation envelope → `validation.failed` + violations), records the
  `notebox-web` grep-check that it does not key on `VALIDATION_FAILED`, and drops the incorrect
  "`ApiExceptionMapper` reused for validation" implication.
- **F4 — RESOLVED.** `AnnotationTypeService.toField` (`AnnotationTypeService.java:110–112`) now guards
  `setNumberBounds` with `if (fieldType == FieldType.NUMBER)`; bounds on a non-Number field are ignored, so
  the "meaningful only for NUMBER" invariant in `data-model.md` now holds.
- **F5 — RESOLVED.** The FR-07 Gherkin no longer over-promises a server-side thumbnail: `spec.md` now reads
  "the response carries the size metadata the client needs to render a thumbnail," with a comment that
  downscaling is a `notebox-web` concern (AD-04). Matches the contract.
- **F6 — OPEN, non-blocking (low).** No negative test proves a *failed* delete writes no `AuditLog` row.
  The atomicity holds by construction (both writes in one `@Transactional` `delete` method), and the
  existing test asserts type-absence *and* exactly one audit row on the happy path. Left as a backlog test;
  not a defect. Suggested backlog item: add a rollback test that forces the delete to fail and asserts
  `auditLog.countForTarget(id) == 0`.

---

## Check-by-check results (re-run against `268fbe2`)

**1. Traceability — PASS.** All 23 Gherkin scenarios map to executable tests; every FR/BR to code (FR-01 →
resource+service tests; FR-02/BR-04 → `ConstraintsTest`/`FieldTypeTest`; FR-03 → constraints + repository
ordered options/colour; FR-07 → image service/resource tests; NFR-06 → `OpenApiCoverageTest`; BR-05/C-10 →
`AnnotationTypeDeleteTest`; NFR-01/C-01 → cross-tenant tests; C-02 → 401 tests). The unified validation
contract now has its own guard, `ValidationErrorContractTest`. No broken links.

**2. Scenario honesty — PASS (F6 note).** Tests fail on regression: the delete test asserts type-absence
*and* audit-row count; isolation tests use two real tenants and assert the foreign tenant gets empty/404;
constraint tests assert the *specific* violation key; the new F1 guard asserts `400` (not merely "an
error"), so it catches a 500 reappearing. The only remaining honesty gap is F6 (no failed-delete negative
test) — non-blocking.

**3. Constitution — PASS.** BR-03/04/05 held. AD-01/02/03/04/07/09/11 held (grep-clean: no domain→infra/api
imports; no `EntityManager`/query outside `infrastructure/`; every tenant-owned entity — `AnnotationType`,
`Image`, `AuditLog` — via `TenantScopedRepository`, children with no `tenant_id`/repo; no `byte[]` in DTOs,
bytes only from the binary endpoint; `@Transactional` writes; mapping only in `@Provider` mappers). §Errors
is now satisfied on the feat-001 path too — no 500, no stack-trace leak; one wire shape (`validation.failed`
envelope) app-wide.

**4. Compliance — PASS.** Every `applies` item has its evidence: C-01 cross-tenant tests; C-02 401 tests;
C-07 content-type + size validation and correct binary content-type on serve; C-09 message-coverage test
(all 14 feat-003 keys in en+pt); C-10 audit-on-delete same-tx test. `not applicable` items justified.

**5. Scope — PASS (documented deviation).** The one out-of-plan change — deletion of feat-001's
`ValidationExceptionMapper` — is now recorded in `plan.md` with rationale, mitigation, consumer impact
(grep-checked) and OQ-16 linkage. The defensive mapper means the deviation no longer changes feat-001's
availability, only its validation-error envelope (documented). No other unplanned files changed; the T-04/07
DTO shuffle and `workflow.json` status reconciliation remain harmless bookkeeping.

**6. Open Questions — PASS.** OQ-14 and OQ-15 correctly deferred and documented; the OQ-15 Secret flag ships
as declaration-only and is verified — declared (`TypeField.secret`), persisted (`V2 … secret BIT(1) NOT NULL
DEFAULT 0`), validated to TEXT/FREE_TEXT (`@SecretAllowedForFieldType`), defaulted false — with no crypto or
reveal code leaked in. OQ-16 is no longer actioned by silent assumption: the plan records the forced subset
taken (mapper unification + defensive resolution) and keeps the full feat-001 realignment deferred.

## Non-blocking items to carry forward
- **OQ-16 (existing):** migrate feat-001's `LoginRequest`/`UserPatch` constraints to dot-namespaced i18n
  keys so their per-field violations localize to pt (currently raw English via the defensive fallback).
- **Backlog (F6):** add a failed-delete negative test asserting no `AuditLog` row is written on rollback.
- **Backlog (F4 follow-on, optional):** if desired, reject rather than silently ignore Number bounds sent on
  a non-Number field — no spec scenario requires it, so left as looseness.

## GitHub
On this pass verdict, the audit issue comment (`./bin/wf github comment feat-003-annotation-types.audit
--event audit`) posts publicly — **not run here; recommend the coordinator run it** (public post requires an
explicit go-ahead). Nothing merges/closes at audit; publish, PR and human approval remain the `publish`/
`review` steps.
