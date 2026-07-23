# Plan — Annotation types (feat-003, US-1.1)

## Origin
- **Spec:** [spec.md](../spec.md) (approved 2026-07-23, 23 scenarios).
- **User Story:** US-1.1 — define an annotation type with an icon and typed fields (incl. list options).
- **FRs:** FR-01, FR-02, FR-03, FR-07. **BRs:** BR-03, BR-04, BR-05.
- **Architecture read:** `constitution/01-architecture.md` (AD-01/02/03/04/07/09/11), `03-code-standards.md`;
  feat-001 pattern survey (reuse, not reinvent).

## Approach (three sentences)
Add a tenant-owned **`AnnotationType`** aggregate (root → ordered `TypeField` → ordered `FieldOption`)
persisted through a new `AnnotationTypeRepository extends TenantScopedRepository`, with all cross-field
rules (name uniqueness, closed field-type set, options-only-on-choice, List-only badge colours, number
bounds, per-field-type visibility defaults) enforced in an `application`-layer `AnnotationTypeService`
behind `@Transactional`, surfacing feat-001's `ApiException`/`Problem` error contract with specific
machine codes. Type/field **icons** introduce the codebase's first image handling: an **`Image`** BLOB
entity (`@Lob`) stored via raw-binary `POST /api/images` and streamed back from `GET /api/images/{id}`,
with JSON payloads carrying only an image **reference** (AD-04). The irreversible type **delete** writes
the first **`AuditLog`** row in the same transaction (BR-05, C-10). Everything reuses feat-001's
`TenantContext`, tenant-scoped repository base, error mappers and ResourceBundle i18n unchanged.

**Secret flag (human decision 2026-07-23, added post-approval).** `TypeField` gains a `secret` boolean
(`BIT(1)`, default false), allowed only on `TEXT`/`FREE_TEXT` (`FieldType.allowsSecret()`), else
`VALIDATION_SECRET_NOT_ALLOWED`. feat-003 stores the flag as inert schema metadata **only** — value
encryption at rest, the elevated-role audited reveal, and the UI masking are a **new requirement**
(future FR + AD "encryption at rest" + compliance C-item + BR) **deferred to US-2.1** and the web
features, gated on **OQ-15** (crypto mechanism & key management). No crypto, no key material, no reveal
endpoint enters feat-003.

Layout (new files; nothing in feat-001 is modified except additive catalog/config lines):
- `domain/`: `AnnotationType`, `TypeField`, `FieldOption`, `Image`, `AuditLog`, enums `FieldType`, `BadgeColour`.
- `infrastructure/persistence/`: `AnnotationTypeRepository`, `ImageRepository`, `AuditLogRepository`.
- `application/annotation/`: `AnnotationTypeService`, `ImageService` (+ a validator helper).
- `api/`: `AnnotationTypeResource`, `ImageResource`; `api/dto/`: `AnnotationTypeInput`, `TypeFieldInput`,
  `FieldOptionInput`, `AnnotationTypeDto`, `TypeFieldDto`, `FieldOptionDto`, `ImageRefDto`.
- `api/error/`: add `ApiException` factories for the new codes (no new mapper — reuse `ApiExceptionMapper`).
- `resources/db/migration/V2__annotation_types.sql`; new keys in `messages.properties` + `messages_pt.properties`.

Detail: [data-model.md](data-model.md) · [contracts/rest-api.md](contracts/rest-api.md).

## One-way decisions (spend the scrutiny here)
1. **Aggregate & tenancy boundary.** `AnnotationType` is the tenant-scoped root; `TypeField`/`FieldOption`
   are aggregate-internal (no `tenant_id`, no own repository, cascade + orphanRemoval). `Image` and
   `AuditLog` are independent tenant-owned entities. — *Satisfies AD-03: no code path to a child except
   through the tenant-scoped root.*
2. **Image storage = MySQL `@Lob` BLOB + dedicated binary endpoint; JSON carries a reference** (AD-04).
   **Upload = raw image bytes** with a `Content-Type` header — **no multipart, no new transport dep.**
3. **Update = `PUT` full-replacement** of the type definition (not `PATCH`), because the aggregate has
   ordered children — unambiguous for the feat-004 type-builder.
