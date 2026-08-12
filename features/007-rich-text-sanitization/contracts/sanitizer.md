# Contract — RichTextSanitizer port (feat-007)

## Port (application → infrastructure)
```java
// application/content/RichTextSanitizer.java
public interface RichTextSanitizer {
    /** Returns the dialect-clean form of {@code html}: identity for dialect input, idempotent for all. */
    String sanitize(String html);
}
// impl: infrastructure/content/JsoupRichTextSanitizer (@ApplicationScoped)
```

## Invocation seams
| Seam | Where | Rule |
|---|---|---|
| Write | `AnnotationRecordService.applyText` | non-secret `FREE_TEXT` only; sanitize **before** the length check; sanitized form is stored and echoed |
| Read | `AnnotationRecordDto.from(record, type, sanitizer)` → `AnnotationValueDto.from(value, field, sanitizer)` | non-secret `FREE_TEXT` only; DTO-only — the entity is never mutated |

## Wire contract
**No shape change** to `features/005-annotation-records-notebox-api/contracts/rest-api.md` — same
endpoints, payloads, codes and keys. One semantic row is added to that contract's meaning:

> The `text` of a non-secret `FREE_TEXT` value is always the **sanitized dialect form** — on the
> response of the write that stored it and on every read, including rows stored before feat-007.

No new error condition exists: sanitization strips, never rejects (spec §Out of scope), so the
error table is unchanged and no i18n key is added (C-09 n/a).

## Dependency
`org.jsoup:jsoup` — infrastructure-only (03-standards layering, as with BouncyCastle/`javax.crypto`).
