package com.notebox.api.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A field declared on an {@link AnnotationType}: its name, one of the seven {@link FieldType} kinds
 * (BR-04), and the design-time metadata (icon, visibility, Secret flag, Number bounds, and — for the
 * choice kinds — an ordered option list). Aggregate-internal: reached only through its owning type.
 */
@Entity
@Table(name = "type_field")
public class TypeField {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", length = 20, nullable = false)
    private FieldType fieldType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "icon_image_id", length = 36)
    private UUID iconImageId;

    @Column(name = "visible_for_viewing", nullable = false)
    private boolean visibleForViewing;

    @Column(name = "secret", nullable = false)
    private boolean secret;

    @Column(name = "number_min", precision = 38, scale = 10)
    private BigDecimal numberMin;

    @Column(name = "number_max", precision = 38, scale = 10)
    private BigDecimal numberMax;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "type_field_id", nullable = false)
    @OrderColumn(name = "position")
    private List<FieldOption> options = new ArrayList<>();

    protected TypeField() {
    }

    public TypeField(String name, FieldType fieldType) {
        this.name = name;
        this.fieldType = fieldType;
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

    public String getName() {
        return name;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public UUID getIconImageId() {
        return iconImageId;
    }

    public boolean isVisibleForViewing() {
        return visibleForViewing;
    }

    public boolean isSecret() {
        return secret;
    }

    public BigDecimal getNumberMin() {
        return numberMin;
    }

    public BigDecimal getNumberMax() {
        return numberMax;
    }

    public List<FieldOption> getOptions() {
        return options;
    }

    public void setIconImageId(UUID iconImageId) {
        this.iconImageId = iconImageId;
    }

    public void setVisibleForViewing(boolean visibleForViewing) {
        this.visibleForViewing = visibleForViewing;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public void setNumberBounds(BigDecimal numberMin, BigDecimal numberMax) {
        this.numberMin = numberMin;
        this.numberMax = numberMax;
    }

    public void addOption(FieldOption option) {
        this.options.add(option);
    }
}
