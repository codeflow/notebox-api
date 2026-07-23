package com.notebox.api.application.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.FieldOptionInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.AnnotationTypeNameTakenException;
import com.notebox.api.domain.error.AnnotationTypeNotFoundException;
import com.notebox.api.domain.error.ImageNotFoundException;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for annotation types (FR-01/02/03). Input shape is already validated at the api edge
 * (AD-07); this layer enforces the stateful rules — per-tenant name uniqueness and icon-reference
 * existence — and applies the per-field-type defaults (D2, Secret). Writes are transactional (AD-09).
 */
@ApplicationScoped
public class AnnotationTypeService {

    private final AnnotationTypeRepository repository;
    private final ImageService images;
    private final TenantContext tenant;

    public AnnotationTypeService(AnnotationTypeRepository repository, ImageService images, TenantContext tenant) {
        this.repository = repository;
        this.images = images;
        this.tenant = tenant;
    }

    @Transactional
    public AnnotationType create(AnnotationTypeInput input) {
        if (repository.existsByName(input.name())) {
            throw new AnnotationTypeNameTakenException();
        }
        requireImageExists(input.iconImageId());
        AnnotationType type = new AnnotationType(tenant.tenantId(), input.name());
        type.setIconImageId(input.iconImageId());
        for (TypeFieldInput field : input.fieldsOrEmpty()) {
            type.addField(toField(field));
        }
        return repository.persistInTenant(type);
    }

    public List<AnnotationType> list(int page, int size) {
        return repository.listInTenant(page, size);
    }

    public AnnotationType get(UUID id) {
        return repository.findByIdInTenant(id).orElseThrow(AnnotationTypeNotFoundException::new);
    }

    /** Builds a field from its input, applying the per-field-type visibility (D2) and Secret defaults. */
    TypeField toField(TypeFieldInput input) {
        requireImageExists(input.iconImageId());
        FieldType fieldType = FieldType.valueOf(input.fieldType());
        TypeField field = new TypeField(input.name(), fieldType);
        field.setIconImageId(input.iconImageId());
        field.setVisibleForViewing(
                input.visibleForViewing() != null
                        ? input.visibleForViewing()
                        : fieldType.defaultVisibleForViewing());
        field.setSecret(Boolean.TRUE.equals(input.secret()));
        field.setNumberBounds(input.numberMin(), input.numberMax());
        for (FieldOptionInput option : input.optionsOrEmpty()) {
            BadgeColour colour = option.badgeColour() == null ? null : BadgeColour.valueOf(option.badgeColour());
            field.addOption(new FieldOption(option.label(), colour));
        }
        return field;
    }

    List<TypeField> toFields(List<TypeFieldInput> inputs) {
        List<TypeField> fields = new ArrayList<>();
        for (TypeFieldInput input : inputs) {
            fields.add(toField(input));
        }
        return fields;
    }

    void requireImageExists(UUID imageId) {
        if (!images.existsInTenant(imageId)) {
            throw new ImageNotFoundException();
        }
    }
}
