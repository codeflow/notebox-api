package com.notebox.api.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One value of an {@link AnnotationRecord} for a given {@code TypeField} (BR-03): exactly the
 * payload matching the field's {@link FieldType} is populated. A Secret Text/Free-text value is
 * stored as ciphertext + IV + key-version (AD-14); {@code textValue} stays {@code null} for it.
 * Aggregate-internal — no {@code tenant_id}; reached only through its owning record.
 */
@Entity
@Table(name = "annotation_value")
public class AnnotationValue {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "type_field_id", length = 36, nullable = false, updatable = false)
    private UUID typeFieldId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "image_id", length = 36)
    private UUID imageId;

    @Lob
    @Column(name = "text_value")
    private String textValue;

    @Column(name = "number_value", precision = 38, scale = 10)
    private BigDecimal numberValue;

    @Column(name = "secret_ciphertext")
    private byte[] secretCiphertext;

    @Column(name = "secret_iv")
    private byte[] secretIv;

    @Column(name = "secret_key_version")
    private Integer secretKeyVersion;

    @ElementCollection
    @CollectionTable(name = "annotation_value_option", joinColumns = @JoinColumn(name = "annotation_value_id"))
    @Column(name = "field_option_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private Set<UUID> selectedOptionIds = new HashSet<>();

    protected AnnotationValue() {
    }

    public AnnotationValue(UUID typeFieldId) {
        this.typeFieldId = typeFieldId;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTypeFieldId() {
        return typeFieldId;
    }

    public UUID getImageId() {
        return imageId;
    }

    public String getTextValue() {
        return textValue;
    }

    public BigDecimal getNumberValue() {
        return numberValue;
    }

    public byte[] getSecretCiphertext() {
        return secretCiphertext;
    }

    public byte[] getSecretIv() {
        return secretIv;
    }

    public Integer getSecretKeyVersion() {
        return secretKeyVersion;
    }

    public Set<UUID> getSelectedOptionIds() {
        return selectedOptionIds;
    }

    /** Whether this value carries an encrypted (Secret-field) payload rather than plaintext. */
    public boolean isSecret() {
        return secretCiphertext != null;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public void setNumberValue(BigDecimal numberValue) {
        this.numberValue = numberValue;
    }

    public void setImageId(UUID imageId) {
        this.imageId = imageId;
    }

    /** Stores the encrypted payload of a Secret field value (AD-14); {@code textValue} stays null. */
    public void setSecret(byte[] ciphertext, byte[] iv, int keyVersion) {
        this.secretCiphertext = ciphertext;
        this.secretIv = iv;
        this.secretKeyVersion = keyVersion;
    }

    public void setSelectedOptionIds(Set<UUID> optionIds) {
        this.selectedOptionIds = new HashSet<>(optionIds);
    }
}
