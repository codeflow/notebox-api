package com.notebox.api.domain.error;

/** A record value targets a field the annotation type does not define (BR-03). */
public class AnnotationRecordFieldUnknownException extends DomainException {

    public AnnotationRecordFieldUnknownException() {
        super(ErrorCategory.INVALID, "annotation.record.field.unknown");
    }
}