4. **Error handling per constitution §Errors (03-code-standards):** *business/stateful/binary* errors are
   **specific domain exceptions** (`AnnotationTypeNotFoundException`, `AnnotationTypeNameTakenException`,
   `ImageTooLargeException`, …) extending a `domain/error/DomainException` base, mapped by one
   `DomainExceptionMapper` (AD-11, one wire shape). *Input-shape* rules are **Bean Validation with custom
   constraints** (`@ValidFieldType`, `@OptionsAllowedForFieldType`, `@BadgeColourAllowed`,
   `@NumberBoundsValid`, `@SecretAllowedForFieldType`) returning per-field violations. All messages are
   **dot-namespaced i18n keys** (`annotation.type.name.required`) in en + pt (C-09). *(This replaces the
   earlier `VALIDATION_*` string-code approach — a correction back to the constitution. feat-001's generic
   `ApiException` + SCREAMING_SNAKE keys are a pre-existing drift, tracked for realignment in **OQ-16**; not
   touched by this feature.)*
5. **Schema `V2__annotation_types.sql`** — 5 tables + FK graph; `DECIMAL(38,10)` number bounds, `LONGBLOB`
   image bytes. One-way (migration).
6. **New audit infrastructure** (`AuditLog` table + repository) — feat-003 is the first irreversible-delete
   feature, so C-10's audit trail starts here, deliberately minimal.

**Reversible (no deliberation spent):** service-vs-resource split; validator internal shape; whether the
type list later gains pagination; orphan-image cleanup policy.

## Alternatives rejected
- **Multipart upload for icons** (`quarkus-rest-multipart`). Rejected: raw-binary body + `Content-Type`
  header carries everything needed with zero new dependency (AD-08 restraint); multipart adds parsing
  surface for a single-file case.
- **Base64-embed image bytes in the type JSON.** Rejected: violates AD-04 (list/detail JSON must stay
  light; bytes only from the binary endpoint) and bloats every grid read.
- **Server-side thumbnail generation** (an image library). Rejected: AD-04 makes thumbnails a `notebox-web`
  rendering concern; the API serves the original + size, the web downscales. Avoids an image-processing dep.
- **One flat "type-with-fields" table / JSON blob of fields.** Rejected: ordered relational children give
  FK integrity, per-option colours and `@OrderColumn` ordering that BR-03/BR-04 invariants and the audit
  can check; a JSON blob makes field-type validation and future annotation-value FKs (US-2.1) unenforceable.
- **`PATCH` partial update of fields.** Rejected: partial semantics over an ordered child collection
  (which field moved? which option was removed?) are ambiguous; `PUT` replace is deterministic.
- **Redis-cache type schemas now** (NFR-03/AD-13). Rejected **for this feature** — see Risk; deferring is
  reversible and keeps the blast radius to one subsystem.

## Blast radius
**New — additive, no behavioural change to feat-001:** all `domain`/`application`/`api`/`infrastructure`
files listed above; `V2__annotation_types.sql`.
**Modified (additive lines only):**
- `messages.properties`, `messages_pt.properties` — new error keys (en + pt).
- `application.properties` — `notebox.image.max-bytes=5242880`, allowed content-types config.
- `pom.xml` — add **`quarkus-smallrye-openapi`** (NFR-06 contract docs). No other extension: `@Lob`/BLOB
  uses the existing `quarkus-hibernate-orm`; raw-binary upload uses the existing `quarkus-rest`.
**Consumers:** `notebox-web` (feat-004) codes against `contracts/rest-api.md`. No existing consumer of the
API changes (this is net-new surface). Reused unchanged: `TenantScopedRepository`, `TenantContext`,
`JwtTenantFilter`, `ApiExceptionMapper`, `MessageResolver`, `LocaleResolver`.

## Risk
| Risk | Signal that reveals it |
|---|---|
| First BLOB path — oversize upload or MySQL `max_allowed_packet` truncation | oversize-reject test (`VALIDATION_IMAGE_TOO_LARGE`) + a 5 MB round-trip test; document `max_allowed_packet ≥ 8 MB` for deploy |
| Audit row not atomic with delete | delete test asserts exactly one `audit_log` row **and** the type gone, in one `@Transactional` |
| Ordered `@OneToMany` + `@OrderColumn` + orphanRemoval reorder/replace bugs | PUT-reorder test + replace-drops-removed-children test |
| A cross-field rule missed | the 23 spec scenarios become the executable tests (implement step) |
| Child entities bypass tenant scoping | children have no repository; access-through-root enforced; `@InjectMock TenantContext` repo test proves cross-tenant type read = 404 |
| **NFR-03 (cached read p95 < 50 ms) unmet by design** | deliberate deferral: no Redis in feat-003; type reads hit MySQL and are correct (NFR-05 principle). Reversible — a later cache feature adds cache-aside behind `infrastructure` with **no contract change**. **Needs human sign-off** (an NFR "Must" is being deferred). |

## Gate
`human_approval`. Approve the three-sentence approach, the six one-way decisions (esp. #1 aggregate/tenancy
boundary and #2 image transport), the blast radius, and the **NFR-03 deferral**. Then `tasks`.
