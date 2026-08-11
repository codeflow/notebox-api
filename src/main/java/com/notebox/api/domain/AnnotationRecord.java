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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-owned instance of an {@link AnnotationType} (BR-03): a name and a value per populated
 * field, each validated for conformance to the type's schema before persistence. The aggregate
 * root — its values are persisted and removed only through it (AD-03).
 */
@Entity
@Table(name = "annotation_record")
public class AnnotationRecord implements TenantOwned {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", length = 36, nullable = false, updatable = false)
    private UUID tenantId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "annotation_type_id", length = 36, nullable = false, updatable = false)
    private UUID annotationTypeId;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "annotation_record_id", nullable = false)
    private List<AnnotationValue> values = new ArrayList<>();

    protected AnnotationRecord() {
    }

    public AnnotationRecord(UUID tenantId, UUID annotationTypeId, String name) {
        this.tenantId = tenantId;
        this.annotationTypeId = annotationTypeId;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAnnotationTypeId() {
        return annotationTypeId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<AnnotationValue> getValues() {
        return values;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addValue(AnnotationValue value) {
        this.values.add(value);
    }

    /** Replaces the whole value set (PUT semantics); orphanRemoval deletes the dropped ones. */
    public void replaceValues(List<AnnotationValue> newValues) {
        this.values.clear();
        this.values.addAll(newValues);
    }
}
