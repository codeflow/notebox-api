package com.notebox.api.domain.error;

/** A text or secret value exceeds the maximum length its column can store (audit F3). */
public class AnnotationRecordValueTooLongException extends DomainException {

    public AnnotationRecordValueTooLongException() {
        super(ErrorCategory.INVALID, "annotation.record.value.too_long");
    }
}
