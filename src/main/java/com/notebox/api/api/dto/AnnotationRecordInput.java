package com.notebox.api.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Create/replace request for an annotation record (FR-04). {@code annotationTypeId} is required on
 * create and ignored on replace (the path id fixes the record's type).
 */
public record AnnotationRecordInput(
        UUID annotationTypeId, @NotBlank(message = "annotation.record.name.required") String name,
        @Valid List<AnnotationValueInput> values) {

    /** Never-null value view for the service. */
    public List<AnnotationValueInput> valuesOrEmpty() {
        return values == null ? List.of() : values;
    }
}
