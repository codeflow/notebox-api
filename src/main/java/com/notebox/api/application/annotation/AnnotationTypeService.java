package com.notebox.api.application.annotation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.AnnotationTypeInput;
import com.notebox.api.api.dto.FieldOptionInput;
import com.notebox.api.api.dto.TypeFieldInput;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.AnnotationTypeFieldHasRecordsException;
import com.notebox.api.domain.error.AnnotationTypeFieldSecretFlipException;
import com.notebox.api.domain.error.AnnotationTypeHasRecordsException;
import com.notebox.api.domain.error.AnnotationTypeNameTakenException;
import com.notebox.api.domain.error.AnnotationTypeNotFoundException;
import com.notebox.api.domain.error.ImageNotFoundException;
import com.notebox.api.infrastructure.persistence.AnnotationRecordRepository;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for annotation types (FR-01/02/03). Input shape is already validated at the api edge
 * (AD-07); this layer enforces the stateful rules — per-tenant name uniqueness and icon-reference
 * existence — and applies the per-field-type defaults (D2, Secret). Writes are transactional (AD-09).
 */
@ApplicationScoped
public class AnnotationTypeService {

    private static final String TARGET_ANNOTATION_TYPE = "ANNOTATION_TYPE";
    private static final String ACTION_TYPE_DELETED = "ANNOTATION_TYPE_DELETED";

    private final AnnotationTypeRepository repository;
    private final AnnotationRecordRepository records;
    private final AuditLogRepository auditLog;
    private final ImageService images;
    private final TenantContext tenant;

    public AnnotationTypeService(
            AnnotationTypeRepository repository,
            AnnotationRecordRepository records,
            AuditLogRepository auditLog,
            ImageService images,
            TenantContext tenant) {
        this.repository = repository;
        this.records = records;
        this.auditLog = auditLog;
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

    /**
     * Replaces the whole definition (PUT): rename, re-icon, and reorder/add/remove fields and
     * options. Name-matched fields keep their identity — and therefore every record's values
     * (OQ-17): an identical resend or a type rename never orphans values. Removing or retyping a
     * field while the type owns records, or flipping a field's Secret flag while it holds values
     * (OQ-18), is rejected as a conflict.
     */
    @Transactional
    public AnnotationType replace(UUID id, AnnotationTypeInput input) {
        AnnotationType type = get(id);
        if (!type.getName().equals(input.name()) && repository.existsByName(input.name())) {
            throw new AnnotationTypeNameTakenException();
        }
        requireImageExists(input.iconImageId());
        type.setName(input.name());
        type.setIconImageId(input.iconImageId());
        applyFields(type, input.fieldsOrEmpty());
        return type;
    }

    /**
     * Matches incoming fields to existing ones by name, preserving matched identities (OQ-17).
     * Same-named fields match in declaration order (FIFO), so duplicates cannot slip past the
     * removal guard unmatched (audit R2-02).
     */
    private void applyFields(AnnotationType type, List<TypeFieldInput> inputs) {
        boolean hasRecords = records.existsByType(type.getId());
        Map<String, Deque<TypeField>> unmatched = new LinkedHashMap<>();
        for (TypeField field : type.getFields()) {
            unmatched.computeIfAbsent(field.getName(), name -> new ArrayDeque<>()).add(field);
        }
        List<TypeField> result = new ArrayList<>();
        for (TypeFieldInput input : inputs) {
            Deque<TypeField> candidates = unmatched.get(input.name());
            TypeField existing = candidates == null ? null : candidates.poll();
            if (existing == null) {
                result.add(toField(input));
                continue;
            }
            if (existing.getFieldType() != FieldType.valueOf(input.fieldType())) {
                if (hasRecords) {
                    throw new AnnotationTypeFieldHasRecordsException();
                }
                result.add(toField(input));
                continue;
            }
            result.add(updateField(existing, input));
        }
        boolean leftovers = unmatched.values().stream().anyMatch(queue -> !queue.isEmpty());
        if (leftovers && hasRecords) {
            throw new AnnotationTypeFieldHasRecordsException();
        }
        type.replaceFields(result);
    }

    /** In-place update of a preserved field; the Secret flag is immutable while values exist (OQ-18). */
    private TypeField updateField(TypeField field, TypeFieldInput input) {
        boolean secret = Boolean.TRUE.equals(input.secret());
        if (field.isSecret() != secret && records.existsValueForField(field.getId())) {
            throw new AnnotationTypeFieldSecretFlipException();
        }
        requireImageExists(input.iconImageId());
        field.setIconImageId(input.iconImageId());
        field.setVisibleForViewing(
                input.visibleForViewing() != null
                        ? input.visibleForViewing()
                        : field.getFieldType().defaultVisibleForViewing());
        field.setSecret(secret);
        if (field.getFieldType() == FieldType.NUMBER) {
            field.setNumberBounds(input.numberMin(), input.numberMax());
        }
        updateOptions(field, input);
        return field;
    }

    /**
     * Label-matched options keep their identity so an unchanged choice value's selection survives
     * the PUT (OQ-17). Same-labelled options match in declaration order (FIFO), so an identical
     * resend cannot orphan a duplicate and dangle the selection pointing at it (audit R3-01);
     * dropped labels are removed with feat-003 replace semantics (type-evolution rules for values
     * referencing them are a deferred future feature).
     */
    private void updateOptions(TypeField field, TypeFieldInput input) {
        Map<String, Deque<FieldOption>> unmatched = new LinkedHashMap<>();
        for (FieldOption option : field.getOptions()) {
            unmatched.computeIfAbsent(option.getLabel(), label -> new ArrayDeque<>()).add(option);
        }
        List<FieldOption> result = new ArrayList<>();
        for (FieldOptionInput optionInput : input.optionsOrEmpty()) {
            BadgeColour colour =
                    optionInput.badgeColour() == null ? null : BadgeColour.valueOf(optionInput.badgeColour());
            Deque<FieldOption> candidates = unmatched.get(optionInput.label());
            FieldOption existing = candidates == null ? null : candidates.poll();
            if (existing == null) {
                result.add(new FieldOption(optionInput.label(), colour));
            } else {
                existing.setBadgeColour(colour);
                result.add(existing);
            }
        }
        field.replaceOptions(result);
    }

    /**
     * Deletes an (empty) type irreversibly and records the action in the audit trail (BR-05, C-10).
     * Blocked while the type still owns records (OQ-14): they must be deleted first.
     */
    @Transactional
    public void delete(UUID id) {
        AnnotationType type = get(id);
        if (records.existsByType(id)) {
            throw new AnnotationTypeHasRecordsException();
        }
        type.getFields().size(); // initialize children so the cascade delete removes them
        repository.remove(type);
        auditLog.persistInTenant(
                new AuditLog(tenant.tenantId(), tenant.userId(), ACTION_TYPE_DELETED, TARGET_ANNOTATION_TYPE, id));
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
        if (fieldType == FieldType.NUMBER) {
            field.setNumberBounds(input.numberMin(), input.numberMax());
        }
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
