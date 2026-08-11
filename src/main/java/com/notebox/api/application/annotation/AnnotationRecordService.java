package com.notebox.api.application.annotation;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.api.dto.AnnotationRecordInput;
import com.notebox.api.api.dto.AnnotationValueInput;
import com.notebox.api.application.crypto.EncryptedValue;
import com.notebox.api.application.crypto.SecretValueCipher;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.AuditLog;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.Role;
import com.notebox.api.domain.TypeField;
import com.notebox.api.domain.error.AnnotationRecordFieldUnknownException;
import com.notebox.api.domain.error.AnnotationRecordNotFoundException;
import com.notebox.api.domain.error.AnnotationRecordRevealNotSecretException;
import com.notebox.api.domain.error.AnnotationRecordValueImageNotFoundException;
import com.notebox.api.domain.error.AnnotationRecordValueOptionUnknownException;
import com.notebox.api.domain.error.AnnotationRecordValueOutOfBoundsException;
import com.notebox.api.domain.error.AnnotationRecordValueTooLongException;
import com.notebox.api.domain.error.AnnotationRecordValueTypeMismatchException;
import com.notebox.api.domain.error.AnnotationTypeNotFoundException;
import com.notebox.api.domain.error.SecretRevealForbiddenException;
import com.notebox.api.infrastructure.persistence.AnnotationRecordRepository;
import com.notebox.api.infrastructure.persistence.AnnotationTypeRepository;
import com.notebox.api.infrastructure.persistence.AuditLogRepository;
import com.notebox.api.infrastructure.security.TenantContext;

/**
 * Use cases for annotation records (FR-04): create, read and update, each validated for
 * conformance to the owning type's schema (BR-03). Secret Text/Free-text values are encrypted on
 * write via {@link SecretValueCipher} (AD-14) and never decrypted by a plain read — masking on the
 * wire is representational (T-08), but the service itself never exposes cleartext outside reveal.
 */
@ApplicationScoped
public class AnnotationRecordService {

    private static final String TARGET_ANNOTATION_RECORD = "ANNOTATION_RECORD";
    private static final String ACTION_RECORD_DELETED = "ANNOTATION_RECORD_DELETED";
    private static final String ACTION_SECRET_REVEALED = "ANNOTATION_RECORD_SECRET_REVEALED";
    private static final String ACTION_SECRET_CLEARED = "ANNOTATION_RECORD_SECRET_CLEARED";

    // Storage bounds of V3: text_value TEXT; secret_ciphertext VARBINARY(4096) minus the 16-byte GCM tag.
    private static final int TEXT_VALUE_MAX_BYTES = 65535;
    private static final int SECRET_VALUE_MAX_BYTES = 4080;

    private final AnnotationRecordRepository records;
    private final AnnotationTypeRepository types;
    private final AuditLogRepository auditLog;
    private final ImageService images;
    private final SecretValueCipher cipher;
    private final TenantContext tenant;

    public AnnotationRecordService(
            AnnotationRecordRepository records,
            AnnotationTypeRepository types,
            AuditLogRepository auditLog,
            ImageService images,
            SecretValueCipher cipher,
            TenantContext tenant) {
        this.records = records;
        this.types = types;
        this.auditLog = auditLog;
        this.images = images;
        this.cipher = cipher;
        this.tenant = tenant;
    }

    @Transactional
    public AnnotationRecord create(AnnotationRecordInput input) {
        AnnotationType type = requireType(input.annotationTypeId());
        AnnotationRecord record = new AnnotationRecord(tenant.tenantId(), type.getId(), input.name());
        for (AnnotationValue value : toValues(type, input.valuesOrEmpty())) {
            record.addValue(value);
        }
        return records.persistInTenant(record);
    }

    /** Reads one record; a Secret value keeps its ciphertext — plain reads never decrypt. */
    public AnnotationRecord get(UUID id) {
        return records.findByIdInTenant(id).orElseThrow(AnnotationRecordNotFoundException::new);
    }

