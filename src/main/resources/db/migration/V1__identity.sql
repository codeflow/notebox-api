-- V1 — identity & tenancy schema (feature 001)
-- Tenant is the tenancy root; app_user is tenant-owned (BR-02). `user` is reserved in MySQL,
-- hence `app_user`.

CREATE TABLE tenant (
    id         CHAR(36)     NOT NULL,
    name       VARCHAR(120) NOT NULL,
    slug       VARCHAR(60)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_tenant PRIMARY KEY (id),
    CONSTRAINT uq_tenant_slug UNIQUE (slug)
) ENGINE = InnoDB;

CREATE TABLE app_user (
    id                CHAR(36)     NOT NULL,
    tenant_id         CHAR(36)     NOT NULL,
    display_name      VARCHAR(120) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    locale_preference VARCHAR(5)   NULL,
    password_hash     VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    active            BIT(1)       NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT fk_app_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
) ENGINE = InnoDB;

-- Backs tenant-scoped queries (AD-03).
CREATE INDEX ix_app_user_tenant ON app_user (tenant_id);
