# Contract — Annotation types REST API (feat-003, US-1.1)

> The artifact `implement` codes against and `audit` checks against. Consumed by
> **feat-004-annotation-types-web**. All endpoints are `@Authenticated` (C-02); every read/write is
> tenant-scoped via `TenantContext` (AD-03) — a foreign-tenant id yields `404 RESOURCE_NOT_FOUND`
> and discloses nothing (C-01). Errors use feat-001's `Problem{code,message,correlationId}` shape,
> `message` localized by key = `code` (AD-05, ResourceBundle). Base path `/api`.

## Endpoints

### Annotation types
| Method | Path | Body | Success | Purpose |
|---|---|---|---|---|
| POST | `/api/annotation-types` | `AnnotationTypeInput` (JSON) | `201` + `AnnotationTypeDto` | create a type |
| GET | `/api/annotation-types` | — | `200` + `AnnotationTypeDto[]` | list this tenant's types (not paginated — NFR-08 targets annotation/task lists) |
| GET | `/api/annotation-types/{id}` | — | `200` + `AnnotationTypeDto` | full type schema |
| PUT | `/api/annotation-types/{id}` | `AnnotationTypeInput` (JSON) | `200` + `AnnotationTypeDto` | **replace** the whole definition (rename, reorder/add/remove fields & options) |
| DELETE | `/api/annotation-types/{id}` | — | `204` | irreversible delete of an **empty** type (BR-05, + audit C-10) |

> **PUT (full replace), not PATCH:** the type is an aggregate with ordered children; a wholesale
> replace is unambiguous for a type-builder UI. Deleting a type that owns annotation records is
> **out of scope / [TBD — OQ-14]**; no records can exist until US-2.1, so DELETE here removes only
> the type + its field/option definitions.

### Images (AD-04 — first binary surface in the codebase)
| Method | Path | Body | Success | Purpose |
|---|---|---|---|---|
| POST | `/api/images` | **raw image bytes**, `Content-Type: image/png\|jpeg\|gif\|webp` | `201` + `ImageRefDto` | upload an icon binary, get its id |
| GET | `/api/images/{id}` | — | `200` + **binary stream** (`Content-Type` = stored type) | fetch icon bytes (tenant-scoped) |

> **Raw-binary upload, not multipart:** the request body is the image bytes and `Content-Type` names
> the format — no `quarkus-rest-multipart`/new dependency. Client flow: upload icon → receive
> `imageId` → reference it in `AnnotationTypeInput`. JSON never carries base64 bytes (AD-04).
> **Thumbnails:** the API serves the original binary + size metadata; downscaled/thumbnail rendering
> is `notebox-web`'s concern (AD-04 consequence), satisfied by deriving from the served original —
> no server-side image processing here.

## Payloads (JSON records)

```jsonc
// AnnotationTypeInput  (POST / PUT request)
{
  "name": "RabbitMQ",                 // required, ≤120, unique per tenant
  "iconImageId": "uuid|null",         // optional; must resolve to an Image in this tenant
  "fields": [                         // ordered; array order == field order
    {
      "name": "Status",               // required, ≤120
      "fieldType": "LIST",            // required; one of the 7 (enum)
      "iconImageId": "uuid|null",     // optional field icon
      "visibleForViewing": true,      // optional; omitted => per-type default (D2)
      "secret": false,                // optional; TEXT/FREE_TEXT only; default false (value encryption is US-2.1, OQ-15)
      "numberMin": null,              // Number only, optional
      "numberMax": null,              // Number only, optional
      "options": [                    // only for LIST / SINGLE_CHOICE / MULTIPLE_CHOICE; ordered
        { "label": "open",    "badgeColour": "GREEN" },  // colour: List only, from palette
        { "label": "blocked", "badgeColour": "RED" }
      ]
    }
  ]
}

// AnnotationTypeDto  (response) — adds server-assigned ids + resolved defaults
{
  "id": "uuid", "name": "RabbitMQ", "iconImageId": "uuid|null",
  "createdAt": "2026-07-23T10:00:00Z",
  "fields": [
    { "id": "uuid", "name": "Status", "fieldType": "LIST", "iconImageId": null,
      "visibleForViewing": true, "secret": false, "numberMin": null, "numberMax": null,
      "options": [ { "id": "uuid", "label": "open", "badgeColour": "GREEN" } ] }
  ]
}

// ImageRefDto  (POST /api/images response) — never the bytes
{ "id": "uuid", "contentType": "image/png", "sizeBytes": 204800 }
```

**Enums on the wire:** `fieldType` ∈ `TEXT|LIST|NUMBER|FREE_TEXT|SINGLE_CHOICE|MULTIPLE_CHOICE|IMAGE`;
`badgeColour` ∈ `RED|GREEN|BLUE|BLACK|GRAY|YELLOW`.

