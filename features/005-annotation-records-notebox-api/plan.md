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
