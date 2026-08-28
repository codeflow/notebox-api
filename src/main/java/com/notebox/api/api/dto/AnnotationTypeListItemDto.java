package com.notebox.api.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.notebox.api.domain.AnnotationType;

/**
 * Listing-row shape of an annotation type (design 07): the scalars the list shows, plus the counts
 * it reports — how many fields the type declares, how many of those appear in the records grid, and
 * how many records exist.
 *
 * <p>This is a SEPARATE shape from {@link AnnotationTypeDto} on purpose. A Java record serialises
 * every component, so adding {@code recordCount} to the shared DTO would force every single-type
 * response to publish a number it does not have — which is exactly the defect feat-016's audit
 * found (F-01) when a group DTO published a zero it could not know.
 */
public record AnnotationTypeListItemDto(
        UUID id,
        String name,
        UUID iconImageId,
        int fieldCount,
        int visibleFieldCount,
        long recordCount,
        Instant createdAt) {

    /**
     * Maps a type and its record count to the listing-row shape.
     *
     * @param type the type
     * @param recordCount how many records the tenant holds of it
     * @return the row representation
     */
    public static AnnotationTypeListItemDto from(AnnotationType type, long recordCount) {
        int visible = (int) type.getFields().stream().filter(field -> field.isVisibleForViewing()).count();
        return new AnnotationTypeListItemDto(
                type.getId(),
                type.getName(),
                type.getIconImageId(),
                type.getFields().size(),
                visible,
                recordCount,
                type.getCreatedAt());
    }
}
