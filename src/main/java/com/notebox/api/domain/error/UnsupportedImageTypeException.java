package com.notebox.api.domain.error;

/** An uploaded image has a content type outside the allowed set (C-07). */
public class UnsupportedImageTypeException extends DomainException {

    public UnsupportedImageTypeException() {
        super(ErrorCategory.INVALID, "annotation.image.type.unsupported");
    }
}
