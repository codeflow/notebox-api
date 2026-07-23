package com.notebox.api.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * The tenant-owned schema of a category of annotations (BR-03): a name (unique per tenant), an
 * optional icon image reference, and an ordered list of {@link TypeField}s it owns. The aggregate
 * root — its fields and their options are persisted and removed only through it (AD-03).
 */
@Entity
@Table(name = "annotation_type")
public class AnnotationType implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "icon_image_id", length = 36)
    private UUID iconImageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "annotation_type_id", nullable = false)
    @OrderColumn(name = "position")
    private List<TypeField> fields = new ArrayList<>();

    protected AnnotationType() {
    }

    public AnnotationType(UUID tenantId, String name) {
        this.tenantId = tenantId;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public UUID getIconImageId() {
        return iconImageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TypeField> getFields() {
        return fields;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIconImageId(UUID iconImageId) {
        this.iconImageId = iconImageId;
    }

    public void addField(TypeField field) {
        this.fields.add(field);
    }

    /** Replaces the whole ordered field list (PUT semantics); orphanRemoval deletes the dropped ones. */
    public void replaceFields(List<TypeField> newFields) {
        this.fields.clear();
        this.fields.addAll(newFields);
    }
}
