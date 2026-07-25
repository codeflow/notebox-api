# Tasks — Annotation records (feat-005, US-2.1)

> Decomposition of the approved [plan.md](plan.md) / [contracts/rest-api.md](contracts/rest-api.md) /
> [data-model.md](data-model.md). Each task is one outcome, ships its test, and cites the Gherkin
> scenario(s) it makes pass. Error handling follows constitution 03-code-standards §Errors: **specific
> domain exceptions** for business/stateful errors, **Bean Validation** for input shape, **dot-namespaced
> i18n keys** in en+pt. Ordered by dependency, then risk-first. Root package `com.notebox.api`.

- [x] **T-01 · Persistence schema (V3 migration) + record/value entities + tenant-scoped repository**
      - files: `resources/db/migration/V3__annotation_records.sql`, `domain/AnnotationRecord.java`, `domain/AnnotationValue.java`, `infrastructure/persistence/AnnotationRecordRepository.java`, `infrastructure/persistence/AnnotationRecordRepositoryTest.java`
      - covers: BR-03, FR-04, AD-03 · scenarios: "Create a record with values for defined fields" (persistence side), "A member cannot read, update, or delete another tenant's record" (repo-level)
      - notes: `AnnotationRecord implements TenantOwned` (own `tenant_id`); `AnnotationValue` aggregate-internal (no tenant_id), one-of typed columns + `@ElementCollection` selected options; `@OneToMany(cascade=ALL, orphanRemoval=true)`; repo `extends TenantScopedRepository<AnnotationRecord>` with `existsByType(typeId)` + scoped `remove`; test fakes tenant via `@InjectMock TenantContext` (mirror `TenantScopedRepositoryTest`)
      - depends: — · parallel: no  *(defines the schema — risk-first)*
      - verify: `./mvnw test -Dtest=AnnotationRecordRepositoryTest`

- [x] **T-02 · Secret cipher port + AES-256-GCM impl + versioned-key config**
      - files: `application/crypto/SecretValueCipher.java`, `application/crypto/EncryptedValue.java`, `infrastructure/security/AesGcmSecretValueCipher.java`, `resources/application.properties` (crypto keyring), `.env.example`, `infrastructure/security/AesGcmSecretValueCipherTest.java`
      - covers: FR-18, BR-10, AD-14, C-05, C-12 · scenario: "A secret value is stored as ciphertext, never cleartext" (encrypt/decrypt round-trip; ciphertext ≠ plaintext; IV + key-version present)
      - notes: AES-256-GCM, fresh 12-byte IV/value, 128-bit tag; keyring `notebox.crypto.keys.<v>` + `active-key-version`, dev/test default + `%prod` env override (mirror JWT-key pattern); `javax.crypto` used only under `infrastructure/`; fail fast if active key missing
      - depends: — · parallel: yes
      - verify: `./mvnw test -Dtest=AesGcmSecretValueCipherTest`

- [x] **T-03 · Domain exceptions + `ErrorCategory.FORBIDDEN` (403) + mapper + i18n keys (en/pt)**
      - files: `domain/error/ErrorCategory.java` (+FORBIDDEN), `domain/error/AnnotationRecordNotFoundException.java`, `domain/error/SecretRevealForbiddenException.java`, `domain/error/AnnotationTypeHasRecordsException.java`, `api/error/DomainExceptionMapper.java` (+403), `resources/messages.properties`, `resources/messages_pt.properties`, `infrastructure/i18n/AnnotationRecordMessageCoverageTest.java`
      - covers: constitution §Errors, C-09 · underpins the reveal-forbidden, record-not-found, type-has-records, and value-conformance keys
      - notes: keys `annotation.record.*` + `annotation.type.has_records`; mapper adds `FORBIDDEN→403`; coverage test asserts every new key in **en and pt**
      - depends: — · parallel: yes
      - verify: `./mvnw test -Dtest=AnnotationRecordMessageCoverageTest`

- [x] **T-04 · `AnnotationRecordService` — create/read/update with type-conformance validation + secret encryption/masking**
      - files: `application/annotation/AnnotationRecordService.java`, `application/annotation/AnnotationRecordServiceTest.java`
      - covers: FR-04, BR-03, FR-18 (encrypt on write / mask on read) · scenarios: "Reject a value for a field the type does not define", "Reject a value whose data does not match the field type", "Reject a Number value outside the field bounds", "Choice value must reference a predefined option", "Multiple choice accepts several predefined options", "Update changes a record's values", "Ordinary read masks a secret value"
      - notes: loads type via `AnnotationTypeRepository.findByIdInTenant`; dispatch on `fieldType`; encrypt Secret values via `SecretValueCipher`; tenant only from `TenantContext`; keys `annotation.record.field.unknown`, `…value.type_mismatch`, `…value.number.out_of_bounds`, `…value.option.unknown`, `…value.image.not_found`
      - depends: T-01, T-02, T-03 · parallel: no
      - verify: `./mvnw test -Dtest=AnnotationRecordServiceTest`

