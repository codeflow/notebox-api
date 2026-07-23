package com.notebox.api.api.dto;

import java.util.UUID;

import com.notebox.api.domain.Image;

/** Image reference returned in JSON (AD-04): id + metadata, never the bytes. */
public record ImageRefDto(UUID id, String contentType, long sizeBytes) {

    public static ImageRefDto from(Image image) {
        return new ImageRefDto(image.getId(), image.getContentType(), image.getSizeBytes());
    }
}
