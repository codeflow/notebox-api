package com.notebox.api.api.dto;

import java.util.UUID;

import com.notebox.api.domain.FieldOption;

/** Response view of a field option (FR-03). */
public record FieldOptionDto(UUID id, String label, String badgeColour) {

    public static FieldOptionDto from(FieldOption option) {
        String colour = option.getBadgeColour() == null ? null : option.getBadgeColour().name();
        return new FieldOptionDto(option.getId(), option.getLabel(), colour);
    }
}
