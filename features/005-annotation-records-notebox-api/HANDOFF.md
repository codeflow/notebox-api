# HANDOFF — feat-005 annotation records (resume here)

**Updated:** 2026-08-04 · **State:** audit R2 remediation (T-09…T-15) implemented; awaiting full
verify + audit round 3. **PR [#6](https://github.com/codeflow/notebox-api/pull/6) is DRAFT — do not merge.**

## How to resume

```bash
cd notebox-api && claude      # the hook injects the pipeline automatically
/wf-status                     # see the state
/wf-next                       # continues at the next eligible step
```

⚠️ **`mvn -B verify` requires Docker running** (MySQL via Testcontainers/Dev Services). Without it,
`@QuarkusTest` fails with *"Could not find a valid Docker environment"* — not a code defect.
`open -a Docker`, wait ~5 s; a full verify takes ~10 min. GitHub CI does not depend on local Docker.

## Where things stand

- **Round 1 (2026-07-25, verdict fail):** F1–F14 — all behavioural findings **fixed and empirically
  verified** by the R2 audit's HTTP probes (no 500 envelopes; PUT preserves secrets; identical type PUT
  preserves field/option identity and every value; guards 409 with machine codes).
- **Spec v2 approved (2026-08-03/04):** 30 scenarios. Human decisions **OQ-17** (type field edits:
  preserve unchanged/identical; block removal/retype with records; adds allowed) and **OQ-18**
  (Secret-flag flip blocked while the field has values) — recorded in the catalog, PRD §3.1/§8, spec.
- **Round 2 (2026-08-04, verdict fail — narrow):** R2-01…R2-14 in `audit.md` (Round 2 section, top of
  file). Plan Addendum v2 + tasks T-09…T-15 were approved and **implemented**:
  duplicate-name-safe FIFO field matching (R2-02), wire 409+code tests for all three guards with
  post-state re-reads (R2-04/06), field-identity pinning + DTO-path reads in preservation tests (R2-03),
  who/when audit asserts (R2-05), `values:[null]` → 400 `annotation.record.value.required` (R2-07),
  accept-side boundary tests, distinct OpenAPI path asserts, `optionIds:[null]` guard test, C-12
  stored-bytes assertion, Javadoc fix, this file's rewrite (R2-08…R2-14).

## What remains

1. Full `mvn -B verify` green after T-09…T-15 (last full run: 122/122 before T-09).
2. Re-run the **audit** (round 3, opus/xhigh) → on pass, `catalogs/epics.md` → delivered, GitHub
   comment, then **publish** (take PR #6 out of draft) and **review**.
3. **All work is uncommitted** on `feature/annotation-records` — a queue of per-task commit proposals
   awaits the human's confirmation in the session (or "commit as you go").
4. Out of scope, tracked separately: dead `%prod.quarkus.hibernate-orm.schema-management.strategy`
   config key (unrecognized in Quarkus 3.15.1 — prod runs without schema validation; task chip open);
   type PUT returns `fields[].id = null` (feat-003).