    /**
     * Replaces a record's name and values (PUT); the record's type never changes. Secret values
     * follow the decided semantics (human decision 2026-07-25, audit F4): omitted or echoed-masked
     * ({@code text: null}) → the stored ciphertext is preserved; a new {@code text} → re-encrypted;
     * {@code clearSecret: true} → erased with an audit entry (BR-05, BR-10).
     */
    @Transactional
    public AnnotationRecord update(UUID id, AnnotationRecordInput input) {
        AnnotationRecord record = get(id);
        AnnotationType type = requireType(record.getAnnotationTypeId());
        record.setName(input.name());
        record.replaceValues(toUpdatedValues(type, record, input.valuesOrEmpty()));
        return record;
    }

    /** Applies the F4 secret-preservation table on top of plain replace semantics for everything else. */
    private List<AnnotationValue> toUpdatedValues(
            AnnotationType type, AnnotationRecord record, List<AnnotationValueInput> inputs) {
        Map<UUID, TypeField> fieldsById =
                type.getFields().stream().collect(Collectors.toMap(TypeField::getId, Function.identity()));
        Map<UUID, AnnotationValue> existingByField = record.getValues().stream()
                .collect(Collectors.toMap(AnnotationValue::getTypeFieldId, Function.identity(), (a, b) -> a));
        Set<UUID> mentionedFields = new HashSet<>();
        List<AnnotationValue> values = new ArrayList<>();
        for (AnnotationValueInput input : inputs) {
            TypeField field = fieldsById.get(input.fieldId());
            if (field == null) {
                throw new AnnotationRecordFieldUnknownException();
            }
            mentionedFields.add(field.getId());
            if (!field.isSecret()) {
                values.add(toValue(field, input));
                continue;
            }
            AnnotationValue existing = existingByField.get(field.getId());
            if (input.clearSecretRequested()) {
                if (existing != null && existing.isSecret()) {
                    auditLog.persistInTenant(new AuditLog(
                            tenant.tenantId(), tenant.userId(), ACTION_SECRET_CLEARED,
                            TARGET_ANNOTATION_RECORD, record.getId(), "fieldId=" + field.getId()));
                }
                continue;
            }
            if (input.text() == null) {
                preservedSecret(field, existing).ifPresent(values::add);
                continue;
            }
            values.add(toValue(field, input));
        }
        for (AnnotationValue existing : record.getValues()) {
            TypeField field = fieldsById.get(existing.getTypeFieldId());
            if (field == null || !field.isSecret() || !existing.isSecret()
                    || mentionedFields.contains(field.getId())) {
                continue;
            }
            preservedSecret(field, existing).ifPresent(values::add);
        }
        return values;
    }

    /** Carries an existing ciphertext into the replacement value row, byte-identical (audit F4). */
    private Optional<AnnotationValue> preservedSecret(TypeField field, AnnotationValue existing) {
        if (existing == null || !existing.isSecret()) {
            return Optional.empty();
        }
        AnnotationValue preserved = new AnnotationValue(field.getId());
        preserved.setSecret(
                existing.getSecretCiphertext(), existing.getSecretIv(), existing.getSecretKeyVersion());
        return Optional.of(preserved);
    }

