# Data model — Annotation types (feat-003, US-1.1)

> Entities, fields, invariants and the V2 migration. Invariants trace to the BRs cited in the spec
> (BR-03, BR-04, BR-05). Mirrors feat-001 conventions: UUID stored `CHAR(36)` via
> `@JdbcTypeCode(SqlTypes.CHAR)`, no `@GeneratedValue` (ids assigned in `@PrePersist`), enums
> `@Enumerated(STRING)` → `VARCHAR`, timestamps `DATETIME(6)` via `Instant`, `BIT(1)` booleans,
> InnoDB, named constraints, `ix_<table>_tenant` index behind AD-03.

## Aggregate & tenancy boundary (one-way)

- **`AnnotationType` is the tenant-owned aggregate root** (`implements TenantOwned`). It owns its
  `TypeField`s, which own their `FieldOption`s. Children are **aggregate-internal**: they carry **no
  `tenant_id`** and have **no repository of their own** — they are reached, persisted (cascade) and
  removed (orphanRemoval) only through the root. This satisfies AD-03: there is no code path to a
  child except through the tenant-scoped root repository.
- **`Image`** and **`AuditLog`** are **independent tenant-owned entities** (`implements TenantOwned`),
  each with its own `TenantScopedRepository`, because both are looked up directly by id (the binary
  endpoint fetches an image; audit is queried per tenant).

## Entities

### AnnotationType  *(root, TenantOwned)*
| Field | Type | Column | Notes |
|---|---|---|---|
| id | UUID | `id CHAR(36)` PK | assigned in `@PrePersist` |
| tenantId | UUID | `tenant_id CHAR(36)` NOT NULL, immutable | AD-03; `getTenantId()` |
| name | String | `name VARCHAR(120)` NOT NULL | trimmed; unique per tenant |
| iconImageId | UUID? | `icon_image_id CHAR(36)` NULL, FK→`image(id)` | optional type icon (AD-04 reference) |
| fields | List\<TypeField\> | — | `@OneToMany(cascade=ALL, orphanRemoval=true)` + `@OrderColumn(position)`; declared order preserved |
| createdAt | Instant | `created_at DATETIME(6)` NOT NULL | `@PrePersist` |

**Invariants**
- `name` required and **unique within `tenant_id`** (DB `uq_annotation_type_tenant_name (tenant_id, name)` + pre-write service check → `AnnotationTypeNameTakenException` (`annotation.type.name.taken`, 409)). Uniqueness is per-tenant: the same name may exist in two tenants.
- A type is the **schema** of its future annotations (BR-03); it exists independently of any record.
- Referenced `iconImageId` must resolve to an `Image` in the same tenant.

### TypeField  *(aggregate-internal child)*
| Field | Type | Column | Notes |
|---|---|---|---|
| id | UUID | `id CHAR(36)` PK | `@PrePersist` |
| annotationTypeId | UUID | `annotation_type_id CHAR(36)` NOT NULL, FK→`annotation_type(id)` | parent |
| name | String | `name VARCHAR(120)` NOT NULL | required |
| fieldType | FieldType | `field_type VARCHAR(20)` NOT NULL | closed set of 7 (BR-04) |
| iconImageId | UUID? | `icon_image_id CHAR(36)` NULL, FK→`image(id)` | optional field icon (same image mechanism as type icon) |
| visibleForViewing | boolean | `visible_for_viewing BIT(1)` NOT NULL | default **per field type** (D2), applied at create when omitted |
| secret | boolean | `secret BIT(1)` NOT NULL | default `false`; **only TEXT / FREE_TEXT** may be `true` (human decision 2026-07-23). Declaration only — value encryption/reveal is US-2.1 (OQ-15) |
| numberMin | BigDecimal? | `number_min DECIMAL(38,10)` NULL | Number fields only; optional |
| numberMax | BigDecimal? | `number_max DECIMAL(38,10)` NULL | Number fields only; optional |
| position | int | `position INT` NOT NULL | `@OrderColumn` within the type |
| options | List\<FieldOption\> | — | `@OneToMany(cascade=ALL, orphanRemoval=true)` + `@OrderColumn` |

**Invariants**
- `name` required (`@NotBlank` → `annotation.field.name.required`).
- `fieldType` ∈ {TEXT, LIST, NUMBER, FREE_TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, IMAGE} (BR-04). An unrecognized value → `@ValidFieldType` → `annotation.field.type.unknown`.
- **Options allowed only** when `fieldType ∈ {LIST, SINGLE_CHOICE, MULTIPLE_CHOICE}`; options on any other type → `@OptionsAllowedForFieldType` → `annotation.field.options.not_allowed`.
- `numberMin`/`numberMax` meaningful only for NUMBER; when both present, `min ≤ max` else `@NumberBoundsValid` → `annotation.field.number.bounds.invalid`.
- **Default visibility (D2):** TEXT, LIST, NUMBER → `true`; FREE_TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, IMAGE → `false`. Computed by `FieldType.defaultVisibleForViewing()` when the request omits the flag; an explicit value always wins.
- **`secret`** defaults to `false`; may be `true` **only** when `fieldType ∈ {TEXT, FREE_TEXT}` (`FieldType.allowsSecret()`), else `@SecretAllowedForFieldType` → `annotation.field.secret.not_allowed`. In feat-003 the flag is inert metadata on the schema; US-2.1 consumes it to encrypt values at rest and gate the reveal (OQ-15).

