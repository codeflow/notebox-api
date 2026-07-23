package com.notebox.api.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.Image;

/** Tenant-scoped access to images (AD-03). A foreign tenant's image id never resolves. */
@ApplicationScoped
public class ImageRepository extends TenantScopedRepository<Image> {

    @Override
    protected Class<Image> entityType() {
        return Image.class;
    }
}
