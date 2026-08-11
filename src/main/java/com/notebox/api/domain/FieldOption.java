package com.notebox.api.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A predefined, selectable option on a List / Single choice / Multiple choice field (FR-03).
 * Aggregate-internal: reached only through its {@link TypeField}. A badge colour is meaningful only
 * for List fields.
 */
@Entity
@Table(name = "field_option")
public class FieldOption {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private UUID id;

    @Column(name = "label", length = 120, nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_colour", length = 10)
    private BadgeColour badgeColour;

    protected FieldOption() {
    }

    public FieldOption(String label, BadgeColour badgeColour) {
        this.label = label;
        this.badgeColour = badgeColour;
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

    public String getLabel() {
        return label;
    }

    public BadgeColour getBadgeColour() {
        return badgeColour;
    }

    public void setBadgeColour(BadgeColour badgeColour) {
        this.badgeColour = badgeColour;
    }
}
