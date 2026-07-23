package com.notebox.api.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.notebox.api.domain.TypeField;

/** Response view of a type field, with server-resolved defaults (FR-02, FR-03). */
public record TypeFieldDto(
        UUID id,
        String name,
        String fieldType,
        UUID iconImageId,
        boolean visibleForViewing,
        boolean secret,
        BigDecimal numberMin,
        BigDecimal numberMax,
        List<FieldOptionDto> options) {

    public static TypeFieldDto from(TypeField field) {
        List<FieldOptionDto> options = field.getOptions().stream().map(FieldOptionDto::from).toList();
        return new TypeFieldDto(
                field.getId(),
                field.getName(),
                field.getFieldType().name(),
                field.getIconImageId(),
                field.isVisibleForViewing(),
                field.isSecret(),
                field.getNumberMin(),
                field.getNumberMax(),
                options);
    }
}
