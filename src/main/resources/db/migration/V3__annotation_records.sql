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