- [x] **T-05 · Delete a record — explicit, irreversible, audited**
      - files: `application/annotation/AnnotationRecordService.java` (delete), `application/annotation/AnnotationRecordDeleteTest.java`
      - covers: FR-06, BR-05, C-10 · scenario: "Delete removes the record and audits it"
      - notes: re-fetch via `findByIdInTenant` (404 `annotation.record.not_found`); scoped `remove`; `AuditLog(ACTION_RECORD_DELETED, TARGET_ANNOTATION_RECORD, id)` in the same `@Transactional`
      - depends: T-04 · parallel: no  *(same service file)*
      - verify: `./mvnw test -Dtest=AnnotationRecordDeleteTest`

- [x] **T-06 · Reveal a secret value — ADMIN-gated, decrypts, audited**
      - files: `application/annotation/AnnotationRecordService.java` (reveal), `application/annotation/AnnotationRecordRevealTest.java`
      - covers: FR-18, BR-10, C-03, C-10, C-12 · scenarios: "Reveal returns cleartext to the elevated role and is audited", "Reveal is denied to a plain member"
      - notes: require `tenantContext.role()==Role.ADMIN` else `SecretRevealForbiddenException` (403); field must be Secret + hold a value (else `annotation.record.reveal.not_secret`, 400); decrypt via cipher; `AuditLog(ACTION_SECRET_REVEALED, …)`
      - depends: T-02, T-03, T-04 · parallel: no  *(same service file)*
      - verify: `./mvnw test -Dtest=AnnotationRecordRevealTest`

- [x] **T-07 · Type-delete guard (OQ-14) in `AnnotationTypeService`**
      - files: `application/annotation/AnnotationTypeService.java` (guard), `application/annotation/AnnotationTypeDeleteGuardTest.java`
      - covers: OQ-14, BR-05 · scenarios: "Deleting a type that owns records is rejected", "Deleting a type with no records still succeeds"
      - notes: before remove, `if (annotationRecordRepository.existsByType(id)) throw new AnnotationTypeHasRecordsException()` (409 `annotation.type.has_records`); empty-type path unchanged (feat-003 behaviour)
      - depends: T-01, T-03 · parallel: yes  *(different file from the records service)*
      - verify: `./mvnw test -Dtest=AnnotationTypeDeleteGuardTest`

- [ ] **T-08 · REST resource + DTOs (input/dto/reveal) + masking + OpenAPI + HTTP/tenant-isolation e2e**
      - files: `api/AnnotationRecordResource.java`, `api/dto/AnnotationRecordInput.java`, `api/dto/AnnotationValueInput.java`, `api/dto/AnnotationRecordDto.java`, `api/dto/AnnotationValueDto.java`, `api/dto/RevealResponse.java`, `api/AnnotationRecordResourceTest.java`, `api/OpenApiCoverageTest.java`
      - covers: FR-04, FR-06, FR-18, NFR-06, NFR-01, C-01, C-02, C-03 · scenarios: "Reject a record with no name", "Ordinary read masks a secret value" (wire), reveal (admin ok / member forbidden), "A member cannot create a record for another tenant's type", "A member cannot read, update, or delete another tenant's record"
      - notes: `@Path("/annotation-records")`, class `@Authenticated`, methods `@Transactional` + `@Valid`; `from(record,type)` orders values by field position, masks secrets; `RevealResponse` cleartext only from `…/reveal`; tokens via `TestTokens`, tenants via `TestData`; extend `OpenApiCoverageTest` for the 5 endpoints
      - depends: T-04, T-05, T-06 · parallel: no
      - verify: `./mvnw test -Dtest=AnnotationRecordResourceTest,OpenApiCoverageTest`

## Coverage & sequencing
- **17/17 spec scenarios covered:** FR-04 (T-01/T-04/T-08), FR-06 (T-05), FR-18 (T-02/T-04/T-06/T-08),
  OQ-14 (T-07), tenant isolation (T-01/T-08).
- **Dependency chain:** T-01, T-02, T-03 have no deps → T-04 (needs all three) → T-05, T-06 (serial, same
  service file) → T-08. T-07 branches off T-01+T-03.
- **Parallelisable (worktree):** T-02, T-03, T-07 (file-disjoint). T-01, T-04, T-05, T-06, T-08 run serially.
- **Uncovered scenarios:** none.
