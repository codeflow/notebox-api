package com.notebox.api.domain.error;

/** An annotation type cannot be deleted while it still owns records (OQ-14, BR-05). */
public class AnnotationTypeHasRecordsException extends DomainException {

    public AnnotationTypeHasRecordsException() {
        super(ErrorCategory.CONFLICT, "annotation.type.has_records");
    }
}
