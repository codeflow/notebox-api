-- FR-16 · Runtime translation management.
--
-- The bundled messages[_xx].properties stay the product's defaults. This table holds a tenant's
-- OVERRIDES on top of them, so an administrator can reword a message without a deploy and a tenant
-- that overrides nothing keeps behaving exactly as before.
--
-- Uniqueness is per tenant AND per locale: the same key may be reworded differently in en and pt,
-- and one tenant's wording never reaches another's (BR-01, BR-02).
CREATE TABLE message_override (
  id           CHAR(36)     NOT NULL,
  tenant_id    CHAR(36)     NOT NULL,
  locale       VARCHAR(5)   NOT NULL,
  message_key  VARCHAR(120) NOT NULL,
  value        TEXT         NOT NULL,
  updated_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_message_override_tenant_locale_key UNIQUE (tenant_id, locale, message_key),
  CONSTRAINT fk_message_override_tenant FOREIGN KEY (tenant_id)
      REFERENCES tenant (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- The resolver's hot path is (tenant, locale, key); the unique constraint above already covers it,
-- so no further index is created.
