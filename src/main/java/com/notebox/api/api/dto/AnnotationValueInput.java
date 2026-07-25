package com.notebox.api.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * One value in a create/replace request (FR-04): a target field id and exactly one populated
 * payload slot matching that field's declared kind — {@code text}, {@code number}, {@code imageId}
 * or {@code optionIds}. Conformance to the field's type (unknown field, wrong slot, bounds, unknown
 * option) is validated by {@code AnnotationRecordService} (BR-03), not here.
 */
public record AnnotationValueInput(UUID fieldId, String text, BigDecimal number, UUID imageId, List<UUID> optionIds) {

    /** Never-null option-id view for the service. */
    public Set<UUID> optionIdsOrEmpty() {
        return optionIds == null ? Set.of() : Set.copyOf(optionIds);
    }
}
