# Plan — Paginated record listing with visible-field column projection

## Origin
Spec: [spec.md](spec.md) (approved 2026-08-12, OQ-20 decided: newest first) · US-2.2 · FR-05 ·
NFR-08 · BR-09. Consumes the shipped feat-005/007 value semantics; feeds feat-009's grid.

## Approach (three sentences)
Add `GET /api/annotation-records?typeId=…&page=…&size=…` to the existing resource, backed by two new
tenant-scoped repository queries (a page ordered `createdAt DESC, id DESC` and a count), returning a
new `PageDto` envelope whose items are assembled by a **listing variant of the existing DTO factory**
that filters to visible-for-viewing fields — so masking and rich-text sanitization are inherited from
the exact code feat-005/007 already proved, not reimplemented. Query-parameter validation follows the
established Bean-Validation posture (`typeId` required, `1 ≤ size ≤ 200`) with two new dot-namespaced
keys in en+pt. The classic join-fetch-plus-pagination trap is avoided by not join-fetching at all:
Hibernate's `default_batch_fetch_size` (one new config line) collapses the would-be N+1 on values and
their option collections into batched IN-queries without touching pagination.

## Design

### 1. Wire (contracts/listing.md)
- `GET /annotation-records?typeId={uuid}&page={0+}&size={1..200}` — member, authenticated.
- Response `200 PageDto`: `{ "items": [AnnotationRecordDto…], "page": n, "size": n, "total": n }`.
  Items are the **existing `AnnotationRecordDto` shape** with `values` filtered to visible fields —
  the client already knows how to render it (feat-006), no second value model.
- Errors: missing `typeId` → 400 `annotation.record.list.type.required`; size out of bounds → 400
  `annotation.record.list.size.out_of_bounds` (both new keys, en+pt); unknown/foreign `typeId` →
  404 `annotation.type.not_found` (existing key — indistinguishability inherited from
  `findByIdInTenant`).
- Order: `createdAt DESC, id DESC` — documented in OpenAPI (OQ-20).

### 2. Repository — `AnnotationRecordRepository`
Two additions, both inside the tenant-scoped pattern (AD-03):
- `listByTypeInTenant(UUID typeId, int page, int size)` — `tenantId = :tenant and annotationTypeId
  = :type order by createdAt desc, id desc`, `setFirstResult/setMaxResults`.
- `countByTypeInTenant(UUID typeId)` — same predicate, `count(*)`.

### 3. Assembly — `AnnotationRecordDto.forListing(record, type, sanitizer)`
A sibling factory beside `from`, identical except it includes only `field.isVisibleForViewing()`
fields. Reuses `AnnotationValueDto.from(value, field, sanitizer)` untouched — masking (FR-18) and
sanitize-on-read (C-08) arrive for free on the new path, which is precisely what the spec's
masked-row and legacy-row scenarios verify. BR-09 lives in the factory's javadoc: projection is
presentation, never access control.

### 4. Resource — one new method on `AnnotationRecordResource`
`list(@QueryParam typeId, page, size)` mirroring `AnnotationTypeResource.list`'s idiom, but with the
NFR-08 bounds enforced via Bean Validation (`@NotNull`, `@Min(1) @Max(200)`) so violations flow
through the existing `ConstraintViolationMapper`. The type is loaded first (`typeService.get` → 404
path), then page + count + assembly. OpenAPI annotations + an `OpenApiCoverageTest` row (NFR-06).

### 5. Fetch strategy — the trap and its avoidance
`AnnotationValue` is a lazy collection and each value holds an `@ElementCollection` of option ids:
`join fetch` with pagination would force in-memory pagination (Hibernate reads the whole result set),
and nested collection fetches risk MultipleBagFetchException. Neither is worth it for a 50-row page.
Instead: `quarkus.hibernate-orm.unsupported-properties."hibernate.default_batch_fetch_size"=64`
(one config line) — the values of all page rows load in batched IN-queries (~3 queries per page
instead of 1+2×50). Reversible, global, and benefits the existing detail read too.

### 6. i18n + tests
- `annotation.record.list.type.required`, `annotation.record.list.size.out_of_bounds` in
  `messages.properties` + `messages_pt.properties` + `AnnotationRecordMessageCoverageTest` rows.
- Tests: repository (order + scoping + count), resource wire (all 11 spec scenarios — projection,
  BR-09 pair, masked row, sanitized legacy row via native SQL, page bounds, empty page, order,
  isolation, validation), OpenAPI row.

## Alternatives rejected
- **Join-fetch the values in the page query** — in-memory pagination + multiple-bag hazard; the
  batch-fetch config achieves the same round-trip economy without either.
- **A thin bespoke row DTO** (id + name + cell strings) — diverges from `AnnotationRecordDto`, and
  every value rule (masking, sanitization, option ids) would need a second implementation the audits
  would then have to re-verify; drift risk outweighs the payload savings at 50 rows.
- **Silently clamping size to 200** — the spec pinned rejection (uniform explicit-validation
  posture); clamping reshapes caller intent invisibly.
- **Offset-free cursor pagination** — better at scale, but NFR-08 asks for page/size and the grid's
  pager (screen 11) is page-numbered; cursors are a future story if volume demands.

## Blast radius
- **New:** `api/dto/PageDto.java`, `contracts/listing.md`, 2 i18n keys ×2 locales.
- **Modified:** `AnnotationRecordResource` (+1 method), `AnnotationRecordRepository` (+2 queries),
  `AnnotationRecordDto` (+`forListing`), `application.properties` (+1 batch-fetch line),
  `AnnotationRecordResourceTest`, `AnnotationRecordRepositoryTest`,
  `AnnotationRecordMessageCoverageTest`, `OpenApiCoverageTest`.
- **Test callers of changed signatures:** none — `forListing` is additive (the feat-007 audit
  observation applied).
- **Consumers:** feat-009 codes against `PageDto` + the filtered `AnnotationRecordDto` rows.
- **Untouched:** every shipped endpoint and behaviour; no schema change.

## Risk
- **Batch-fetch config changes global fetch behaviour.** *Signal:* the full existing suite (153
  tests) stays green — any lazy-loading assumption it breaks fails loudly there.
- **Order instability under equal timestamps.** *Signal:* the order test creates records fast enough
  to collide on `createdAt` precision; the id tiebreak assertion catches a missing secondary sort.
- **Projection drift from the detail view.** *Signal:* the BR-09 pair scenario reads the same record
  both ways in one test.

## Reversibility
| Decision | Kind |
|---|---|
| Wire contract (`PageDto` envelope, param names, order, error keys) | **one-way** (feat-009 codes against it) |
| `forListing` factory beside `from` | reversible |
| `default_batch_fetch_size=64` | reversible |
| Repository query pair | reversible |
