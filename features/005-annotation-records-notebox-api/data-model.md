# Data model — Annotation records

Entities follow feat-003 conventions: UUID `CHAR(36)` PKs via `@JdbcTypeCode(SqlTypes.CHAR)`, `DATETIME(6)`
timestamps, `BIT(1)` booleans, `DECIMAL(38,10)` numbers, `ENGINE=InnoDB`, ordered children via
`@JoinColumn`, tenant scoping on the **root only**.

## Entities

### `AnnotationRecord` (aggregate root, `implements TenantOwned`)
| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK, assigned in `@PrePersist` |
| `tenantId` | `UUID` | `NOT NULL`; scoping key (BR-01/BR-02) |
| `annotationTypeId` | `UUID` | `NOT NULL`, FK → `annotation_type.id` |
| `name` | `String` | `NOT NULL`, `@NotBlank`, `@Size(max=120)` (validation baseline: annotation name required) |
| `createdAt` | `Instant` | set in `@PrePersist` |
| `updatedAt` | `Instant` | set in `@PrePersist`/`@PreUpdate` |
| `values` | `List<AnnotationValue>` | `@OneToMany(cascade=ALL, orphanRemoval=true) @JoinColumn(annotation_record_id)`; `replaceValues()` for PUT |

**Invariants:** every value's `typeFieldId` ∈ the referenced type's field ids (BR-03); at most one value per
field id. Records are tenant-owned and only reachable through `TenantScopedRepository<AnnotationRecord>`.

### `AnnotationValue` (aggregate-internal — **no `tenant_id`**)
| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | PK |
| `typeFieldId` | `UUID` | `NOT NULL`; which `TypeField` this value is for |
| `textValue` | `String` (`TEXT`) | non-secret TEXT/FREE_TEXT only; `NULL` otherwise and always `NULL` for secret |
| `numberValue` | `BigDecimal(38,10)` | NUMBER only |
| `imageId` | `UUID` | IMAGE only; references `image.id`, validated via `ImageService.existsInTenant` |
| `secretCiphertext` | `byte[]` (`VARBINARY`) | secret TEXT/FREE_TEXT only |
| `secretIv` | `byte[]` (`VARBINARY(12)`) | 96-bit GCM IV, secret only |
| `secretKeyVersion` | `Integer` | key-version used, secret only (AD-14 rotation) |
| `selectedOptionIds` | `Set<UUID>` | `@ElementCollection` → `annotation_value_option`; SINGLE_CHOICE/LIST = exactly 1, MULTIPLE_CHOICE = 0..n; each ∈ the field's `FieldOption` ids |

**One-of payload invariant** (service-enforced by `fieldType`):
- TEXT/FREE_TEXT non-secret → `textValue`; secret → `secretCiphertext`+`secretIv`+`secretKeyVersion`
- NUMBER → `numberValue` (within `numberMin/max`)
- IMAGE → `imageId`
- SINGLE_CHOICE/LIST → one `selectedOptionIds`; MULTIPLE_CHOICE → set of `selectedOptionIds`

## Migration — `V3__annotation_records.sql` (Flyway, after `V2__annotation_types.sql`)

```sql
CREATE TABLE annotation_record (
  id                 CHAR(36)     NOT NULL,
  tenant_id          CHAR(36)     NOT NULL,
  annotation_type_id CHAR(36)     NOT NULL,
  name               VARCHAR(120) NOT NULL,
  created_at         DATETIME(6)  NOT NULL,
  updated_at         DATETIME(6)  NOT NULL,
  CONSTRAINT pk_annotation_record PRIMARY KEY (id),
  CONSTRAINT fk_record_type FOREIGN KEY (annotation_type_id) REFERENCES annotation_type (id)
) ENGINE=InnoDB;
CREATE INDEX ix_record_tenant       ON annotation_record (tenant_id);
CREATE INDEX ix_record_type         ON annotation_record (annotation_type_id);

CREATE TABLE annotation_value (
  id                  CHAR(36)       NOT NULL,
  annotation_record_id CHAR(36)      NOT NULL,
  type_field_id       CHAR(36)       NOT NULL,
  text_value          TEXT           NULL,
  number_value        DECIMAL(38,10) NULL,
  image_id            CHAR(36)       NULL,
  secret_ciphertext   VARBINARY(4096) NULL,
  secret_iv           VARBINARY(12)  NULL,
  secret_key_version  INT            NULL,
  CONSTRAINT pk_annotation_value PRIMARY KEY (id),
  CONSTRAINT fk_value_record FOREIGN KEY (annotation_record_id)
    REFERENCES annotation_record (id) ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE INDEX ix_value_record ON annotation_value (annotation_record_id);
CREATE INDEX ix_value_field  ON annotation_value (type_field_id);

CREATE TABLE annotation_value_option (
  annotation_value_id CHAR(36) NOT NULL,
  field_option_id     CHAR(36) NOT NULL,
  CONSTRAINT pk_annotation_value_option PRIMARY KEY (annotation_value_id, field_option_id),
  CONSTRAINT fk_avo_value FOREIGN KEY (annotation_value_id)
    REFERENCES annotation_value (id) ON DELETE CASCADE
) ENGINE=InnoDB;
```

Notes: `ON DELETE CASCADE` mirrors JPA `orphanRemoval` at the DB level for defence-in-depth. `image_id` and
`field_option_id` are validated in the application layer (soft references) rather than hard FKs, matching
feat-003's `iconImageId` loose-reference choice (survey point 7). `secret_ciphertext` sized for the
GCM-encrypted form of a bounded secret text value; widen in a later migration if a larger secret type is
introduced.
