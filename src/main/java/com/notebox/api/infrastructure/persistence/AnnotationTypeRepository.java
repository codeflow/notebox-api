package com.notebox.api.infrastructure.persistence;

import jakarta.enterprise.context.ApplicationScoped;

import com.notebox.api.domain.AnnotationType;

/** Tenant-scoped access to annotation types (AD-03). All queries are filtered by the caller's tenant. */
@ApplicationScoped
public class AnnotationTypeRepository extends TenantScopedRepository<AnnotationType> {

    @Override
    protected Class<AnnotationType> entityType() {
        return AnnotationType.class;
    }
}
