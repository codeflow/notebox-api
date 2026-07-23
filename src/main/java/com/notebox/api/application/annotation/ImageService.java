package com.notebox.api.application.annotation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import com.notebox.api.domain.Image;
import com.notebox.api.domain.error.ImageNotFoundException;
import com.notebox.api.domain.error.ImageTooLargeException;
import com.notebox.api.domain.error.UnsupportedImageTypeException;
import com.notebox.api.infrastructure.persistence.ImageRepository;
import com.notebox.api.infrastructure.security.TenantContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Stores and serves icon binaries (FR-07, AD-04). An upload is validated for content type and size
 * before it becomes a BLOB (AD-07, NFR-04, C-07); reads are tenant-scoped (C-01).
 */
@ApplicationScoped
public class ImageService {

    private final ImageRepository images;
    private final TenantContext tenant;
    private final long maxBytes;
    private final Set<String> allowedContentTypes;

    public ImageService(
            ImageRepository images,
            TenantContext tenant,
            @ConfigProperty(name = "notebox.image.max-bytes") long maxBytes,
            @ConfigProperty(name = "notebox.image.allowed-content-types") List<String> allowedContentTypes) {
        this.images = images;
        this.tenant = tenant;
        this.maxBytes = maxBytes;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    @Transactional
    public Image store(String contentType, byte[] bytes) {
        if (contentType == null || !allowedContentTypes.contains(contentType)) {
            throw new UnsupportedImageTypeException();
        }
        if (bytes == null || bytes.length == 0) {
            throw new UnsupportedImageTypeException();
        }
        if (bytes.length > maxBytes) {
            throw new ImageTooLargeException();
        }
        return images.persistInTenant(new Image(tenant.tenantId(), contentType, bytes));
    }

    public Image get(UUID id) {
        return images.findByIdInTenant(id).orElseThrow(ImageNotFoundException::new);
    }

    /** Whether an optional icon reference resolves to an image in the caller's tenant (used by the type service). */
    public boolean existsInTenant(UUID id) {
        return id == null || images.findByIdInTenant(id).isPresent();
    }
}