### FieldOption  *(aggregate-internal child)*
| Field | Type | Column | Notes |
|---|---|---|---|
| id | UUID | `id CHAR(36)` PK | `@PrePersist` |
| typeFieldId | UUID | `type_field_id CHAR(36)` NOT NULL, FK→`type_field(id)` | parent |
| label | String | `label VARCHAR(120)` NOT NULL | required |
| badgeColour | BadgeColour? | `badge_colour VARCHAR(10)` NULL | **List fields only**; from fixed palette |
| position | int | `position INT` NOT NULL | `@OrderColumn`; order preserved |

**Invariants**
- `label` required.
- `badgeColour` ∈ {RED, GREEN, BLUE, BLACK, GRAY, YELLOW} (palette). A value outside it → `@BadgeColourAllowed` → `annotation.field.option.colour.invalid`. A colour on a non-LIST field's option → `@BadgeColourAllowed` → `annotation.field.option.colour.invalid` (colour is a List-badge concept).

### Image  *(TenantOwned)*
| Field | Type | Column | Notes |
|---|---|---|---|
| id | UUID | `id CHAR(36)` PK | `@PrePersist` |
| tenantId | UUID | `tenant_id CHAR(36)` NOT NULL | AD-03 |
| contentType | String | `content_type VARCHAR(40)` NOT NULL | one of the allowed image types |
| sizeBytes | long | `size_bytes BIGINT` NOT NULL | ≤ 5 MB (NFR-04) |
| bytes | byte[] | `bytes LONGBLOB` NOT NULL | `@Lob` (AD-04; first BLOB in the codebase) |
| createdAt | Instant | `created_at DATETIME(6)` NOT NULL | `@PrePersist` |

**Invariants**
- `contentType` ∈ {`image/png`, `image/jpeg`, `image/gif`, `image/webp`} else `UnsupportedImageTypeException` (`annotation.image.type.unsupported`) (validation baseline OQ-09, C-07).
- `sizeBytes` ≤ 5 242 880 else `ImageTooLargeException` (`annotation.image.too_large`) (NFR-04) — enforced **before** persist (AD-07).
- Fetched only via tenant-scoped repository → a foreign tenant's image id returns not-found (C-01).

### AuditLog  *(TenantOwned — new audit infra for C-10)*
| Field | Type | Column | Notes |
|---|---|---|---|
| id | UUID | `id CHAR(36)` PK | `@PrePersist` |
| tenantId | UUID | `tenant_id CHAR(36)` NOT NULL | AD-03 |
| actorUserId | UUID | `actor_user_id CHAR(36)` NOT NULL | from `TenantContext.userId()` |
| action | String | `action VARCHAR(60)` NOT NULL | e.g. `ANNOTATION_TYPE_DELETED` |
| targetType | String | `target_type VARCHAR(60)` NOT NULL | e.g. `ANNOTATION_TYPE` |
| targetId | UUID | `target_id CHAR(36)` NOT NULL | the deleted type id |
| at | Instant | `at DATETIME(6)` NOT NULL | `@PrePersist` |

**Invariant:** the audit row is written in the **same transaction** as the irreversible delete (BR-05, C-10); a failed delete writes no audit row.

## Enums (domain)
- `FieldType { TEXT, LIST, NUMBER, FREE_TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, IMAGE }` — with `boolean defaultVisibleForViewing()` (D2), `boolean allowsOptions()` (LIST/SINGLE_CHOICE/MULTIPLE_CHOICE), `boolean allowsBadgeColour()` (LIST only), and `boolean allowsSecret()` (TEXT/FREE_TEXT only).
- `BadgeColour { RED, GREEN, BLUE, BLACK, GRAY, YELLOW }`.

## Migration — `V2__annotation_types.sql` (Flyway)

Tables in dependency order: `image`, `annotation_type`, `type_field`, `field_option`, `audit_log`.

- All PK/FK `CHAR(36)`; `tenant_id CHAR(36) NOT NULL` on `image`, `annotation_type`, `audit_log`, each with `fk_<t>_tenant → tenant(id)` and `ix_<t>_tenant` (AD-03).
- `annotation_type`: `uq_annotation_type_tenant_name (tenant_id, name)`; `fk_annotation_type_icon → image(id)` (nullable).
- `type_field`: `fk_type_field_type → annotation_type(id)`, `fk_type_field_icon → image(id)` (nullable), `field_type VARCHAR(20)`, `visible_for_viewing BIT(1)`, `secret BIT(1) NOT NULL DEFAULT 0`, `number_min/number_max DECIMAL(38,10)`, `position INT`, `ix_type_field_type (annotation_type_id, position)`.
- `field_option`: `fk_field_option_field → type_field(id)`, `badge_colour VARCHAR(10)` NULL, `position INT`, `ix_field_option_field (type_field_id, position)`.
- `image`: `bytes LONGBLOB NOT NULL`, `content_type VARCHAR(40)`, `size_bytes BIGINT`.
- `audit_log`: columns per entity above; `ix_audit_log_tenant`.
- `ENGINE = InnoDB` on every table.

**Reversibility:** the V2 schema and the FK graph are **one-way** (a migration + data). The choice of `DECIMAL(38,10)` for number bounds and `LONGBLOB` for image bytes are the deliberate width decisions.
