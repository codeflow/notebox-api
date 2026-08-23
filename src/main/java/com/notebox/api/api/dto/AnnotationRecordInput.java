package com.notebox.api.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.notebox.api.api.validation.AtMostOneValuePerField;

/**
 * Create/replace request for an annotation record (FR-04). {@code annotationTypeId} is required on
 * create and ignored on replace (the path id fixes the record's type). Replace semantics: an
 * omitted group clears it (FR-08).
 */
@AtMostOneValuePerField
public record AnnotationRecordInput(
        UUID annotationTypeId,
        @NotBlank(message = "annotation.record.name.required")
                @Size(max = 120, message = "annotation.record.name.too_long")
                String name,
        @Valid List<@NotNull(message = "annotation.record.value.required") AnnotationValueInput> values,
        UUID groupId) {

    /** Never-null value view for the service. */
    public List<AnnotationValueInput> valuesOrEmpty() {
        return values == null ? List.of() : values;
    }
}
