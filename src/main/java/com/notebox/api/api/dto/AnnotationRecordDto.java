package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.notebox.api.application.content.RichTextSanitizer;
import com.notebox.api.domain.AnnotationRecord;
import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.TypeField;

/** Full response view of an annotation record: its values, ordered by the type's field position (FR-04). */
public record AnnotationRecordDto(
        UUID id, UUID annotationTypeId, String name, Instant createdAt, Instant updatedAt,
        List<AnnotationValueDto> values) {

    public static AnnotationRecordDto from(AnnotationRecord record, AnnotationType type, RichTextSanitizer sanitizer) {
        Map<UUID, AnnotationValue> valuesByField = record.getValues().stream()
                .collect(Collectors.toMap(AnnotationValue::getTypeFieldId, Function.identity()));
        List<AnnotationValueDto> values = new ArrayList<>();
        for (TypeField field : type.getFields()) {
            AnnotationValue value = valuesByField.get(field.getId());
            if (value != null) {
                values.add(AnnotationValueDto.from(value, field, sanitizer));
            }
        }
        return new AnnotationRecordDto(
                record.getId(), record.getAnnotationTypeId(), record.getName(), record.getCreatedAt(),
                record.getUpdatedAt(), values);
    }
}
