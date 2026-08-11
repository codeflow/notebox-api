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

- [x] **T-08 · REST resource + DTOs (input/dto/reveal) + masking + OpenAPI + HTTP/tenant-isolation e2e**
      - files: `api/AnnotationRecordResource.java`, `api/dto/AnnotationRecordInput.java`, `api/dto/AnnotationValueInput.java`, `api/dto/AnnotationRecordDto.java`, `api/dto/AnnotationValueDto.java`, `api/dto/RevealResponse.java`, `api/AnnotationRecordResourceTest.java`, `api/OpenApiCoverageTest.java`
      - covers: FR-04, FR-06, FR-18, NFR-06, NFR-01, C-01, C-02, C-03 · scenarios: "Reject a record with no name", "Ordinary read masks a secret value" (wire), reveal (admin ok / member forbidden), "A member cannot create a record for another tenant's type", "A member cannot read, update, or delete another tenant's record"
      - notes: `@Path("/annotation-records")`, class `@Authenticated`, methods `@Transactional` + `@Valid`; `from(record,type)` orders values by field position, masks secrets; `RevealResponse` cleartext only from `…/reveal`; tokens via `TestTokens`, tenants via `TestData`; extend `OpenApiCoverageTest` for the 5 endpoints
      - depends: T-04, T-05, T-06 · parallel: no
      - verify: `./mvnw test -Dtest=AnnotationRecordResourceTest,OpenApiCoverageTest`

## R2 remediation tasks (spec v2 · plan Addendum v2 · audit Round 2, 2026-08-04)

> T-01…T-08 absorbed the R1 remediation in their rework (F1–F4, F6, F7, F11, F13 and the OQ-17/OQ-18
> guards — see plan §A) and are **done**. The tasks below carry the R2 findings. Risk-first: T-09 is the
> only behavioural change.

