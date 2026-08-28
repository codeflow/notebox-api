package com.notebox.api.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.notebox.api.application.content.RichTextSanitizer;
import com.notebox.api.domain.AnnotationValue;
import com.notebox.api.domain.TypeField;

/**
 * Response view of one record value (FR-04). A Secret field's value is always {@code masked:true}
 * with {@code text:null} — cleartext is returned only by the dedicated reveal endpoint (FR-18).
 */
public record AnnotationValueDto(
        UUID fieldId,
        String fieldType,
        boolean secret,
        boolean masked,
        String text,
        BigDecimal number,
        UUID imageId,
        List<UUID> optionIds) {

    public static AnnotationValueDto from(AnnotationValue value, TypeField field, RichTextSanitizer sanitizer) {
        if (field.isSecret()) {
            return new AnnotationValueDto(
                    field.getId(), field.getFieldType().name(), true, true, null, null, null, null);
        }
        String text = null;
        BigDecimal number = null;
        UUID imageId = null;
        List<UUID> optionIds = null;
        switch (field.getFieldType()) {
            // Read-side C-08: rich values are served dialect-clean even when stored rows predate
            // sanitization (INV-S2); the DTO alone changes — the entity is never touched (INV-S5).
            case FREE_TEXT -> text = sanitizer.sanitize(value.getTextValue());
            case TEXT -> text = value.getTextValue();
            case NUMBER -> number = value.getNumberValue();
            case IMAGE -> imageId = value.getImageId();
            case LIST, SINGLE_CHOICE, MULTIPLE_CHOICE -> optionIds = List.copyOf(value.getSelectedOptionIds());
        }
        return new AnnotationValueDto(
                field.getId(), field.getFieldType().name(), false, false, text, number, imageId, optionIds);
    }
}
