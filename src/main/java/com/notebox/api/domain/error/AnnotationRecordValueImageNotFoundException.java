package com.notebox.api.domain.error;

/** An Image-field value references an image id that does not resolve in the caller's tenant (C-01, C-07). */
public class AnnotationRecordValueImageNotFoundException extends DomainException {

    public AnnotationRecordValueImageNotFoundException() {
        super(ErrorCategory.NOT_FOUND, "annotation.record.value.image.not_found");
    }
}
