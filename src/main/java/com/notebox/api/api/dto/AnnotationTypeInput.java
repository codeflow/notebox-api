package com.notebox.api.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Create/replace request for an annotation type (FR-01). {@code iconImageId} references an uploaded image. */
public record AnnotationTypeInput(
        @NotBlank(message = "annotation.type.name.required") String name,
        UUID iconImageId,
        @Valid List<TypeFieldInput> fields) {

    /** Never-null field view for callers and the service. */
    public List<TypeFieldInput> fieldsOrEmpty() {
        return fields == null ? List.of() : fields;
    }
}
