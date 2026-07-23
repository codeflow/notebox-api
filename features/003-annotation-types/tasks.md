# Tasks — Annotation types (feat-003, US-1.1)

> Decomposition of the approved [plan.md](plan.md) / [contracts/rest-api.md](contracts/rest-api.md) /
> [data-model.md](data-model.md). Each task is one outcome, ships its test, and cites the Gherkin
> scenario(s) it makes pass. Error handling follows constitution 03-code-standards §Errors: **specific
> domain exceptions** for business/stateful/binary errors, **Bean Validation (custom constraints)** for
> input shape, **dot-namespaced i18n keys** in en+pt. Ordered by dependency, then risk-first. Root
> package `com.notebox.api`.

- [x] **T-01 · Domain enums `FieldType` + `BadgeColour` with helper predicates**
      - files: `domain/FieldType.java`, `domain/BadgeColour.java`, `domain/FieldTypeTest.java`
      - covers: BR-04, FR-02, FR-03 · scenarios: "All seven field types are accepted", "Visible-for-viewing default depends on the field type"
      - helpers: `defaultVisibleForViewing()` (D2), `allowsOptions()`, `allowsBadgeColour()` (LIST), `allowsSecret()` (TEXT/FREE_TEXT)
      - depends: — · parallel: yes
      - verify: `./mvnw test -Dtest=FieldTypeTest`

- [ ] **T-02 · Persistence schema (V2 migration, all 5 tables) + aggregate entities + tenant-scoped repository**
      - files: `resources/db/migration/V2__annotation_types.sql`, `domain/AnnotationType.java`, `domain/TypeField.java`, `domain/FieldOption.java`, `infrastructure/persistence/AnnotationTypeRepository.java`, `infrastructure/persistence/AnnotationTypeRepositoryTest.java`
      - covers: BR-03, FR-01, FR-02, FR-03, AD-03 · scenarios: "Create a type with ordered fields", "Update reorders fields" (persistence side), "A foreign tenant cannot read another tenant's type" (repo-level), secret column persists
      - notes: V2 also creates `image`/`audit_log` tables (entities in T-05/T-09); `@OrderColumn` on fields & options; repo extends `TenantScopedRepository<AnnotationType>`; test fakes tenant via `@InjectMock TenantContext`
      - depends: T-01 · parallel: no  *(defines the schema — risk-first)*
      - verify: `./mvnw test -Dtest=AnnotationTypeRepositoryTest`

- [ ] **T-03 · Domain exception hierarchy + `DomainExceptionMapper` + i18n catalog (en/pt, dot-namespaced)**
      - files: `domain/error/DomainException.java` (base, HTTP-free, holds message key + `ErrorCategory`), `domain/error/ErrorCategory.java`, `api/error/DomainExceptionMapper.java`, `resources/messages.properties`, `resources/messages_pt.properties`, `infrastructure/i18n/AnnotationTypeMessageCoverageTest.java`
      - covers: constitution §Errors, C-09 · underpins every specific-exception scenario ("Type name is unique…", not-found, image errors); the code = the dot-namespaced key resolved via `MessageResolver`
      - notes: mapper translates `ErrorCategory` → HTTP (NOT_FOUND→404, CONFLICT→409, INVALID→400) and resolves the localized message; keys `annotation.type.*`, `annotation.field.*`, `annotation.image.*`; coverage test asserts every key present in **en and pt**
      - depends: — · parallel: yes
      - verify: `./mvnw test -Dtest=AnnotationTypeMessageCoverageTest`

- [ ] **T-04 · Custom Bean Validation constraints + validators + validation-error contract**
      - files: `api/validation/{ValidFieldType,OptionsAllowedForFieldType,BadgeColourAllowed,NumberBoundsValid,SecretAllowedForFieldType}.java` (+ their `ConstraintValidator`s), `api/error/ConstraintViolationMapper.java` (emits `violations[]` with localized messages), i18n message wiring to the request locale, `api/validation/ConstraintsTest.java`
      - covers: AD-07, C-09, BR-04 · scenarios: "Reject an unknown field type", "Reject options on a non-choice field", "Reject a badge colour outside the palette", "Reject a Number field whose min exceeds its max", "Reject Secret on a non-text field"
      - notes: custom constraints preferred over imperative checks; messages are dot-namespaced keys (`annotation.field.type.unknown`, …) interpolated per request locale; the mapper returns the `validation.failed` envelope + per-field violations
      - depends: T-01 · parallel: yes
      - verify: `./mvnw test -Dtest=ConstraintsTest`

- [ ] **T-05 · Image storage: `Image` entity + repository + `ImageService` + specific exceptions + config**
      - files: `domain/Image.java`, `domain/error/{ImageTooLargeException,UnsupportedImageTypeException,ImageNotFoundException}.java`, `infrastructure/persistence/ImageRepository.java`, `application/annotation/ImageService.java`, `application/annotation/ImageServiceTest.java`, `resources/application.properties` (image limits)
      - covers: FR-07, AD-04, AD-07, NFR-04, C-07 · scenarios: "Reject an oversize icon", "Reject a disallowed icon format" (+ valid store)
      - notes: `@Lob byte[]` (first BLOB); maps to `image` table from V2; validates `{png,jpeg,gif,webp}` + ≤5 MB before persist, raising the specific exceptions (extend `DomainException` from T-03); `notebox.image.max-bytes=5242880`
      - depends: T-02, T-03 · parallel: no
      - verify: `./mvnw test -Dtest=ImageServiceTest`

