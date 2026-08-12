# Data model — Rich Free-text sanitization (feat-007)

**No schema change and no migration.** The feature changes what the existing
`annotation_value.text_value` column *holds* for non-secret `FREE_TEXT` fields, not its shape.

## Semantic change
| Column | Before | After |
|---|---|---|
| `annotation_value.text_value` (FREE_TEXT, non-secret) | verbatim caller input (may carry hostile markup) | the **sanitized dialect form** of the caller input |

## Invariants
- **INV-S1 · Stored form is dialect-clean (writes).** Every value written through the API for a
  non-secret `FREE_TEXT` field satisfies `sanitize(v) == v` at rest. (C-08 input half.)
- **INV-S2 · Served form is dialect-clean (reads).** Every `FREE_TEXT` value leaving the API equals
  `sanitize(stored)` — which, for rows predating this feature, may differ from what is at rest.
  (C-08 output half; legacy rows inert without migration.)
- **INV-S3 · Dialect fidelity.** For dialect-clean input, `sanitize` is the identity (byte-equal) —
  the property that keeps the web editor's own identity pin true end to end.
- **INV-S4 · Idempotence.** `sanitize(sanitize(v)) == sanitize(v)` for all input.
- **INV-S5 · Read path never mutates the entity.** Read-side sanitization exists only in DTO
  assembly; the managed entity is untouched, so a `@Transactional` read never flushes an UPDATE.
- **INV-S6 · Scope.** TEXT values and secret values are never passed through the sanitizer
  (spec-pinned scenarios).

## The dialect (shared contract)
The allow-list table in [spec.md](spec.md) §The dialect — byte-for-byte the client's
`notebox-web/lib/annotationRecords/sanitize.ts` configuration. A change to either side is a
contract change and must touch both features' test corpora.
