-- V2 — annotation types schema (feature 003)
-- AnnotationType is the tenant-owned aggregate root (AD-03); TypeField and FieldOption are
-- aggregate-internal children (no tenant_id — reached only through the root). Image and audit_log
-- are independent tenant-owned entities. Icons are stored as BLOBs and referenced by id (AD-04).

CREATE TABLE image (
    id           CHAR(36)    NOT NULL,
    tenant_id    CHAR(36)    NOT NULL,
    content_type VARCHAR(40) NOT NULL,
    size_bytes   BIGINT      NOT NULL,
    bytes        LONGBLOB    NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    CONSTRAINT pk_image PRIMARY KEY (id),
    CONSTRAINT fk_image_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB;

CREATE INDEX ix_image_tenant ON image (tenant_id);

CREATE TABLE annotation_type (
    id            CHAR(36)     NOT NULL,
    tenant_id     CHAR(36)     NOT NULL,
    name          VARCHAR(120) NOT NULL,
    icon_image_id CHAR(36)     NULL,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_annotation_type PRIMARY KEY (id),
    CONSTRAINT uq_annotation_type_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT fk_annotation_type_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_annotation_type_icon FOREIGN KEY (icon_image_id) REFERENCES image (id)
) ENGINE = InnoDB;

CREATE INDEX ix_annotation_type_tenant ON annotation_type (tenant_id);

CREATE TABLE type_field (
    id                  CHAR(36)       NOT NULL,
    annotation_type_id  CHAR(36)       NOT NULL,
    name                VARCHAR(120)   NOT NULL,
    field_type          VARCHAR(20)    NOT NULL,
    icon_image_id       CHAR(36)       NULL,
    visible_for_viewing BIT(1)         NOT NULL,
    secret              BIT(1)         NOT NULL DEFAULT 0,
    number_min          DECIMAL(38,10) NULL,
    number_max          DECIMAL(38,10) NULL,
    position            INT            NOT NULL,
    CONSTRAINT pk_type_field PRIMARY KEY (id),
    CONSTRAINT fk_type_field_type FOREIGN KEY (annotation_type_id) REFERENCES annotation_type (id),
    CONSTRAINT fk_type_field_icon FOREIGN KEY (icon_image_id) REFERENCES image (id)
) ENGINE = InnoDB;

CREATE INDEX ix_type_field_type ON type_field (annotation_type_id, position);

CREATE TABLE field_option (
    id            CHAR(36)     NOT NULL,
    type_field_id CHAR(36)     NOT NULL,
    label         VARCHAR(120) NOT NULL,
    badge_colour  VARCHAR(10)  NULL,
    position      INT          NOT NULL,
    CONSTRAINT pk_field_option PRIMARY KEY (id),
    CONSTRAINT fk_field_option_field FOREIGN KEY (type_field_id) REFERENCES type_field (id)
) ENGINE = InnoDB;

CREATE INDEX ix_field_option_field ON field_option (type_field_id, position);

CREATE TABLE audit_log (
    id            CHAR(36)    NOT NULL,
    tenant_id     CHAR(36)    NOT NULL,
    actor_user_id CHAR(36)    NOT NULL,
    action        VARCHAR(60) NOT NULL,
    target_type   VARCHAR(60) NOT NULL,
    target_id     CHAR(36)    NOT NULL,
    at            DATETIME(6) NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id),
    CONSTRAINT fk_audit_log_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB;

CREATE INDEX ix_audit_log_tenant ON audit_log (tenant_id);
