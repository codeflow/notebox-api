package com.notebox.api.domain.error;

/** A type field cannot be removed or retyped while the type still owns records (OQ-17, audit F5). */
public class AnnotationTypeFieldHasRecordsException extends DomainException {

    public AnnotationTypeFieldHasRecordsException() {
        super(ErrorCategory.CONFLICT, "annotation.type.field.has_records");
    }
}