- [x] **T-09 · Duplicate-name-safe field matching in the type PUT (R2-02)**
      - files: `application/annotation/AnnotationTypeService.java`, `application/annotation/AnnotationTypeEditGuardTest.java`
      - covers: OQ-17, BR-05, BR-03 · scenario: "Resending an identical type definition preserves every record's values" (duplicate-name edge — today it silently destroys the 2nd same-named field's values)
      - notes: replace the `putIfAbsent` name map with per-name FIFO queues (`Map<String, Deque<TypeField>>`) per plan §B; leftovers keep flowing into the existing has-records 409. Tests: `[X,X]` type — identical resend preserves BOTH identities/values; resend with one `X` → 409
      - depends: — · parallel: no  *(behavioural, most likely to invalidate the plan)*
      - verify: `mvn -B test -Dtest=AnnotationTypeEditGuardTest`

- [x] **T-10 · Guard wire contract: HTTP 409 + machine code for the three guards (R2-04)**
      - files: `api/AnnotationTypeResourceTest.java` (extend)
      - covers: OQ-14/17/18, C-09 · scenarios: "Deleting a type that owns records is rejected", "Removing a field is rejected while records exist", "Flagging a field Secret is rejected while it holds values" — the *rejected with error code* clauses at the wire
      - notes: assert status **409** and body `code` = `annotation.type.has_records` / `annotation.type.field.has_records` / `annotation.type.field.secret_flip.has_values` (no 409 is asserted anywhere in the suite today)
      - depends: T-09 · parallel: no *(same guard surface)*
      - verify: `mvn -B test -Dtest=AnnotationTypeResourceTest`

- [x] **T-11 · Preservation tests pin field identity and read through the DTO path (R2-03)**
      - files: `application/annotation/AnnotationTypeEditGuardTest.java`
      - covers: OQ-17 · scenarios: the three preservation scenarios ("identical resend", "rename", "add a field")
      - notes: capture field ids before the PUT and assert equality after; assert value survival via the wire read model, not the raw entity collection (orphans must not count as survivors)
      - depends: T-09 *(same file)* · parallel: no
      - verify: `mvn -B test -Dtest=AnnotationTypeEditGuardTest`

- [x] **T-12 · Audit entries assert who and when (R2-05)**
      - files: `application/annotation/AnnotationRecordDeleteTest.java`, `application/annotation/AnnotationRecordRevealTest.java`, `application/annotation/AnnotationRecordServiceTest.java`
      - covers: FR-06, FR-18, BR-05, BR-10, C-10 · scenarios: the three "an audit entry records who … and when" clauses (delete, reveal, erase)
      - notes: assert `actorUserId` equals the stubbed caller and `at` is set, on all three entries
      - depends: — · parallel: yes
      - verify: `mvn -B test -Dtest='AnnotationRecordDeleteTest,AnnotationRecordRevealTest,AnnotationRecordServiceTest'`

- [x] **T-13 · `values:[null]` → localized 400 (R2-07)**
      - files: `api/dto/AnnotationRecordInput.java`, `resources/messages.properties`, `resources/messages_pt.properties`, `infrastructure/i18n/AnnotationRecordMessageCoverageTest.java`, `api/AnnotationRecordResourceTest.java`
      - covers: constitution §Errors (uniform envelope), C-09 · scenario: envelope integrity (last known off-envelope 500 shape)
      - notes: element-level `@NotNull` on `values` with new key `annotation.record.value.required` (en+pt, coverage list, contract row already added); wire test posts `values:[null]` → 400 + violation code
      - depends: — · parallel: yes
      - verify: `mvn -B test -Dtest='AnnotationRecordMessageCoverageTest,AnnotationRecordResourceTest'`

- [x] **T-14 · Test-honesty bundle: post-states, accept-side bounds, OpenAPI paths, F13 guard, C-12 at-rest assert, Javadoc (R2-06/08/09/10/11/12)**
      - files: `application/annotation/AnnotationTypeEditGuardTest.java`, `api/AnnotationRecordResourceTest.java`, `api/OpenApiCoverageTest.java`, `application/annotation/AnnotationRecordServiceTest.java`, `infrastructure/persistence/AnnotationRecordRepository.java` (Javadoc only)
      - covers: the guard post-state clauses (scenarios 23/24/26/27), C-12 evidence, NFR-06
      - notes: post-state re-reads after guard 409s (fresh TX); accept 120/4 080/65 535; OpenAPI asserts the CRUD paths distinctly (not substrings); `optionIds:[null]` → 400 test; native-read assert `text_value IS NULL` ∧ `secret_ciphertext` ≠ plaintext bytes; restore `existsByType` Javadoc
      - depends: T-09, T-12 *(shared files)* · parallel: no
      - verify: `mvn -B test -Dtest='AnnotationType*Test,AnnotationRecord*Test,OpenApiCoverageTest'`

- [x] **T-15 · HANDOFF.md rewritten in English, state-accurate (R2-13/14)**
      - files: `HANDOFF.md`
      - covers: audit R2 artifact-hygiene findings; `project.language = en`
      - notes: drop the stale "falta implementar"/94-tests/17-scenarios claims; reflect R1+R2 state and what remains
      - depends: — · parallel: yes
      - verify: proofread — no stale claim survives `grep -i "falta\|94 test\|17/17" HANDOFF.md`

## R3 remediation task (audit Round 3, 2026-08-04)

> Round 3 verified all thirteen R2 findings fixed and guarded (verify green, 131 tests) and fenced them:
> **do not redo them**. One new finding remains — the R2-02 defect survives in the twin method one level
> below the one T-09 fixed. T-16 is the only behavioural work left in this feature.

- [x] **T-16 · Duplicate-label-safe option matching in the type PUT (R3-01)**
      - files: `application/annotation/AnnotationTypeService.java`, `application/annotation/AnnotationTypeEditGuardTest.java`
      - covers: OQ-17, BR-05, BR-03 · scenario: "Resending an identical type definition preserves every record's values" (duplicate-option-label edge — today the second same-labelled option is orphan-removed and the record's selection becomes a dangling id)
      - notes: mirror T-09's fix in `updateOptions` (`:167-169`): replace the `putIfAbsent` label map with per-label FIFO queues (`Map<String, Deque<FieldOption>>`), each incoming option polling its label's queue, exactly as `applyFields` (`:113-115`) does for names. No new guard and no new input validation — dropped labels keep the feat-003 replace semantics OQ-17 explicitly defers; only the *identical resend* must stop destroying identity. Tests: `[dev, dev]` SINGLE_CHOICE with a record selecting the **second** option — identical resend preserves both option ids **and** the record's `optionIds` (read through the DTO path, per T-11); dropping one `dev` still removes exactly one option (deferred replace semantics, unchanged)
      - depends: — · parallel: no  *(behavioural, and shares the file/test with T-09…T-14)*
      - verify: `mvn -B test -Dtest=AnnotationTypeEditGuardTest`

## Coverage & sequencing
- **30/30 spec-v2 scenarios covered:** FR-04 (T-01/T-04/T-08), FR-06 (T-05/T-12), FR-18 core + PUT-secret
  (T-02/T-04/T-06/T-08/T-12), OQ-14 (T-07/T-10), OQ-17 (T-07 rework/T-09/T-10/T-11/**T-16**), OQ-18 (T-07
  rework/T-10), tenant isolation (T-01/T-08).
- **Dependency chain:** T-01…T-08 done (R1 + rework). R2 done: T-09 → T-10, T-11; T-12, T-13, T-15 free;
  T-14 last (shares files with T-09/T-12). R3: T-16 alone, no dependencies.
- **Parallelisable (worktree):** T-12, T-13, T-15 (file-disjoint). T-09, T-10, T-11, T-14, T-16 serial.
- **Uncovered scenarios:** none by omission — the "identical resend preserves every record's values"
  scenario is *covered but currently red* on the duplicate-option-label input; T-16 closes it. The four
  post-state/who-when clause gaps the R2 audit named closed via T-11/T-12/T-14.
