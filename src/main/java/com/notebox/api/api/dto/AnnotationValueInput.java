package com.notebox.api.api.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One value in a create/replace request (FR-04): a target field id and exactly one populated
 * payload slot matching that field's declared kind — {@code text}, {@code number}, {@code imageId}
 * or {@code optionIds}. On replace, a Secret field additionally honours {@code clearSecret}: the
 * decided PUT semantics (human decision 2026-07-25, audit F4) preserve the stored ciphertext unless
 * a new {@code text} is sent or erasure is explicitly requested. Conformance to the field's type
 * (unknown field, wrong slot, bounds, unknown option) is validated by
 * {@code AnnotationRecordService} (BR-03), not here.
 */
public record AnnotationValueInput(
        UUID fieldId, String text, BigDecimal number, UUID imageId, List<UUID> optionIds, Boolean clearSecret) {

    public AnnotationValueInput(UUID fieldId, String text, BigDecimal number, UUID imageId, List<UUID> optionIds) {
        this(fieldId, text, number, imageId, optionIds, null);
    }

    /** Never-null option-id view; tolerates a null element (audit F13) so the service rejects it as unknown. */
    public Set<UUID> optionIdsOrEmpty() {
        return optionIds == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(optionIds));
    }

    /** True only when the client explicitly asked to erase the stored secret (audit F4). */
    public boolean clearSecretRequested() {
        return Boolean.TRUE.equals(clearSecret);
    }
}
