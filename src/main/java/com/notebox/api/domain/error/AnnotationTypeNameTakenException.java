package com.notebox.api.domain.error;

/** The caller's tenant already has an annotation type with the requested name (per-tenant uniqueness). */
public class AnnotationTypeNameTakenException extends DomainException {

    public AnnotationTypeNameTakenException() {
        super(ErrorCategory.CONFLICT, "annotation.type.name.taken");
    }
}
