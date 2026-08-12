# Audit — Record listing (feat-008, US-2.2 / FR-05) — ROUND 1

**Step:** `feat-008-annotation-listing-notebox-api.audit` · **Date:** 2026-08-12
**Method.** Inline opus/xhigh adversarial audit of the T-01…T-03 implementation (3 commits,
`d1a2d51`→`1cc411f`, on `feature/annotation-listing`): traceability over the 11 spec scenarios into the
real tests, scenario-honesty reads, constitution/layering greps, compliance evidence, scope diff vs
plan blast radius, OQ check. `mvn -B verify` green on the full suite: **161 tests, 0 failures** — which
is also the declared tripwire for the global batch-fetch config (all 153 pre-existing tests held their
lazy-loading assumptions).

## Verdict: **PASS with findings** (2 LOW, non-blocking — logged, no reopen)

### Findings (backlog, not gate)
- **L-01 — LOW · The `page` parameter's `@Min(0)` reuses the size message key.**
  `AnnotationRecordResource:65` — a request with `page=-1` answers 400 with
  *"The page size must be between 1 and 200"*, a misleading message for a page violation. No spec
  scenario covers negative pages (the spec pinned only the `typeId` and `size` messages), so nothing
  green becomes false — but the envelope's C-09 spirit is a message that names its own violation.
  *Fix when convenient:* a dedicated `annotation.record.list.page.out_of_bounds` key (en+pt +
  coverage row) — a five-line change safe to ride any future hub commit.
- **L-02 — LOW · Plan §Blast radius omitted `AnnotationRecordService`.** The resource correctly goes
  through two new service pass-throughs (`listByType`, `countByType`) rather than touching the
  repository directly — architecture-right, but the plan's Modified list missed the file. Same class
  as the feat-007 observation; two occurrences now — the next plan should enumerate the service layer
  whenever a resource method is added.

### Check-by-check
1. **Traceability — PASS.** 11/11 scenarios map to tests that exercise them: projection + the BR-09
   pair read the same record both ways in one test; masking, the sanitized legacy row (native SQL
   plant replayed on the listing path), default-50/total, size-500 → 400 + key, empty page, order,
   isolation, missing-typeId — plus the repo-level order/scoping/arithmetic tests.
2. **Scenario honesty — PASS.** The strong guards: the order test creates rows in one transaction so
   `createdAt` collides and only the id tiebreak can pass it; the 404 indistinguishability is asserted
   **byte-level** modulo correlation id, not just code-equal; the masked-row test asserts no cleartext
   across the whole response body. A fixture assumption (FREE_TEXT visible by default) was caught by
   the D2 defaults during implementation — the projection excluded what it should, and the fixture now
   declares visibility explicitly; recorded as evidence the projection logic, not the test, was
   authoritative.
3. **Constitution — PASS.** Both new queries tenant-scoped in the established pattern (AD-03/C-01);
   resource → service → repository layering restored by the pass-throughs; no new dependency; Javadoc
   on the public surface, English identifiers.
4. **Compliance — PASS.** C-08 on the new exit path (legacy-row listing test); C-09 both new keys in
   en+pt with coverage rows (L-01 notes a *reused* key, not a missing one); C-12/FR-18 masked rows
   with the whole-body assert; C-01 the byte-indistinguishability test.
5. **Scope — PASS (with L-02).** Diff matches plan §Blast radius plus the service pass-throughs and
   the OpenAPI marker assert — both within the plan's intent, one unlisted (L-02).
6. **Open Questions — PASS.** OQ-20 was decided by the human before plan; the order it fixed is
   pinned by tests at both levels. None pending, none closed by assumption.

### Gate
`audit_pass` **opens** (findings logged as backlog, per the pass-with-findings protocol). Next:
`publish` (push + CI), then `review` (PR → develop). feat-009 (the grid) can spec against
[contracts/listing.md](contracts/listing.md) as soon as this merges.