    /** Deletes a record irreversibly and records the action in the audit trail (FR-06, BR-05, C-10). */
    @Transactional
    public void delete(UUID id) {
        AnnotationRecord record = get(id);
        records.remove(record);
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_RECORD_DELETED, TARGET_ANNOTATION_RECORD, id));
    }

    /**
     * Reveals the cleartext of one Secret value, gated to the elevated reveal role (Tenant
     * administrator) and audited (FR-18, BR-10, C-03, C-10, C-12).
     */
    @Transactional
    public String reveal(UUID recordId, UUID fieldId) {
        if (tenant.role() != Role.ADMIN) {
            throw new SecretRevealForbiddenException();
        }
        AnnotationRecord record = get(recordId);
        AnnotationValue value = record.getValues().stream()
                .filter(v -> v.getTypeFieldId().equals(fieldId))
                .findFirst()
                .orElseThrow(AnnotationRecordRevealNotSecretException::new);
        if (!value.isSecret()) {
            throw new AnnotationRecordRevealNotSecretException();
        }
        String cleartext = cipher.decrypt(
                new EncryptedValue(value.getSecretCiphertext(), value.getSecretIv(), value.getSecretKeyVersion()));
        auditLog.persistInTenant(new AuditLog(
                tenant.tenantId(), tenant.userId(), ACTION_SECRET_REVEALED, TARGET_ANNOTATION_RECORD, recordId,
                "fieldId=" + fieldId));
        return cleartext;
    }

    private AnnotationType requireType(UUID typeId) {
        return types.findByIdInTenant(typeId).orElseThrow(AnnotationTypeNotFoundException::new);
    }

    /** Validates each input value against its field's schema (BR-03) and builds the persisted payload. */
    private List<AnnotationValue> toValues(AnnotationType type, List<AnnotationValueInput> inputs) {
        Map<UUID, TypeField> fieldsById =
                type.getFields().stream().collect(Collectors.toMap(TypeField::getId, Function.identity()));
        List<AnnotationValue> values = new ArrayList<>();
        for (AnnotationValueInput input : inputs) {
            TypeField field = fieldsById.get(input.fieldId());
            if (field == null) {
                throw new AnnotationRecordFieldUnknownException();
            }
            values.add(toValue(field, input));
        }
        return values;
    }

    private AnnotationValue toValue(TypeField field, AnnotationValueInput input) {
        AnnotationValue value = new AnnotationValue(field.getId());
        switch (field.getFieldType()) {
            case TEXT, FREE_TEXT -> applyText(field, input, value);
            case NUMBER -> applyNumber(field, input, value);
            case IMAGE -> applyImage(input, value);
            case LIST, SINGLE_CHOICE -> applyOptions(field, input, value, 1, 1);
            case MULTIPLE_CHOICE -> applyOptions(field, input, value, 0, Integer.MAX_VALUE);
        }
        return value;
    }

    private void applyText(TypeField field, AnnotationValueInput input, AnnotationValue value) {
        if (input.text() == null) {
            throw new AnnotationRecordValueTypeMismatchException();
        }
        int utf8Length = input.text().getBytes(StandardCharsets.UTF_8).length;
        if (utf8Length > (field.isSecret() ? SECRET_VALUE_MAX_BYTES : TEXT_VALUE_MAX_BYTES)) {
            throw new AnnotationRecordValueTooLongException();
        }
        if (field.isSecret()) {
            EncryptedValue encrypted = cipher.encrypt(input.text());
            value.setSecret(encrypted.ciphertext(), encrypted.iv(), encrypted.keyVersion());
        } else {
            value.setTextValue(input.text());
        }
    }

    private void applyNumber(TypeField field, AnnotationValueInput input, AnnotationValue value) {
        BigDecimal number = input.number();
        if (number == null) {
            throw new AnnotationRecordValueTypeMismatchException();
        }
        BigDecimal min = field.getNumberMin();
        BigDecimal max = field.getNumberMax();
        if ((min != null && number.compareTo(min) < 0) || (max != null && number.compareTo(max) > 0)) {
            throw new AnnotationRecordValueOutOfBoundsException();
        }
        value.setNumberValue(number);
    }

    private void applyImage(AnnotationValueInput input, AnnotationValue value) {
        UUID imageId = input.imageId();
        if (imageId == null) {
            throw new AnnotationRecordValueTypeMismatchException();
        }
        if (!images.existsInTenant(imageId)) {
            throw new AnnotationRecordValueImageNotFoundException();
        }
        value.setImageId(imageId);
    }

    private void applyOptions(
            TypeField field, AnnotationValueInput input, AnnotationValue value, int minSelected, int maxSelected) {
        Set<UUID> optionIds = input.optionIdsOrEmpty();
        if (optionIds.size() < minSelected || optionIds.size() > maxSelected) {
            throw new AnnotationRecordValueTypeMismatchException();
        }
        Set<UUID> known = field.getOptions().stream().map(FieldOption::getId).collect(Collectors.toSet());
        if (!known.containsAll(optionIds)) {
            throw new AnnotationRecordValueOptionUnknownException();
        }
        value.setSelectedOptionIds(optionIds);
    }
}