## Errors — specific exceptions + Bean Validation (constitution 03-code-standards §Errors)

Two mechanisms, per the standard. Every `key` below is **dot-namespaced** and has a row in
`messages.properties` + `messages_pt.properties`; `code` on the wire equals the key.

### 1. Input-shape validation → **Bean Validation** (custom constraints preferred), i18n per field
Declarative on the request DTOs; a violation yields **HTTP 400** with a `validation.failed` envelope whose
`violations[]` lists each offending field + its localized message.

| Constraint (annotation) | Field / target | Message key |
|---|---|---|
| `@NotBlank` | type `name`, field `name`, option `label` | `annotation.type.name.required`, `annotation.field.name.required`, `annotation.field.option.label.required` |
| `@ValidFieldType` (custom) | `fieldType` | `annotation.field.type.unknown` (BR-04 closed set) |
| `@OptionsAllowedForFieldType` (custom, class-level) | a field's `options` | `annotation.field.options.not_allowed` |
| `@BadgeColourAllowed` (custom, class-level) | an option's `badgeColour` | `annotation.field.option.colour.invalid` |
| `@NumberBoundsValid` (custom, class-level) | `numberMin`/`numberMax` | `annotation.field.number.bounds.invalid` |
| `@SecretAllowedForFieldType` (custom, class-level) | `secret` | `annotation.field.secret.not_allowed` |

### 2. Business / stateful / binary errors → **specific domain exceptions**
Each extends `domain/error/DomainException` (HTTP-free; carries the message key + a domain `ErrorCategory`);
`api/error/DomainExceptionMapper` maps category → status and resolves the localized message.

| Exception | HTTP | Message key | When |
|---|---|---|---|
| `AnnotationTypeNotFoundException` | 404 | `annotation.type.not_found` | type id absent **or** another tenant's (no disclosure) |
| `AnnotationTypeNameTakenException` | 409 | `annotation.type.name.taken` | type `name` already exists in this tenant |
| `ImageNotFoundException` | 404 | `annotation.image.not_found` | image id absent/foreign, or referenced `iconImageId` missing |
| `ImageTooLargeException` | 400 | `annotation.image.too_large` | upload > 5 MB (NFR-04) |
| `UnsupportedImageTypeException` | 400 | `annotation.image.type.unsupported` | upload content-type not in the allowed set (C-07) |

Unauthenticated (401) stays feat-001's `UnauthorizedExceptionMapper` (`AUTH_REQUIRED`) until OQ-16 unifies
feat-001 onto this scheme.

**Validation-error envelope** (extends `Problem` with a violations list — still one wire shape):
```jsonc
{ "code": "validation.failed", "message": "<localized summary>", "correlationId": "uuid",
  "violations": [ { "field": "fields[0].secret", "code": "annotation.field.secret.not_allowed",
                    "message": "<localized>" } ] }
```

## Service / repository interfaces (`application` + `infrastructure.persistence`)

```java
// infrastructure.persistence
@ApplicationScoped class AnnotationTypeRepository extends TenantScopedRepository<AnnotationType> { … }
@ApplicationScoped class ImageRepository          extends TenantScopedRepository<Image>          { … }
@ApplicationScoped class AuditLogRepository        extends TenantScopedRepository<AuditLog>       { … } // write + tenant list

// application  (@ApplicationScoped, @Transactional on write use-cases — AD-09)
interface AnnotationTypeService {
  AnnotationType create(AnnotationTypeInput in);        // stateful checks → persistInTenant
  List<AnnotationType> list(int page, int size);        // tenant-scoped list
  AnnotationType get(UUID id);                          // or throw AnnotationTypeNotFoundException
  AnnotationType replace(UUID id, AnnotationTypeInput in);
  void delete(UUID id);                                 // + AuditLog in same tx (C-10)
}
interface ImageService {
  Image store(String contentType, byte[] bytes);        // ImageTooLarge/UnsupportedImageType → persistInTenant
  Image get(UUID id);                                   // tenant-scoped or ImageNotFoundException
}
```

**Division of labour (constitution §Errors).** *Input-shape* rules are **Bean Validation** on the input
records — built-in (`@NotBlank`, `@Size(max=120)`) and the **custom constraints** in §Errors table 1
(`@ValidFieldType`, `@OptionsAllowedForFieldType`, `@BadgeColourAllowed`, `@NumberBoundsValid`,
`@SecretAllowedForFieldType`). *Stateful/binary* rules a constraint cannot express — **name uniqueness**,
**id existence**, **image type/size**, **image-reference existence** — raise the **specific exceptions** in
§Errors table 2 from `AnnotationTypeService` / `ImageService`.

## Docs
Every endpoint documented in OpenAPI (NFR-06). `quarkus-smallrye-openapi` is **not yet** in `pom.xml`
— add it so the contract is generated from the JAX-RS annotations (the one extension this feature adds).
