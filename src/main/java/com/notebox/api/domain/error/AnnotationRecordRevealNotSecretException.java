package com.notebox.api.domain.error;

/** A reveal was requested for a field that is not Secret, or that holds no value (FR-18). */
public class AnnotationRecordRevealNotSecretException extends DomainException {

    public AnnotationRecordRevealNotSecretException() {
        super(ErrorCategory.INVALID, "annotation.record.reveal.not_secret");
    }
}
