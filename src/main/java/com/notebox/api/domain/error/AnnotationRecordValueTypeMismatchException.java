package com.notebox.api.domain.error;

/** A record value's shape does not match its field's declared {@code FieldType} (BR-03). */
public class AnnotationRecordValueTypeMismatchException extends DomainException {

    public AnnotationRecordValueTypeMismatchException() {
        super(ErrorCategory.INVALID, "annotation.record.value.type_mismatch");
    }
}
