# Tasks — Record listing (feat-008, US-2.2 / FR-05)

> Decomposition of the approved [plan.md](plan.md) / [contracts/listing.md](contracts/listing.md) /
> [data-model.md](data-model.md), against the approved spec (11 scenarios). Hub feature —
> `mvn -B verify` is the signal. Risk-first: the batch-fetch config is global, so it lands first with
> the whole existing suite as its tripwire; then the query pair; the wire last.

- [x] **T-01 · Repository page + count queries, batch-fetch config**
      - files: `infrastructure/persistence/AnnotationRecordRepository.java` (`listByTypeInTenant`, `countByTypeInTenant`), `resources/application.properties` (batch-fetch line), `infrastructure/persistence/AnnotationRecordRepositoryTest.java`
      - covers: FR-05 (data side), NFR-08, OQ-20, AD-03/C-01 · scenarios: "Rows only ever contain records of the requested type and tenant" (repo level), "Rows are returned in the documented sort order" (incl. the equal-timestamp id tiebreak), page/count arithmetic
      - notes: order `createdAt desc, id desc`; both queries tenant-scoped in the established pattern; `hibernate.default_batch_fetch_size=64` via quarkus unsupported-properties — **global**: the plan's declared signal is the full existing suite staying green (checked at T-03's verify)
      - depends: — · parallel: no  *(the global-config risk lands with its tripwire ahead of everything)*
      - verify: `mvn -B test -Dtest=AnnotationRecordRepositoryTest`

- [ ] **T-02 · `forListing` projection factory + `PageDto` + resource method + i18n keys**
      - files: `api/dto/AnnotationRecordDto.java` (+`forListing`), `api/dto/PageDto.java`, `api/AnnotationRecordResource.java` (+`list`), `resources/messages.properties` + `messages_pt.properties` (2 keys), `infrastructure/i18n/AnnotationRecordMessageCoverageTest.java`
      - covers: FR-05, BR-09, NFR-08, C-09 · scenarios: projection shape (visible fields only, field order), the validation pair ("type id is required", "size above 200 rejected")
      - notes: `forListing` is additive (no signature change — feat-007 audit observation applied) and filters on `isVisibleForViewing`, reusing `AnnotationValueDto.from(..., sanitizer)` untouched; Bean Validation on the query params (`@NotNull`, `@Min(1) @Max(200)`) flowing through the existing mapper; BR-09's display-only nature stated in the factory javadoc
      - depends: T-01 · parallel: no
      - verify: `mvn -B test -Dtest=AnnotationRecordMessageCoverageTest`

- [ ] **T-03 · Wire tests — the 11 scenarios end to end + OpenAPI + full verify**
      - files: `api/AnnotationRecordResourceTest.java` (extend), `api/OpenApiCoverageTest.java` (+row)
      - covers: all 11 spec scenarios at the wire, NFR-06, C-08/C-12/FR-18 on the new path · scenarios: projection + BR-09 pair (same record, both reads, one test), masked secret row, **sanitized legacy row** (native SQL plant), default-50/total, size-500 → 400 + key, empty page beyond end, newest-first order, foreign-tenant 404 indistinguishability, missing typeId → 400
      - notes: closes with the hub's **full `mvn -B verify`** — which is also T-01's global-config tripwire (all 153 existing tests must stay green)
      - depends: T-01, T-02 · parallel: no
      - verify: `mvn -B verify`

## Coverage & sequencing
- **11/11 spec scenarios covered:** repo level (T-01: scoping, order, arithmetic) + wire level (T-03: everything), validation + projection assembly (T-02/T-03).
- **Dependency chain:** T-01 → T-02 → T-03. Serial — small feature, shared files.
- **Uncovered scenarios:** none.
