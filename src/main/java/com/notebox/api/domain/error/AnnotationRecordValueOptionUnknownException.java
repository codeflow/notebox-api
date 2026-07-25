package com.notebox.api.domain.error;

/** A choice value references an option id that is not one of its field's predefined options (BR-03). */
public class AnnotationRecordValueOptionUnknownException extends DomainException {

    public AnnotationRecordValueOptionUnknownException() {
        super(ErrorCategory.INVALID, "annotation.record.value.option.unknown");
    }
}
