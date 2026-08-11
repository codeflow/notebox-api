# Contract — Annotation records REST API

Base path `/api` (as feat-003). All endpoints class-level `@Authenticated`, `@Produces(APPLICATION_JSON)`,
each method `@Transactional`, request bodies `@Valid`. Tenant is resolved from the JWT via `TenantContext`
(never from the body). Enums cross the wire as **String**.

## Endpoints

| Method | Path | Purpose | Success | Auth |
|---|---|---|---|---|
| POST | `/annotation-records` | Create a record of a type | `201` + `AnnotationRecordDto` | member |
| GET | `/annotation-records/{id}` | Read one record (secret values masked) | `200` + `AnnotationRecordDto` | member |
| PUT | `/annotation-records/{id}` | Replace a record's name + values | `200` + `AnnotationRecordDto` | member |
| DELETE | `/annotation-records/{id}` | Explicit irreversible delete (audited) | `204` | member |
| POST | `/annotation-records/{id}/values/{fieldId}/reveal` | Reveal one secret value (audited) | `200` + `RevealResponse` | **ADMIN** |

> **Out of scope (US-2.2):** the collection listing `GET /annotation-records?typeId=…` with the
> visible-field column projection + pagination. This contract returns a **single** record; its read already
> returns all fields (secret ones masked).

## Payloads (JSON records)

```jsonc
> **PUT semantics for Secret values — DECIDED (human decision 2026-07-25, audit F4).**
> A `PUT` replaces the record's name and values, with one exception: a **Secret** field.
> - Field **omitted** from `values` → the stored ciphertext is **PRESERVED** intact (a rename never
>   destroys a secret).
> - Field present with `"text": "<new>"` → the value is **re-encrypted** under the active key version.
> - Field present with `"clearSecret": true` → the secret is **erased**, and the erasure writes an
>   **AuditLog** entry (destructive deletion must be explicit and accountable — BR-05, BR-10).
> - Field present with `"text": null` and no `clearSecret` → **no-op preserve** (this is exactly what a
>   client echoes back from a masked `GET`; it must NOT be a `type_mismatch` error).
>
> Non-secret fields keep plain replace semantics: omitted → value removed.

// AnnotationRecordInput  (POST body has annotationTypeId; PUT body omits it — path id fixes the record)
{
  "annotationTypeId": "uuid",           // POST only; required
  "name": "prod-broker",                // required, ≤120
  "values": [                           // at most one entry per defined field
    { "fieldId": "uuid", "text": "amqp://h" },          // TEXT / FREE_TEXT (secret sent as plaintext over TLS)
    { "fieldId": "uuid", "number": 5672 },              // NUMBER
    { "fieldId": "uuid", "imageId": "uuid" },           // IMAGE (reference)
    { "fieldId": "uuid", "optionIds": ["uuid"] },       // SINGLE_CHOICE / LIST (exactly 1) or MULTIPLE_CHOICE (0..n)
    { "fieldId": "uuid", "text": "s3cr3t-token" }       // a Secret TEXT field — server encrypts at rest
  ]
}

// AnnotationRecordDto  (response)
{
  "id": "uuid", "annotationTypeId": "uuid", "name": "prod-broker",
  "createdAt": "2026-07-24T10:00:00Z", "updatedAt": "2026-07-24T10:00:00Z",
  "values": [
    { "fieldId": "uuid", "fieldType": "TEXT",   "secret": false, "masked": false, "text": "amqp://h" },
    { "fieldId": "uuid", "fieldType": "NUMBER", "secret": false, "masked": false, "number": 5672 },
    { "fieldId": "uuid", "fieldType": "SINGLE_CHOICE", "secret": false, "masked": false, "optionIds": ["uuid"] },
    { "fieldId": "uuid", "fieldType": "TEXT",   "secret": true,  "masked": true,  "text": null }   // secret → masked
  ]
}

// RevealResponse  (POST …/reveal) — cleartext, never listed/logged
{ "recordId": "uuid", "fieldId": "uuid", "value": "s3cr3t-token" }
```

Values are returned **ordered by the type's field `position`** (DTO assembly reads the type). A field with no
stored value is omitted from `values`.

## Secret cipher port (application → infrastructure)

