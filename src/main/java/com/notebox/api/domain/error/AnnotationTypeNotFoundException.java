package com.notebox.api.domain.error;

/** The requested annotation type does not exist in the caller's tenant (C-01). */
public class AnnotationTypeNotFoundException extends DomainException {

    public AnnotationTypeNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "annotation.type.not_found");
    }
}
