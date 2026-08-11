package com.notebox.api.domain.error;

/** The requested annotation record does not exist in the caller's tenant (C-01). */
public class AnnotationRecordNotFoundException extends DomainException {

    public AnnotationRecordNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "annotation.record.not_found");
    }
}
