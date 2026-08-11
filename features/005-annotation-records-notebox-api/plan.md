# Plan — Annotation records (API): CRUD, conformance, secret encryption & audited reveal

## Origin
- **Spec:** [spec.md](spec.md) · **US:** US-2.1 · **FRs:** FR-04, FR-06, FR-18 · **BRs:** BR-03, BR-05, BR-10 · **AD:** AD-03, AD-14.
- Designed against the **notebox-api** stack (Quarkus + Jakarta APIs, Hibernate ORM with plain `EntityManager` — no Panache, MicroProfile JWT, Flyway, MySQL), reusing feat-001/feat-003 patterns verbatim.

## Approach (three sentences)
Add `AnnotationRecord` as a **tenant-owned aggregate root** (mirroring `AnnotationType`) that owns one
`AnnotationValue` per populated field, validated for **conformance to its type** (BR-03) by a new
`AnnotationRecordService` before persistence, through the existing `TenantScopedRepository` choke point
(AD-03). **Secret** field values (Text/Free-text) are encrypted at rest with a new
`SecretValueCipher` port (AES-256-GCM, versioned master key from config/secret-manager — AD-14),
implemented only in `infrastructure/`, stored as ciphertext+IV+key-version, **masked** on ordinary reads,
and disclosed by a dedicated **reveal** action gated to `Role.ADMIN` with an `AuditLog` entry (BR-10, C-10,
C-12). Deleting a record is explicit + audited (FR-06, BR-05), and the existing `AnnotationTypeService.delete`
gains a **record-existence guard** (OQ-14).

## Design detail

### Aggregate & persistence (follows the `AnnotationType` root pattern)
- **`AnnotationRecord`** (`domain/`) `implements TenantOwned`: `id` (UUID `CHAR(36)`), `tenantId`,
  `annotationTypeId` (FK), `name`, `createdAt`, `updatedAt`. Owns `List<AnnotationValue>` via
  `@OneToMany(cascade=ALL, orphanRemoval=true) @JoinColumn(name="annotation_record_id")`. `@PrePersist`
  assigns id/timestamps. A `replaceValues()` method gives PUT semantics (clear + addAll), exactly like
  `AnnotationType.replaceFields()`.
