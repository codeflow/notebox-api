package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.notebox.api.domain.AnnotationType;

/** Full response view of an annotation type: its schema, in declared field order (FR-01). */
public record AnnotationTypeDto(
        UUID id, String name, UUID iconImageId, Instant createdAt, List<TypeFieldDto> fields) {

    public static AnnotationTypeDto from(AnnotationType type) {
        List<TypeFieldDto> fields = type.getFields().stream().map(TypeFieldDto::from).toList();
        return new AnnotationTypeDto(type.getId(), type.getName(), type.getIconImageId(), type.getCreatedAt(), fields);
    }
}