```java
// application/crypto/SecretValueCipher.java  (port; impl in infrastructure/security/AesGcmSecretValueCipher)
public interface SecretValueCipher {
    EncryptedValue encrypt(String plaintext);      // active key version, fresh 96-bit IV
    String         decrypt(EncryptedValue value);  // key chosen by value.keyVersion()
}
// application/crypto/EncryptedValue.java
public record EncryptedValue(byte[] ciphertext, byte[] iv, int keyVersion) {}
```

**Config (mirrors the JWT-key/secret-manager pattern):**
```properties
# dev/test default only — %prod overrides from the secret manager via env vars
notebox.crypto.active-key-version=1
notebox.crypto.keys.1=${NOTEBOX_CRYPTO_KEY_1:<dev-only-base64-AES-256>}
%prod.notebox.crypto.keys.1=${NOTEBOX_CRYPTO_KEY_1}
```
`.env.example` gains `NOTEBOX_CRYPTO_KEY_1`. Startup fails fast if the active version has no key.

## Errors — specific domain exceptions + dot-namespaced keys (constitution 03-code-standards)

Every `key` has a row in `messages.properties` + `messages_pt.properties`; `code` on the wire = the key.
Uniform `Problem(code, message, correlationId)` envelope; Bean-Validation failures use `ValidationProblem`.

| Condition | Mechanism | HTTP | key / code |
|---|---|---|---|
| Record name blank | `@NotBlank` on input | 400 | `annotation.record.name.required` |
| Value targets an undefined field | service → `DomainException(INVALID)` | 400 | `annotation.record.field.unknown` |
| Value data type ≠ field type | service (INVALID) | 400 | `annotation.record.value.type_mismatch` |
| Number outside min/max | service (INVALID) | 400 | `annotation.record.value.number.out_of_bounds` |
| Choice value not a predefined option | service (INVALID) | 400 | `annotation.record.value.option.unknown` |
| Image value id not resolvable in tenant | service (NOT_FOUND) | 404 | `annotation.record.value.image.not_found` |
| Record not found (or foreign tenant) | `AnnotationRecordNotFoundException(NOT_FOUND)` | 404 | `annotation.record.not_found` |
| Reveal by a non-ADMIN caller | `SecretRevealForbiddenException(FORBIDDEN)` | **403** | `annotation.record.secret.reveal.forbidden` |
| Reveal of a non-secret / empty field | service (INVALID) | 400 | `annotation.record.reveal.not_secret` |
| Duplicate value for the same field (F1) | `@AtMostOneValuePerField` on input | 400 | `annotation.record.value.duplicate_field` |
| `values` contains a null element (R2-07) | element `@NotNull` on input | 400 | `annotation.record.value.required` |
| Record name over 120 chars (F2) | `@Size(max=120)` on input | 400 | `annotation.record.name.too_long` |
| Text/secret value over the column bound (F3) | `AnnotationRecordValueTooLongException(INVALID)` | 400 | `annotation.record.value.too_long` |
| **Type delete while records exist** | `AnnotationTypeHasRecordsException(CONFLICT)` | 409 | `annotation.type.has_records` |
| **Field removed/retyped while records exist** (OQ-17) | `AnnotationTypeFieldHasRecordsException(CONFLICT)` | 409 | `annotation.type.field.has_records` |
| **Secret-flag flip while the field has values** (OQ-18) | `AnnotationTypeFieldSecretFlipException(CONFLICT)` | 409 | `annotation.type.field.secret_flip.has_values` |

**Type PUT semantics (OQ-17, human decision 2026-08-03):** the type update matches incoming fields to
existing ones and **preserves field identity** (and therefore all record values) for unchanged fields,
identical resends and type renames; adding a field is allowed; removing or retyping a field while the type
owns ≥1 record → 409 above. The Secret flag is immutable while the field has values (OQ-18) → 409 above.
Both keys need en + pt rows (C-09).

**New error category:** `ErrorCategory.FORBIDDEN → 403`, added to the enum and the `DomainExceptionMapper`
switch (the only taxonomy change; existing `NOT_FOUND/CONFLICT/INVALID` unchanged).

## OpenAPI
All five endpoints documented (NFR-06); `OpenApiCoverageTest` extended to assert their presence.
