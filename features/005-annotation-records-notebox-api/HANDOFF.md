# HANDOFF — feat-005 annotation records (resume here)

**Updated:** 2026-08-11 · **State:** **DONE — merged into `develop`.** Audit Round 4 verdict **pass**,
CI green, PR [#6](https://github.com/codeflow/notebox-api/pull/6) squash-merged as `0f056d7`
(2026-08-11). Awaiting promotion to `main` (`/wf-promote`). The API side of US-2.1 is delivered; the
story itself is not — feat-006 (web) has not started.

## How to resume

```bash
cd notebox-api && claude      # the hook injects the pipeline automatically
/wf-status                     # see the state
/wf-next                       # continues at the next eligible step
```

`wf next` now points at **feat-006-annotation-records-notebox-web.spec** (opus/high, human approval),
which runs in the `notebox-web` satellite.

⚠️ **`mvn -B verify` requires Docker running** (MySQL via Testcontainers/Dev Services). Without it,
`@QuarkusTest` fails with *"Could not find a valid Docker environment"* — not a code defect.
`open -a Docker`, wait ~5 s; a full verify takes ~3–10 min. GitHub CI does not depend on local Docker.

## What shipped

Three audit rounds of remediation, then a pass:

- **Round 1 (2026-07-25, fail):** F1–F14 — all behavioural findings fixed and empirically re-verified
  over HTTP by the later rounds (no 500 envelopes; PUT preserves secrets; identical type PUT preserves
  field/option identity and every value; guards 409 with machine codes).
- **Spec v2 (2026-08-03/04):** 30 scenarios. Human decisions **OQ-17** (type field edits: preserve
  unchanged/identical; block removal/retype with records; adds allowed) and **OQ-18** (Secret-flag flip
  blocked while the field holds values) — recorded in the catalog, PRD §3.1/§8 and spec.
- **Round 2 (2026-08-04, fail — narrow):** R2-01…R2-14, closed by T-09…T-15 (commit `7bec16c`):
  duplicate-name-safe FIFO field matching, wire 409+code tests for all three guards with post-state
  re-reads, field-identity pinning + DTO-path reads in preservation tests, who/when audit asserts,
  `values:[null]` → 400 `annotation.record.value.required`, accept-side boundary tests, distinct
  OpenAPI path asserts, `optionIds:[null]` guard, C-12 stored-bytes assertion, Javadoc, this file.
- **Round 3 (2026-08-04, fail):** R3-01 — `updateOptions` still carried the R2-02 `putIfAbsent`
  pattern one method below the one T-09 fixed, so duplicate option labels made an identical resend
  orphan an option and dangle the record's selection. R3-02 — `tasks.md` checkboxes lagged.
- **Round 4 (2026-08-11, PASS):** both closed by T-16 (commit `0e0acd3`) — options now match through
  per-label FIFO queues. Proven twice: an HTTP replay of Round 3's exact input keeps both option ids
  byte-identical with the selection resolvable, and reverting the fix makes the new test fail.
  `mvn -B verify` and CI both green: **133 tests, 0 failures, 0 errors, 0 skipped.**

## Known, deliberately deferred

- **Dropping an option label still leaves a selecting record's value dangling** — feat-003 replace
  semantics, deferred by the human's 2026-08-03 decision (spec §out-of-scope). Now pinned by a
  characterization test in `AnnotationTypeEditGuardTest`, so changing it has to be deliberate.
- **`lint` CI signal is a placeholder** that echoes and exits zero — a green check that checks nothing.
  Wire it (`wf harness signal lint "<cmd>"`) or drop it from `harness.signals` and regenerate. Human's
  call; worth settling before promotion to `main`.
- Dead `%prod.quarkus.hibernate-orm.schema-management.strategy` key (unrecognized in Quarkus 3.15.1 —
  prod runs without schema validation; task chip open).
- Type PUT returns `fields[].id` / `options[].id` as `null` for newly created children (feat-003).
- The plan's reversibility table lists only the field-level FIFO row, not its option-level twin
  (Round 4 observation, non-blocking).

## What feat-006 codes against

The wire contract this feature froze: the three 409 guard codes
(`annotation.type.has_records`, `annotation.type.field.has_records`,
`annotation.type.field.secret_flip.has_values`), `clearSecret`, the masked-echo no-op on PUT, and the
ADMIN-gated reveal endpoint. Contract detail in [contracts/rest-api.md](contracts/rest-api.md).
