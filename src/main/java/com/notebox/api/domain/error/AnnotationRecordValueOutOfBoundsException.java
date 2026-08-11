package com.notebox.api.domain.error;

/** A Number value falls outside its field's declared {@code numberMin}/{@code numberMax} bounds (BR-03). */
public class AnnotationRecordValueOutOfBoundsException extends DomainException {

    public AnnotationRecordValueOutOfBoundsException() {
        super(ErrorCategory.INVALID, "annotation.record.value.number.out_of_bounds");
    }
}