- [ ] **T-06 · Image REST endpoints: raw-binary `POST /api/images` + streaming `GET /api/images/{id}`**
      - files: `api/ImageResource.java`, `api/dto/ImageRefDto.java`, `api/ImageResourceTest.java`
      - covers: FR-07, AD-04, C-01, C-02 · scenarios: "Upload and retrieve a type icon", foreign-tenant image → `ImageNotFoundException` (404)
      - notes: raw body + `Content-Type` header (no multipart); returns `ImageRefDto`; tenant-scoped fetch
      - depends: T-05 · parallel: yes
      - verify: `./mvnw test -Dtest=ImageResourceTest`

- [ ] **T-07 · Request/response DTOs annotated with built-in + custom constraints**
      - files: `api/dto/{AnnotationTypeInput,TypeFieldInput,FieldOptionInput,AnnotationTypeDto,TypeFieldDto,FieldOptionDto}.java`, `api/dto/AnnotationTypeDtoTest.java`
      - covers: FR-02, FR-03 (structural) · scenarios: "Reject a type with no name", "Reject a field with no name" (both via `@NotBlank`)
      - notes: records; `@NotBlank`/`@Size(max=120)` on names; the T-04 custom constraints on fields/class; nullable `visibleForViewing`/`secret`; response DTOs carry ids + resolved defaults via `from(entity)`
      - depends: T-01, T-04 · parallel: yes
      - verify: `./mvnw test -Dtest=AnnotationTypeDtoTest`

- [ ] **T-08 · `AnnotationTypeService`: create / list / get + stateful specific exceptions**
      - files: `application/annotation/AnnotationTypeService.java`, `domain/error/{AnnotationTypeNotFoundException,AnnotationTypeNameTakenException}.java`, `application/annotation/AnnotationTypeServiceTest.java`
      - covers: FR-01, FR-02, FR-03, BR-03 · scenarios: "Type name is unique within a tenant" (`AnnotationTypeNameTakenException`), "The same type name may exist in two different tenants", "All seven field types are accepted", visible-default & secret-default resolution, "Mark a Text/Free text field as Secret"
      - notes: `@ApplicationScoped` + `@Transactional`; applies D2 visibility & `secret` default; uniqueness pre-check → `AnnotationTypeNameTakenException`; validates referenced `iconImageId` in-tenant (`ImageNotFoundException`); `get` throws `AnnotationTypeNotFoundException`
      - depends: T-02, T-03, T-07 · parallel: no
      - verify: `./mvnw test -Dtest=AnnotationTypeServiceTest`

- [ ] **T-09 · `AnnotationTypeService`: replace (PUT) + irreversible delete with audit trail**
      - files: `application/annotation/AnnotationTypeService.java` (extend), `domain/AuditLog.java`, `infrastructure/persistence/AuditLogRepository.java`, `application/annotation/AnnotationTypeDeleteTest.java`
      - covers: FR-01, BR-05, C-10 · scenarios: "Update reorders fields", "Delete a type is explicit and irreversible" (+ audit entry)
      - notes: PUT replaces the whole definition (orphanRemoval drops removed children); delete writes one `AuditLog` row in the **same** `@Transactional`; `AuditLog` maps to `audit_log` from V2
      - depends: T-08 · parallel: no  *(same service file as T-08)*
      - verify: `./mvnw test -Dtest=AnnotationTypeDeleteTest`

- [ ] **T-10 · `AnnotationTypeResource` — HTTP CRUD (POST/GET/GET{id}/PUT/DELETE), authenticated + tenant-scoped**
      - files: `api/AnnotationTypeResource.java`, `api/AnnotationTypeResourceTest.java`
      - covers: FR-01, C-01, C-02 · scenarios: "Create a type with ordered fields" (HTTP), "A foreign tenant cannot read/modify/delete another tenant's type" (404 via `AnnotationTypeNotFoundException`), "Reject an unauthenticated request" (401), delete → 204; plus HTTP round-trip of the T-04 validation violations
      - notes: `@Authenticated`; `@Valid` bodies (triggers the custom constraints); RestAssured tests mint tokens via `TestTokens` + `TestData`
      - depends: T-07, T-08, T-09 · parallel: no
      - verify: `./mvnw test -Dtest=AnnotationTypeResourceTest`

- [ ] **T-11 · API docs: add `quarkus-smallrye-openapi` + verify endpoint coverage**
      - files: `pom.xml`, `api/OpenApiCoverageTest.java`
      - covers: **NFR-06** (contract in OpenAPI, in sync) — *NFR-driven, no Gherkin scenario*
      - notes: the one new extension; OpenAPI generated from JAX-RS annotations; test asserts every new path appears in `/q/openapi`
      - depends: T-06, T-10 · parallel: no
      - verify: `./mvnw test -Dtest=OpenApiCoverageTest`

## Coverage & ordering summary
- **23/23 Gherkin scenarios covered**; NFR-06 covered by T-11 (the only task without a Gherkin scenario — an NFR, flagged as such).
- **Error handling (constitution §Errors):** T-03 = specific-exception hierarchy + mapper + i18n; T-04 = custom Bean Validation constraints + violation contract. These are the two infrastructure tasks the directive added; every later task reuses them.
- **Dependency chain:** T-01 → T-02 → T-05 → T-06 · T-03 (infra) · (T-01→)T-04 → T-07 → T-08 → T-09 → T-10 → T-11.
- **Parallelisable:** T-03, T-04, T-06, T-07 (independent files once their deps are met). The rest is sequential — schema first, the shared `AnnotationTypeService` file (T-08/T-09), then the resource.
- **Risk-first:** T-02 (schema + tenant scoping) leads; T-03/T-04 (error & validation contract) are early so every consumer builds on the final shape.
