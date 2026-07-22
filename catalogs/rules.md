# Catalog — Business Rules (BR) quick reference

> Quick-reference index of canonical Business Rules. Full text lives in
> `constitution/00-principles.md` — this table only links ids, never restates the rule. Language: English.

**Last sync with PRD:** v1 (2026-07-22)

| ID | Theme | Status | Derived FRs |
|----|-------|--------|-------------|
| BR-01 | Tenant data isolation | ✅ Confirmed | FR-17 (+ all, enforced) |
| BR-02 | Every entity belongs to exactly one tenant | ✅ Confirmed | FR-17 (+ all) |
| BR-03 | Annotations conform to their type | ✅ Confirmed | FR-01, FR-04, FR-05 |
| BR-04 | Field types are a closed set | ✅ Confirmed | FR-02, FR-03 |
| BR-05 | Destructive deletions are explicit and irreversible | ✅ Confirmed | FR-06 |
| BR-06 | Task status is derived, never assigned | ✅ Confirmed | FR-10, FR-11 |
| BR-07 | Task dates derive from subtasks | ✅ Confirmed | FR-12 |
| BR-08 | No user-facing text shown untranslated | ✅ Confirmed | FR-15, FR-16 |
| BR-09 | "Visible for viewing" is presentation, never access control | ✅ Confirmed | FR-05 |

## Rules
- For details of a BR, read `constitution/00-principles.md`.
- IDs immutable; deprecate, never reuse. Changes only via `/new-prd-version`.
