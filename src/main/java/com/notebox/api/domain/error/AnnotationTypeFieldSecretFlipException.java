package com.notebox.api.domain.error;

/** A field's Secret flag cannot change while the field still has values (OQ-18, audit F12). */
public class AnnotationTypeFieldSecretFlipException extends DomainException {

    public AnnotationTypeFieldSecretFlipException() {
        super(ErrorCategory.CONFLICT, "annotation.type.field.secret_flip.has_values");
    }
}
