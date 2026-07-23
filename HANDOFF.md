# HANDOFF — the project

> Session continuity. What's done, what's in flight, and how to resume. Language: English.
> **Updated:** 2026-07-23

## Current state
- **notebox-api** (Java 17 / Quarkus / Jakarta EE / MySQL) + satellite **notebox-web** (react/next).
- On `develop`, **awaiting promotion to `main`** (`/wf-promote`):
  - **US-6.1** identity & multi-tenancy — feat-001 (api) + feat-002 (web).
  - **US-1.1** annotation types (api) — feat-003, merged via PR #4 (audit pass-with-findings).
- `main` is still at the initial scaffold commit; three features sit on `develop` unpromoted.

## In flight
- **feat-004-annotation-types-web** (US-1.1, notebox-web) — next up; `wf next` → `feat-004-annotation-types-web.spec`. Consumes feat-003's `features/003-annotation-types/contracts/rest-api.md`.

## How to resume
1. Read `CLAUDE.md` (rules + working language en) and render the pipeline (`./bin/wf status`).
2. Check `catalogs/open-questions.md` and `ROADMAP.md`.
3. Continue the pipeline with `/wf-next`.

## Gotchas / decisions
- **Validation error handling is unified** on `api/error/ConstraintViolationMapper` (envelope `validation.failed` + per-field `violations[]`, dot-namespaced i18n keys). feat-001's `ValidationExceptionMapper` was **deleted** to resolve a two-mapper collision. **Do not reintroduce a second `ExceptionMapper<ConstraintViolationException>`.** The mapper degrades gracefully on a non-catalog message (feat-001's DTOs still use Bean Validation defaults — full pt localization is **OQ-16**).
- **Error convention (constitution 03 §Errors):** business/stateful errors = specific `domain/error/*Exception` (extend `DomainException`, mapped by `DomainExceptionMapper`); input shape = Bean Validation, custom constraints preferred; messages = dot-namespaced keys in en+pt.
- **Secret fields (OQ-15):** feat-003 ships the type-definition flag only; value encryption-at-rest + role-gated audited reveal + UI masking are deferred to US-2.1 and the web features, and need a PRD v2 + constitution AD/compliance/BR first.
- **Open:** OQ-14 (type delete with records → US-2.1), OQ-15 (Secret crypto/keys), OQ-16 (feat-001 realignment).