- **`AnnotationValue`** (`domain/`, aggregate-internal, **no `tenant_id`** — reached only via the root):
  `id`, `typeFieldId` (references the owning type's `TypeField.id`), and a **one-of** payload determined by
  the field's type:
  - `textValue TEXT NULL` — plaintext for **non-secret** TEXT/FREE_TEXT;
  - `numberValue DECIMAL(38,10) NULL` — NUMBER;
  - `imageId CHAR(36) NULL` — IMAGE (reference, validated via `ImageService.existsInTenant`);
  - `selectedOptionIds` — SINGLE_CHOICE (exactly one) / MULTIPLE_CHOICE (zero-or-more), an
    `@ElementCollection` of `FieldOption.id` in table `annotation_value_option`;
  - **secret payload** for a Secret TEXT/FREE_TEXT field: `secretCiphertext VARBINARY NULL`,
    `secretIv VARBINARY(12) NULL`, `secretKeyVersion INT NULL` — `textValue` stays **NULL** (cleartext
    never persisted).
  Invariant (service-enforced, BR-03): exactly the payload column-set matching the field's `fieldType` is
  populated; a Secret field populates the secret payload, never `textValue`.

### Conformance validation (`AnnotationRecordService`, `application/annotation/`)
On create/update: `annotationTypeRepository.findByIdInTenant(typeId)` (404 → `AnnotationTypeNotFoundException`);
build a `Map<fieldId, TypeField>` from `type.getFields()`; for each input value resolve its `TypeField`
(unknown id → `annotation.record.field.unknown`), then dispatch on `fieldType`:
- TEXT/FREE_TEXT → string; if `field.isSecret()` encrypt via `SecretValueCipher`, else store plaintext;
- NUMBER → parse `BigDecimal`; enforce `numberMin/max` (→ `annotation.record.value.number.out_of_bounds`);
  non-numeric → `annotation.record.value.type_mismatch`;
- SINGLE_CHOICE/LIST/MULTIPLE_CHOICE → each selected id must be one of the field's `FieldOption` ids
  (→ `annotation.record.value.option.unknown`); SINGLE_CHOICE/LIST accept exactly one;
- IMAGE → `imageId` must resolve via `ImageService.existsInTenant` (→ reuse `annotation…image` not-found key).
Tenant is read only from `TenantContext`, never from input.

### Secret encryption (AD-14) — new crypto seam, confined to `infrastructure/`
- **Port** `SecretValueCipher` (`application/crypto/`): `EncryptedValue encrypt(String plaintext)` /
  `String decrypt(EncryptedValue)`. `EncryptedValue` = `(byte[] ciphertext, byte[] iv, int keyVersion)`.
- **Impl** `AesGcmSecretValueCipher` (`infrastructure/security/`): AES-256-GCM, fresh random 12-byte IV per
  value, 128-bit tag. Master key(s) loaded via `@ConfigProperty` mirroring the JWT-key pattern (point 8 of
  the survey): a **versioned keyring** `notebox.crypto.keys.<version>` (base64 AES-256) + active
  `notebox.crypto.active-key-version`; dev/test default committed, `%prod` from `${NOTEBOX_CRYPTO_KEY_...}`.
  Encrypt uses the active version; decrypt selects the key by the stored `keyVersion` (rotation without
  rewriting rows). Grep-checkable: `javax.crypto` usage exists only under `infrastructure/`.

### Reveal + authorization
- `AnnotationRecordService.reveal(recordId, fieldId)`: require `tenantContext.role() == Role.ADMIN` else
  `SecretRevealForbiddenException`; field must be Secret and hold a value; decrypt; write
  `AuditLog(ACTION_SECRET_REVEALED, TARGET_ANNOTATION_RECORD, recordId)` in the same `@Transactional`;
  return cleartext.
- **Error taxonomy extension:** add `FORBIDDEN` (→ HTTP 403) to `ErrorCategory` and the
  `DomainExceptionMapper` switch. `SecretRevealForbiddenException extends DomainException(FORBIDDEN,
  "annotation.record.secret.reveal.forbidden")`.

### Type-delete guard (OQ-14) — touches feat-003
`AnnotationTypeService.delete(id)` calls `annotationRecordRepository.existsByType(id)`; if true throw
`AnnotationTypeHasRecordsException extends DomainException(CONFLICT, "annotation.type.has_records")` before
removing. Empty-type delete keeps feat-003 behaviour.

### DTO & masking (`api/dto/`, `from(entity)` convention)
`AnnotationRecordDto.from(record, type)` orders values by the type's field `position`; `AnnotationValueDto`
carries `fieldId`, `fieldType`, `secret`, and the typed value; **for a secret value it emits `masked:true`
and `value:null`** — cleartext is returned only by the reveal endpoint's `RevealResponse`.

## Alternatives rejected
1. **Single JSON/EAV blob of values on the record** — rejected: US-2.2's listing needs per-field columns
   and choice-option integrity; a blob loses relational validation and the established normalized
   ordered-children pattern (V2). *(one-way: schema)*
2. **Encrypt the whole record** rather than per-secret-value — rejected: BR-09/BR-10 scope confidentiality
   to Secret fields only; whole-record encryption would break listing/filtering of non-secret fields.
3. **`@RolesAllowed("ADMIN")` alone for reveal** — rejected: it throws a generic 403 that can't carry the
   spec's localized key `annotation.record.secret.reveal.forbidden`; an explicit role check + domain
   exception keeps the uniform `Problem` envelope. *(reversible)*
4. **Reveal as `GET`** — rejected: reveal is side-effecting (writes an audit row) and secret-bearing;
   `POST` avoids caching and query-string logging of a sensitive action.

## Reversibility
| Decision | Kind |
|---|---|
| `V3__annotation_records.sql` schema (record/value/value-option tables) | **one-way** |
| Ciphertext-at-rest layout (ciphertext + IV + key-version columns) | **one-way** |
| REST contract for records CRUD + reveal | **one-way** |
| `ErrorCategory.FORBIDDEN` (public error taxonomy) | **one-way** |
| Masking representation (`masked:true`, `value:null`) | reversible |
| Service internal structure, keyring config property names | reversible |

## Blast radius
- **New:** `domain/AnnotationRecord`, `domain/AnnotationValue`, domain exceptions
  (`AnnotationRecordNotFoundException`, `SecretRevealForbiddenException`, `AnnotationTypeHasRecordsException`);
  `infrastructure/persistence/AnnotationRecordRepository`; `application/annotation/AnnotationRecordService`;
  `application/crypto/SecretValueCipher` + `EncryptedValue`; `infrastructure/security/AesGcmSecretValueCipher`;
  `api/AnnotationRecordResource`; `api/dto/*` (record/value input+dto, `RevealResponse`);
  `db/migration/V3__annotation_records.sql`; `messages*.properties` keys; new tests.
- **Modified (feat-003/feat-001):** `AnnotationTypeService.delete` (+ record guard); `ErrorCategory` (+FORBIDDEN);
  `DomainExceptionMapper` (+403); `application.properties` (+crypto config); `.env.example` (+key env vars);
  `OpenApiCoverageTest` (new endpoints); i18n coverage test picks up new keys automatically.
- **Consumers:** feat-006 (web) codes against the records + reveal contract; US-2.2 will consume the record
  read model for the visible-field listing.

## Risk
- **Master-key loss/mismanagement** → secret values unrecoverable (accepted per AD-14). *Signal:* decrypt
  round-trip test; startup fails fast if no active key configured.
- **Conformance gap** (a value type slipping validation). *Signal:* per-field-type service tests + the
  cross-tenant repository test template.
- **Type mutation vs existing records** — feat-003's `PUT` replaces `TypeField`s with `orphanRemoval`, so
  editing a type after records exist could orphan `AnnotationValue.typeFieldId` references. **Out of scope
  here** (type editing is feat-003), but a real latent integrity gap. *Signal:* flag as a follow-up
  candidate (guard field-removal-with-values, symmetric to OQ-14) — recorded in Risk, not opened as an OQ
  since it doesn't block US-2.1's records CRUD.
  *(Superseded by v2: audit R1/F5 proved this gap breaks feat-005's own reads; OQ-17 was opened, decided
  2026-08-03, and the guard is in this feature — see Addendum.)*

---

# Addendum v2 — spec v2 + audit-R2 rework (2026-08-04)

## Origin
Spec **v2** (30 scenarios: +4 F4 PUT-secret, +6 OQ-17, +3 OQ-18) after audit R1 verdict *fail*;
human decisions **OQ-17** and **OQ-18** (2026-08-03, catalog + PRD §3.1); audit **R2** verdict
*fail — narrow* (R2-01…R2-14). This addendum records the design of the shipped R1 remediation
(§A) and designs the remaining R2 work (§B). Audit R2's **verified-good list is fenced**: nothing
there is redesigned.

## A. Shipped remediation design (recorded; already implemented and empirically verified)

- **F4 — PUT vs Secret values** (`AnnotationRecordService.toUpdatedValues` + `AnnotationValueInput.clearSecret`):
  omitted secret field → ciphertext preserved; `text:"<new>"` → re-encrypt under the active key;
  `clearSecret:true` → erase + `ANNOTATION_RECORD_SECRET_CLEARED` audit entry; `text:null` without the
  flag → no-op preserve. Non-secret fields keep pure replace.
- **F1/F2/F3 — request-envelope validation**: class-level Bean-Validation constraint
  `@AtMostOneValuePerField` on `AnnotationRecordInput` (declarative, per 03-code-standards) with key
  `annotation.record.value.duplicate_field`; `@Size(max=120)` name → `annotation.record.name.too_long`;
  byte-bounds in the service — TEXT/FREE_TEXT ≤ 65 535 B, secret cleartext ≤ 4 080 B (VARBINARY(4096) −
  16 B GCM tag) → `AnnotationRecordValueTooLongException` (`annotation.record.value.too_long`).
- **F6 — audit accountability**: **`V4__audit_log_detail.sql`** adds `audit_log.detail VARCHAR(255) NULL`
  *(one-way)*; `AuditLog` 6-arg ctor (5-arg delegates null — feat-003 call sites untouched); reveal and
  clear-secret entries carry `fieldId=<uuid>`; `AuditLogRepository.countForTargetAndAction`.
- **OQ-17/OQ-18 — type-edit guards** (`AnnotationTypeService.applyFields/updateField/updateOptions`):
  incoming fields match existing **by name** and keep entity identity (in-place update); options match
  **by label** (so choice selections survive identical resends); removal/retype with records →
  `AnnotationTypeFieldHasRecordsException` (409, `annotation.type.field.has_records`); Secret-flag change
  while the field holds values (`AnnotationRecordRepository.existsValueForField`, tenant-scoped via the
  owning record) → `AnnotationTypeFieldSecretFlipException` (409, `…secret_flip.has_values`). New entity
  APIs: `TypeField.replaceOptions`, `FieldOption.setBadgeColour`.
- **F7/F11/F13 — hardening**: `textValue` mapped `length=65535` (= V3 `TEXT`) plus
  `%test.quarkus.hibernate-orm.database.generation=validate` so entity↔migration drift fails the suite
  (this guard forced the `Image.bytes`→`LONGBLOB` alignment, a feat-003 one-liner it caught);
  `AesGcmSecretValueCipher.keyFor` rejects non-32-byte keys at startup (no silent AES-128);
  `optionIdsOrEmpty` tolerates a null element.

## B. R2 remediation design (authorized by this addendum)

- **R2-02 — duplicate-named fields bypass the OQ-17 guard.** Replace the `putIfAbsent` name map in
  `applyFields` with per-name **FIFO queues** (`Map<String, Deque<TypeField>>`): each incoming field polls
  its name's queue; identical resends with duplicate names therefore match positionally and preserve both
  identities; any unpolled instance lands in the leftover set, where the existing has-records guard
  already throws the 409. Non-breaking for every existing type. *Alternative rejected:* forbid duplicate
  field names at the API edge — it retroactively 400s the PUT of any type that already holds duplicates
  and belongs to feat-003's contract; left as a follow-up candidate there.
- **R2-03/R2-04/R2-05 — test contract tightening** (signatures only; tests are `implement`):
  preservation tests pin **field-id identity** (capture ids before the PUT, compare after) and read values
  through the DTO path, not the raw collection; new **wire tests** assert HTTP **409 + machine code** for
  all three guards (type delete, field removal/retype, secret flip); audit-entry tests assert
  `actorUserId == the stubbed caller` and `at != null` (the spec's "who … and when").
- **R2-07 — `values:[null]`**: element-level `@NotNull` on `AnnotationRecordInput.values`
  (`List<@Valid @NotNull AnnotationValueInput>`) with new key **`annotation.record.value.required`**
  (en + pt + coverage list + contract row) — closes the last off-envelope 500 shape.
- **R2-06/08/09/10/11/12/13/14 — discretionary bundle** (same implement pass): post-state re-reads after
  guard rejections (fresh TX); accept-side boundary tests at 120/4 080/65 535; OpenAPI test asserts the
  distinct CRUD paths, not substrings; a guarding test for `optionIds:[null]`; a stored-column
  ciphertext-at-rest assertion (native read: `text_value IS NULL ∧ secret_ciphertext ≠ plaintext bytes`)
  making C-12's claimed evidence real; restore `existsByType`'s Javadoc; rewrite `HANDOFF.md` in English
  reflecting the current state.

## Reversibility (v2 additions)
| Decision | Kind |
|---|---|
| `V4__audit_log_detail.sql` (`audit_log.detail`) | **one-way** |
| `clearSecret` input + PUT-secret semantics (public contract, feat-006 codes against it) | **one-way** |
| Guard error keys `annotation.type.field.*`, `annotation.record.value.*` (public contract) | **one-way** |
| Duplicate-name FIFO matching inside `applyFields` | reversible |
| `%test` schema-validation flag | reversible |

## Blast radius (v2 delta — over §Blast radius above)
- **New:** `db/migration/V4__audit_log_detail.sql`; `api/validation/AtMostOneValuePerField(+Validator)`;
  `domain/error/{AnnotationRecordValueDuplicateFieldException, AnnotationRecordValueTooLongException,
  AnnotationTypeFieldHasRecordsException, AnnotationTypeFieldSecretFlipException}`;
  `AnnotationTypeEditGuardTest`, `AesGcmSecretValueCipherKeyLengthTest`; 6 i18n keys (5 shipped +
  `annotation.record.value.required` planned) in both locales.
- **Modified:** `AnnotationTypeService` (type-PUT redesign, +~90 lines); `AnnotationRecordService`
  (update semantics, bounds, audit detail); `AnnotationRecordRepository` (+2 queries);
  `domain/{AuditLog, TypeField, FieldOption, AnnotationValue, Image}`; `AnnotationRecordInput` /
  `AnnotationValueInput`; `application.properties` (`%test` validate); feature tests throughout.
- **Consumers:** feat-006 additionally codes against `clearSecret`, the masked-echo no-op, and the three
  409 guard codes.

## Risk (v2)
- **Guard post-state rests on TX rollback** (service mutates before throwing). *Signal:* R2-06 post-state
  re-reads in a fresh transaction.
- **FIFO duplicate matching is subtle.** *Signal:* dedicated `[X,X]`-type tests — identical resend
  preserves both; dropping one → 409.
- **V4 is live in dev/test only while unmerged** — no deployed schema holds `detail` yet; the one-way
  call is made here, before first deploy.
