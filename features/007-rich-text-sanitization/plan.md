# Plan — Rich Free-text values sanitized on input and output

## Origin
Spec: [spec.md](spec.md) (approved 2026-08-12) · US-2.1 · FR-04 · C-08 · OQ-19 decision record.
The dialect is the one feat-006 froze (`notebox-web/lib/annotationRecords/sanitize.ts` and its
data-model allow-list table); this plan makes the API its authoritative enforcer.

## Approach (three sentences)
Introduce a `RichTextSanitizer` **application port** with a jsoup-backed implementation under
`infrastructure/` (mirroring the `SecretValueCipher` port pattern), whose Safelist is the exact
feat-006 dialect plus a post-pass for the two rules a Safelist cannot express (colour-only `span`
styles, http/https-only `a[href]` with forced `rel`). Invoke it at **two seams**: on write inside
`AnnotationRecordService.applyText` for non-secret `FREE_TEXT` values (the sanitized form is what is
bounds-checked and stored), and on read inside DTO assembly — threaded as a parameter so the managed
entity is **never mutated in a transaction** — so legacy rows are served inert without any migration.
Byte-identity with the client dialect is not assumed but **pinned**: the test corpus imports the
client's canonical dialect document and hostile fixtures verbatim, and the round-trip must be
byte-equal for dialect-clean input and idempotent for everything.

## Design

### 1. Port + implementation
- `application/content/RichTextSanitizer.java` — `String sanitize(String html)`; the javadoc carries
  the dialect table as the contract.
- `infrastructure/content/JsoupRichTextSanitizer.java` — `@ApplicationScoped`. jsoup `Safelist`:
  - tags: `p, br, strong, em, u, s, span, ol, ul, li, blockquote, a, code, pre, img`
  - attributes: `span[style]`, `a[href]`, `pre[data-language]`, `img[data-image-id, alt]`
  - protocols: `a[href]` → `http, https`
  - enforced: `a[rel]="noopener noreferrer"`
  - output settings: `prettyPrint(false)`, HTML syntax, minimal entity escaping — the serialization
    knobs that make jsoup's output shape match the browser's `innerHTML` shape the client emits.
- **Post-pass** (jsoup `NodeVisitor` after clean): `span[style]` — if the style is exactly one colour
  declaration, keep it **verbatim** (byte-identity for dialect input); otherwise extract the colour
  (drop everything else) or drop the attribute when no colour exists. Script/style element **bodies**
  are dropped by parsing (jsoup treats their content as data, removed with the element).
- **New dependency:** `org.jsoup:jsoup` (no transitive deps). It lives only under `infrastructure/`
  (03-standards layering, same rule as `javax.crypto` and BouncyCastle).

### 2. Write seam — `AnnotationRecordService.applyText` (`:226` dispatch)
For `FREE_TEXT` fields with `secret == false`: `text = sanitizer.sanitize(text)` **before** the
length check and persistence. TEXT fields and secret values bypass the sanitizer entirely (spec pins
both). Create and update share `applyText`, so both paths are covered by one change.

### 3. Read seam — DTO assembly, entity untouched
`AnnotationRecordDto.from(record, type)` gains a `RichTextSanitizer` parameter, threaded to
`AnnotationValueDto.from(value, field, sanitizer)`, which sanitizes the text of non-secret
`FREE_TEXT` values **into the DTO only**. The resource injects the sanitizer and passes it at its
three `from` call sites. Rationale for a parameter over a CDI mapper bean: `from` is already the
established static-assembly idiom (feat-005); adding one parameter keeps the diff minimal and makes
the no-entity-mutation property structural — there is no setter call to get wrong inside the
`@Transactional` read (a mutated managed entity would silently UPDATE the row on flush).

### 4. Tests (the corpus is shared with the client)
- `JsoupRichTextSanitizerTest` — unit corpus mirroring `sanitize.test.ts` case-for-case: script
  dropped with body, event handlers stripped, `javascript:`/`data:` hrefs removed, `img src`
  stripped with `data-image-id`/`alt` kept, arbitrary `data-*` dropped, unknown tags dropped keeping
  text, colour-only style filtering, **canonical dialect document byte-identity**, idempotence.
- `AnnotationRecordServiceTest` (extend) — write path: hostile create/update stores + returns the
  sanitized form; TEXT stays verbatim; secret untouched.
- `AnnotationRecordResourceTest` (extend) — wire: hostile POST → 201 with sanitized body; **legacy
  row** planted via native SQL → GET returns it inert (the read-seam proof).
- Bounds: an over-long sanitized value still hits `annotation.record.value.too_long` (existing key —
  no new i18n).

## Alternatives rejected
- **OWASP java-html-sanitizer** — its serializer aggressively re-encodes entities (`'` → `&#39;`,
  attribute reordering), which breaks the byte-identity criterion for dialect-clean input; fighting
  the serializer costs more than jsoup's post-pass.
- **Regex stripping** — unsound over HTML (nesting, entity obfuscation, mXSS); the classic
  sanitization mistake; a parser-based pipeline is non-negotiable for C-08.
- **Reject with 400 instead of sanitizing** — ruled out by the approved spec (would retroactively
  break callers holding non-dialect content).
- **Write-side only + one-off migration** — leaves the exposure window until migration runs, and the
  migration is one-way; the read seam keeps the safety invariant with zero data risk. Migration
  stays available later as pure cleanup.

## Blast radius
- **New:** `application/content/RichTextSanitizer.java`,
  `infrastructure/content/JsoupRichTextSanitizer.java`, `JsoupRichTextSanitizerTest`, jsoup in
  `pom.xml`.
- **Modified:** `AnnotationRecordService` (`applyText`), `AnnotationRecordDto` /
  `AnnotationValueDto` (sanitizer parameter), `AnnotationRecordResource` (3 call sites),
  `AnnotationRecordServiceTest`, `AnnotationRecordResourceTest`.
- **Consumers:** notebox-web — no wire-shape change; the returned value's *content* is now always
  dialect-clean, which the client's own render sanitizer already assumes. feat-006 needs no change.
- **Untouched (spec-pinned):** type endpoints, masking/reveal/audit, encryption, images, bounds keys.

## Risk
- **jsoup serialization ≠ client serialization** (entity escaping, attribute order, void-element
  shape). *Signal:* the byte-identity test on the canonical dialect document imported verbatim from
  the client corpus — if jsoup cannot be configured to match, the fallback (parse-don't-reserialize:
  walk and strip on the original string) is a bounded rewrite of the impl behind the same port and
  tests.
- **Entity double-escaping breaking idempotence** (`&amp;` → `&amp;amp;`). *Signal:* the idempotence
  test.
- **Accidental entity mutation on read.** *Signal:* structural (no setter exists on the read path) +
  an assertion that a re-read after GET returns the raw stored value at the SQL level.

## Reversibility
| Decision | Kind |
|---|---|
| Stored value = sanitized form (original input discarded) | **one-way** (by design — C-08) |
| jsoup as the sanitizer | reversible (behind the port) |
| Read-seam parameter threading | reversible |
| No data migration | reversible (can be added later as cleanup) |
